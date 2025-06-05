import dao.*;
import models.*;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class UDPServer {
    private static final int BUFFER_SIZE = 65507; // Maksymalny rozmiar datagramu UDP
    private DatagramSocket serverSocket;
    private int port;
    private boolean running;
    
    // Store data
    private List<Movie> movies;
    private List<Room> rooms;
    private List<Screening> screenings;
    private List<Reservation> reservations;
    
    // Database access
    private DatabaseManager dbManager;
    private MovieDAO movieDAO;
    private RoomDAO roomDAO;
    private ScreeningDAO screeningDAO;
    private ReservationDAO reservationDAO;
    
    public UDPServer(int port) {
        this.port = port;
        this.movies = new ArrayList<>();
        this.rooms = new ArrayList<>();
        this.screenings = new ArrayList<>();
        this.reservations = new ArrayList<>();
        
        // Initialize database connection
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            // Create database manager and initialize the database
            this.dbManager = new DatabaseManager();
            dbManager.initDatabase();
            
            // Get connection for DAOs
            Connection conn = dbManager.getConnection();
            this.movieDAO = new MovieDAO(conn);
            this.roomDAO = new RoomDAO(conn);
            this.screeningDAO = new ScreeningDAO(conn);
            this.reservationDAO = new ReservationDAO(conn);
            
            // Load data from database
            loadDataFromDatabase();
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            
            // If database initialization fails, use sample data
            initializeTestData();
        }
    }
    
    private void loadDataFromDatabase() {
        try {
            // Load all data from the database
            movies = movieDAO.findAll();
            rooms = roomDAO.findAll();
            screenings = screeningDAO.findAll();
            reservations = reservationDAO.findAll();
            
            // If no data in database, initialize with test data
            if (movies.isEmpty()) {
                System.out.println("No data found in database. Initializing with test data.");
                initializeTestData();
                saveDataToDatabase();
            }
        } catch (SQLException e) {
            System.err.println("Error loading data from database: " + e.getMessage());
            e.printStackTrace();
            
            // If loading fails, use sample data
            initializeTestData();
        }
    }
    
    private void saveDataToDatabase() {
        try {
            // Save all current data to the database
            for (Movie movie : movies) {
                movieDAO.insert(movie);
            }
            
            for (Room room : rooms) {
                roomDAO.insert(room);
            }
            
            for (Screening screening : screenings) {
                screeningDAO.insert(screening);
            }
            
            for (Reservation reservation : reservations) {
                reservationDAO.insert(reservation);
            }
        } catch (SQLException e) {
            System.err.println("Error saving data to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void start() {
        try {
            serverSocket = new DatagramSocket(port);
            running = true;
            System.out.println("Server started on port " + port);
            
            byte[] receiveBuffer = new byte[BUFFER_SIZE];
            
            while (running) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket); // Blocks until packet is received
                
                // Process received data in a new thread
                new Thread(() -> processReceivedPacket(receivePacket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }
    
    private void processReceivedPacket(DatagramPacket packet) {
        try {
            // Deserialize the message
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Message request = (Message) ois.readObject();
            
            System.out.println("Received request: " + request.getType());
            
            // Process the request and create response
            Message response = processRequest(request);
            
            // Send response back to client
            sendResponse(response, packet.getAddress(), packet.getPort());
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean createReservation(Reservation reservation) {
        try {
            System.out.println("Processing reservation request...");
            
            // Upewnij się, że screening istnieje i jest prawidłowy
            Screening screening = reservation.getScreening();
            if (screening == null) {
                System.out.println("Screening is null");
                return false;
            }
            
            // Sprawdź, czy film i sala istnieją
            if (screening.getMovie() == null || screening.getRoom() == null) {
                System.out.println("Movie or Room is null in the screening");
                return false;
            }
            
            List<Seat> seats = reservation.getReservedSeats();
            if (seats == null || seats.isEmpty()) {
                System.out.println("No seats selected");
                return false;
            }
            
            // Sprawdzenie dostępności miejsc
            for (Seat seat : seats) {
                if (!screening.isSeatAvailable(seat.getRow(), seat.getNumber())) {
                    System.out.println("Seat " + seat.getRow() + "-" + seat.getNumber() + " is not available.");
                    return false; // Miejsce niedostępne
                }
            }
            
            // Wszystkie miejsca są dostępne, zarezerwuj je
            reservation.confirmReservation();
            reservations.add(reservation);
            System.out.println("Reservation confirmed in memory: " + reservation.getReservationId());
            
            // Zapisz do bazy danych
            try {
                reservationDAO.insert(reservation);
                System.out.println("Reservation saved to database: " + reservation.getReservationId());
                return true;
            } catch (SQLException e) {
                System.err.println("SQL Error saving reservation: " + e.getMessage());
                e.printStackTrace();
                System.err.println("SQL State: " + e.getSQLState() + ", Error Code: " + e.getErrorCode());
                
                // Mimo błędu bazy danych, zwracamy true, jeśli udało się dodać rezerwację do pamięci
                // To może wyjaśniać, dlaczego widzimy rezerwacje w liście, mimo błędu
                System.out.println("Reservation was added to memory but not to database");
                return true; // Warto rozważyć zmianę na false, jeśli integralność bazy danych jest ważna
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in createReservation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Poprawiony processRequest z obsługą wyjątków
    private Message processRequest(Message request) {
        try {
            switch (request.getType()) {
                case GET_MOVIES:
                    return request.createSuccessResponse(movies);
                    
                case GET_SCREENINGS:
                    if (request.getPayload() instanceof Integer) {
                        Integer movieId = (Integer) request.getPayload();
                        List<Screening> movieScreenings = screenings.stream()
                            .filter(s -> s.getMovie().getMovieId() == movieId)
                            .toList();
                        return request.createSuccessResponse(movieScreenings);
                    }
                    return request.createSuccessResponse(screenings);
                    
                case GET_SEATS:
                    if (request.getPayload() instanceof Integer) {
                        Integer screeningId = (Integer) request.getPayload();
                        Optional<Screening> screeningOpt = screenings.stream()
                            .filter(s -> s.getScreeningId() == screeningId)
                            .findFirst();
                        
                        if (screeningOpt.isPresent()) {
                            Screening screening = screeningOpt.get();
                            
                            // Aktualizuj stan miejsc na podstawie istniejących rezerwacji
                            screening.updateSeatsStatusFromReservations(reservations);
                            
                            return request.createSuccessResponse(screening.getAvailableSeats());
                        } else {
                            return request.createErrorResponse("Screening not found");
                        }
                    }
                    return request.createErrorResponse("Invalid screening ID");
                    
                case MAKE_RESERVATION:
                    System.out.println("Received MAKE_RESERVATION request");
                    if (request.getPayload() instanceof Reservation) {
                        Reservation reservation = (Reservation) request.getPayload();
                        boolean success = createReservation(reservation);
                        
                        if (success) {
                            System.out.println("Reservation created successfully: " + reservation.getReservationId());
                            return request.createSuccessResponse(reservation);
                        } else {
                            System.out.println("Failed to create reservation");
                            return request.createErrorResponse("Failed to create reservation");
                        }
                    }
                    System.out.println("Invalid reservation data");
                    return request.createErrorResponse("Invalid reservation data");
                    
                case CANCEL_RESERVATION:
                    System.out.println("Received CANCEL_RESERVATION request");
                    if (request.getPayload() instanceof String) {
                        String reservationId = (String) request.getPayload();
                        System.out.println("Attempting to cancel reservation: " + reservationId);
                        boolean success = cancelReservation(reservationId);
                        
                        if (success) {
                            return request.createSuccessResponse(true);
                        } else {
                            return request.createErrorResponse("Reservation not found or already cancelled");
                        }
                    }
                    return request.createErrorResponse("Invalid reservation ID");
                    
                case GET_RESERVATIONS_BY_EMAIL:
                    if (request.getPayload() instanceof String) {
                        String email = (String) request.getPayload();
                        System.out.println("Searching for reservations with email: " + email);
                        
                        // Filtruj rezerwacje po adresie email
                        List<Reservation> userReservations = reservations.stream()
                            .filter(r -> email.equals(r.getCustomerEmail()))
                            .collect(Collectors.toList());
                            
                        return request.createSuccessResponse(userReservations);
                    }
                    return request.createErrorResponse("Invalid email address");
                    
                default:
                    return request.createErrorResponse("Unsupported operation");
            }
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            e.printStackTrace();
            return request.createErrorResponse("Server error: " + e.getMessage());
        }
    }
    
    private boolean cancelReservation(String reservationId) {
        try {
            System.out.println("Cancelling reservation with ID: " + reservationId);
            
            // Najpierw szukamy w pamięci
            Optional<Reservation> reservationOpt = reservations.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst();
            
            if (reservationOpt.isPresent()) {
                Reservation reservation = reservationOpt.get();
                System.out.println("Found reservation in memory, updating status...");
                
                // Aktualizuj w pamięci
                reservation.cancelReservation();
                
                // Aktualizuj status w bazie
                try {
                    System.out.println("Updating reservation status in database...");
                    reservationDAO.updateStatus(reservationId, ReservationStatus.CANCELLED);
                    System.out.println("Reservation successfully cancelled.");
                    
                    // Aktualizuj stan miejsc w seansu
                    reservation.getScreening().updateSeatsStatusFromReservations(reservations);
                    
                    return true;
                } catch (SQLException e) {
                    System.err.println("Database error when updating reservation: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Mimo błędu bazy danych, zwracamy true, bo rezerwacja została anulowana w pamięci
                    return true;
                }
            } else {
                System.out.println("Reservation not found in memory, searching in database...");
                
                // Jeśli nie znaleziono w pamięci, sprawdź bazę danych
                try {
                    Reservation dbReservation = reservationDAO.findById(reservationId);
                    if (dbReservation != null) {
                        System.out.println("Found reservation in database, updating status...");
                        
                        // Aktualizuj w pamięci
                        dbReservation.cancelReservation();
                        reservations.add(dbReservation);
                        
                        // Aktualizuj status w bazie
                        reservationDAO.updateStatus(reservationId, ReservationStatus.CANCELLED);
                        System.out.println("Reservation successfully cancelled.");
                        return true;
                    } else {
                        System.out.println("Reservation not found in database.");
                        return false;
                    }
                } catch (SQLException e) {
                    System.err.println("Database error when finding reservation: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in cancelReservation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void sendResponse(Message response, InetAddress address, int port) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(response);
            
            byte[] responseData = baos.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address, port);
            serverSocket.send(responsePacket);
            
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeTestData() {
        // Create movies
        Movie movie1 = new Movie(1, "Inception", 148, 
            "A thief who steals corporate secrets through the use of dream-sharing technology.",
            "Sci-Fi", "Christopher Nolan", 2010, "English");
        Movie movie2 = new Movie(2, "The Shawshank Redemption", 142,
            "Two imprisoned men bond over a number of years.",
            "Drama", "Frank Darabont", 1994, "English");
        
        movies.add(movie1);
        movies.add(movie2);
        
        // Create rooms
        Room room1 = new Room(1, "Sala 1", 10, 15);
        Room room2 = new Room(2, "Sala 2", 8, 12);
        
        rooms.add(room1);
        rooms.add(room2);
        
        // Create screenings
        Screening screening1 = new Screening(
            1, movie1, room1, LocalDateTime.now().plusDays(1).withHour(18).withMinute(0), 25.0);
        Screening screening2 = new Screening(
            2, movie1, room1, LocalDateTime.now().plusDays(1).withHour(21).withMinute(0), 25.0);
        Screening screening3 = new Screening(
            3, movie2, room2, LocalDateTime.now().plusDays(2).withHour(19).withMinute(0), 22.0);
        
        screenings.add(screening1);
        screenings.add(screening2);
        screenings.add(screening3);
    }
    
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        // Close database connection
        if (dbManager != null) {
            dbManager.closeConnection();
        }
    }
    
    public static void main(String[] args) {
        int port = 9876; // Default port
        
        // Allow custom port via command line
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + port);
            }
        }
        
        UDPServer server = new UDPServer(port);
        server.start();
    }
    
    public void updateStatus(String id, ReservationStatus status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE reservationId = ?";
        System.out.println("Executing SQL: " + sql + " with params: [" + status.name() + ", " + id + "]");
        
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, id);
            
            int updatedRows = pstmt.executeUpdate();
            System.out.println("Rows updated: " + updatedRows);
            
            if (updatedRows == 0) {
                System.out.println("Warning: No rows were updated when changing status for ID: " + id);
            }
        }
    }
}
