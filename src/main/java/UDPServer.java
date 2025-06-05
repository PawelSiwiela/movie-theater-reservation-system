import dao.*;
import models.*;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

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
    
    private Message processRequest(Message request) {
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
                    Optional<Screening> screening = screenings.stream()
                        .filter(s -> s.getScreeningId() == screeningId)
                        .findFirst();
                    
                    if (screening.isPresent()) {
                        return request.createSuccessResponse(screening.get().getAvailableSeats());
                    } else {
                        return request.createErrorResponse("Screening not found");
                    }
                }
                return request.createErrorResponse("Invalid screening ID");
                
            case MAKE_RESERVATION:
                if (request.getPayload() instanceof Reservation) {
                    Reservation reservation = (Reservation) request.getPayload();
                    boolean success = createReservation(reservation);
                    
                    if (success) {
                        return request.createSuccessResponse(reservation);
                    } else {
                        return request.createErrorResponse("Failed to create reservation");
                    }
                }
                return request.createErrorResponse("Invalid reservation data");
                
            case CANCEL_RESERVATION:
                if (request.getPayload() instanceof String) {
                    String reservationId = (String) request.getPayload();
                    boolean success = cancelReservation(reservationId);
                    
                    if (success) {
                        return request.createSuccessResponse(true);
                    } else {
                        return request.createErrorResponse("Reservation not found or already cancelled");
                    }
                }
                return request.createErrorResponse("Invalid reservation ID");
                
            default:
                return request.createErrorResponse("Unsupported operation");
        }
    }
    
    private boolean createReservation(Reservation reservation) {
        // Check if all seats are available
        Screening screening = reservation.getScreening();
        List<Seat> seats = reservation.getReservedSeats();
        
        for (Seat seat : seats) {
            if (!screening.isSeatAvailable(seat.getRow(), seat.getNumber())) {
                return false; // Seat not available
            }
        }
        
        // If all seats available, mark them as reserved
        reservation.confirmReservation();
        reservations.add(reservation);
        
        // Save to database
        try {
            reservationDAO.insert(reservation);
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving reservation to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean cancelReservation(String reservationId) {
        Optional<Reservation> optionalReservation = reservations.stream()
            .filter(r -> r.getReservationId().equals(reservationId))
            .findFirst();
            
        if (optionalReservation.isPresent()) {
            Reservation reservation = optionalReservation.get();
            reservation.cancelReservation();
            
            // Update in database
            try {
                reservationDAO.updateStatus(reservationId, ReservationStatus.CANCELLED);
                return true;
            } catch (SQLException e) {
                System.err.println("Error updating reservation in database: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        return false;
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
}
