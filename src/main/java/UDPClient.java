import models.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class UDPClient {
    private static final int BUFFER_SIZE = 65507;
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private int serverPort;
    private Scanner scanner;
    
    public UDPClient(String serverHost, int serverPort) {
        try {
            this.clientSocket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(serverHost);
            this.serverPort = serverPort;
            this.scanner = new Scanner(System.in);
        } catch (UnknownHostException | SocketException e) {
            System.err.println("Error initializing client: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void start() {
        System.out.println("Cinema Booking System Client");
        System.out.println("===========================");
        
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1:
                    listMovies();
                    break;
                case 2:
                    listScreenings();
                    break;
                case 3:
                    makeReservation();
                    break;
                case 4:
                    cancelReservation();
                    break;
                case 5:
                    listMyReservations();
                    break;
                case 0:
                    running = false;
                    System.out.println("Exiting program. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
        
        clientSocket.close();
    }
    
    private void displayMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. List all movies");
        System.out.println("2. List screenings for a movie");
        System.out.println("3. Make reservation");
        System.out.println("4. Cancel reservation");
        System.out.println("5. List my reservations");
        System.out.println("0. Exit");
    }
    
    private void listMovies() {
        Message request = new Message(MessageType.GET_MOVIES, null);
        Message response = sendRequest(request);
        
        if (response != null && response.isSuccess()) {
            List<Movie> movies = (List<Movie>) response.getPayload();
            System.out.println("\nAvailable Movies:");
            System.out.println("=================");
            
            for (Movie movie : movies) {
                System.out.println("ID: " + movie.getMovieId() + 
                                 ", Title: " + movie.getTitle() + 
                                 ", Duration: " + movie.getDuration() + " min" +
                                 ", Genre: " + movie.getGenre() +
                                 ", Director: " + movie.getDirector());
            }
        } else {
            System.out.println("Failed to get movies: " + 
                             (response != null ? response.getStatusMessage() : "No response from server"));
        }
    }
    
    private void listScreenings() {
        listMovies();
        int movieId = getIntInput("\nEnter movie ID to see screenings: ");
        
        Message request = new Message(MessageType.GET_SCREENINGS, movieId);
        Message response = sendRequest(request);
        
        if (response != null && response.isSuccess()) {
            List<Screening> screenings = (List<Screening>) response.getPayload();
            System.out.println("\nAvailable Screenings:");
            System.out.println("====================");
            
            for (Screening screening : screenings) {
                System.out.println("Screening ID: " + screening.getScreeningId() + 
                                 ", Movie: " + screening.getMovie().getTitle() + 
                                 ", Time: " + screening.getScreeningTime() +
                                 ", Room: " + screening.getRoom().getRoomName() +
                                 ", Price: " + screening.getTicketPrice());
            }
        } else {
            System.out.println("Failed to get screenings: " + 
                             (response != null ? response.getStatusMessage() : "No response from server"));
        }
    }
    
    private void makeReservation() {
        try {
            // List screenings
            listScreenings();
            
            // Get screening selection
            int screeningId = getIntInput("\nEnter screening ID to book: ");
            
            // Get seat information
            Message request = new Message(MessageType.GET_SEATS, screeningId);
            Message response = sendRequest(request);
            
            if (response == null || !response.isSuccess()) {
                System.out.println("Failed to get seat information: " + 
                                 (response != null ? response.getStatusMessage() : "No response from server"));
                return;
            }
            
            // Display seat map
            boolean[][] availableSeats = (boolean[][]) response.getPayload();
            displaySeatMap(availableSeats);
            
            // Get customer info
            System.out.print("\nEnter your name: ");
            String customerName = scanner.nextLine();
            
            System.out.print("Enter your email: ");
            String customerEmail = scanner.nextLine();
            
            System.out.print("Enter your phone: ");
            String customerPhone = scanner.nextLine();
            
            // Get seat selections
            List<Seat> selectedSeats = new ArrayList<>();
            boolean selectingSeats = true;
            
            while (selectingSeats) {
                int row = getIntInput("Enter row number (0 to finish): ");
                if (row == 0) {
                    selectingSeats = false;
                    continue;
                }
                
                int seatNum = getIntInput("Enter seat number: ");
                
                if (row <= 0 || row > availableSeats.length || 
                    seatNum <= 0 || seatNum > availableSeats[0].length) {
                    System.out.println("Invalid seat position.");
                    continue;
                }
                
                if (!availableSeats[row-1][seatNum-1]) {
                    System.out.println("This seat is not available.");
                    continue;
                }
                
                selectedSeats.add(new Seat(row, seatNum, SeatStatus.RESERVED));
                System.out.println("Seat " + row + "-" + seatNum + " added to selection.");
            }
            
            if (selectedSeats.isEmpty()) {
                System.out.println("No seats selected. Reservation cancelled.");
                return;
            }
            
            // Create reservation
            // We need to get the screening details first
            Message getScreeningRequest = new Message(MessageType.GET_SCREENINGS, screeningId);
            Message getScreeningResponse = sendRequest(getScreeningRequest);
            
            if (getScreeningResponse == null || !getScreeningResponse.isSuccess()) {
                System.out.println("Failed to get screening details: " + 
                                 (getScreeningResponse != null ? getScreeningResponse.getStatusMessage() : "No response from server"));
                return;
            }
            
            List<Screening> screenings = (List<Screening>) getScreeningResponse.getPayload();
            if (screenings.isEmpty()) {
                System.out.println("Screening not found.");
                return;
            }
            
            Screening screening = screenings.get(0);
            Reservation reservation = new Reservation(screening, selectedSeats, customerName, customerEmail, customerPhone);
            
            // Send reservation request
            Message reservationRequest = new Message(MessageType.MAKE_RESERVATION, reservation);
            Message reservationResponse = sendRequest(reservationRequest);
            
            if (reservationResponse != null && reservationResponse.isSuccess()) {
                Reservation confirmedReservation = (Reservation) reservationResponse.getPayload();
                System.out.println("\nReservation successful!");
                System.out.println("Reservation ID: " + confirmedReservation.getReservationId());
                System.out.println("Movie: " + confirmedReservation.getScreening().getMovie().getTitle());
                System.out.println("Time: " + confirmedReservation.getScreening().getScreeningTime());
                System.out.println("Number of seats: " + confirmedReservation.getReservedSeats().size());
                System.out.println("Total price: " + confirmedReservation.getTotalPrice());
                System.out.println("\nPlease save your reservation ID for future reference.");
            } else {
                System.out.println("There was an issue with your reservation. Please check your reservations list to confirm if it was processed.");
                System.out.println("Error: " + (reservationResponse != null ? reservationResponse.getStatusMessage() : "No response from server"));
            }
        } catch (Exception e) {
            System.err.println("Error during reservation process: " + e.getMessage());
            System.out.println("Reservation cancelled due to error.");
        }
    }
    
    private void displaySeatMap(boolean[][] availableSeats) {
        System.out.println("\nSeat Map (O=Available, X=Occupied):");
        System.out.println("=================================");
        
        // Print seat numbers header
        System.out.print("    ");
        for (int j = 1; j <= availableSeats[0].length; j++) {
            System.out.printf("%2d ", j);
        }
        System.out.println();
        
        // Print rows with seat availability
        for (int i = 0; i < availableSeats.length; i++) {
            System.out.printf("%2d: ", (i+1));
            for (int j = 0; j < availableSeats[i].length; j++) {
                System.out.print(availableSeats[i][j] ? " O " : " X ");
            }
            System.out.println();
        }
    }
    
    private void cancelReservation() {
        try {
            System.out.print("\nEnter your email address: ");
            String email = scanner.nextLine();
            
            // Pobierz wszystkie rezerwacje dla podanego adresu email
            Message request = new Message(MessageType.GET_RESERVATIONS_BY_EMAIL, email);
            Message response = sendRequest(request);
            
            if (response == null || !response.isSuccess()) {
                System.out.println("Failed to get reservations: " + 
                                 (response != null ? response.getStatusMessage() : "No response from server"));
                return;
            }
            
            List<Reservation> reservations = (List<Reservation>) response.getPayload();
            if (reservations.isEmpty()) {
                System.out.println("No reservations found for email: " + email);
                return;
            }
            
            // Wyświetl listę rezerwacji
            System.out.println("\nYour reservations:");
            System.out.println("=================");
            
            for (int i = 0; i < reservations.size(); i++) {
                Reservation res = reservations.get(i);
                System.out.println((i+1) + ". ID: " + res.getReservationId() + 
                                 ", Movie: " + res.getScreening().getMovie().getTitle() +
                                 ", Time: " + res.getScreening().getScreeningTime() +
                                 ", Status: " + res.getStatus());
            }
            
            // Wybór rezerwacji do anulowania
            int choice = getIntInput("\nEnter the number of the reservation to cancel (0 to cancel): ");
            if (choice <= 0 || choice > reservations.size()) {
                System.out.println("Operation cancelled.");
                return;
            }
            
            Reservation selectedReservation = reservations.get(choice-1);
            
            // Sprawdź, czy rezerwacja już nie jest anulowana
            if (selectedReservation.getStatus() == ReservationStatus.CANCELLED) {
                System.out.println("This reservation is already cancelled.");
                return;
            }
            
            // Potwierdzenie anulowania
            System.out.println("\nYou selected to cancel this reservation:");
            System.out.println("Movie: " + selectedReservation.getScreening().getMovie().getTitle());
            System.out.println("Time: " + selectedReservation.getScreening().getScreeningTime());
            System.out.println("Seats: " + selectedReservation.getReservedSeats().size());
            
            System.out.print("Are you sure you want to cancel this reservation? (y/n): ");
            String confirm = scanner.nextLine();
            
            if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                System.out.println("Cancellation aborted.");
                return;
            }
            
            // Anulowanie rezerwacji
            String reservationId = selectedReservation.getReservationId();
            System.out.println("Sending cancellation request for reservation: " + reservationId);
            
            Message cancelRequest = new Message(MessageType.CANCEL_RESERVATION, reservationId);
            Message cancelResponse = sendRequest(cancelRequest);
            
            if (cancelResponse != null && cancelResponse.isSuccess()) {
                System.out.println("Reservation successfully cancelled.");
            } else {
                System.out.println("Failed to cancel reservation: " + 
                                 (cancelResponse != null ? cancelResponse.getStatusMessage() : "No response from server"));
            }
        } catch (Exception e) {
            System.err.println("Error during cancellation process: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Cancellation failed due to error.");
        }
    }
    
    private void listMyReservations() {
        System.out.print("\nEnter your email address: ");
        String email = scanner.nextLine();
        
        Message request = new Message(MessageType.GET_RESERVATIONS_BY_EMAIL, email);
        Message response = sendRequest(request);
        
        if (response != null && response.isSuccess()) {
            List<Reservation> reservations = (List<Reservation>) response.getPayload();
            if (reservations.isEmpty()) {
                System.out.println("No reservations found for email: " + email);
                return;
            }
            
            System.out.println("\nYour reservations:");
            System.out.println("=================");
            
            for (Reservation res : reservations) {
                System.out.println("ID: " + res.getReservationId() + 
                                 ", Movie: " + res.getScreening().getMovie().getTitle() +
                                 ", Time: " + res.getScreening().getScreeningTime() +
                                 ", Status: " + res.getStatus());
            }
        } else {
            System.out.println("Failed to get reservations: " + 
                             (response != null ? response.getStatusMessage() : "No response from server"));
        }
    }
    
    private Message sendRequest(Message request) {
        try {
            // Serialize the request
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(request);
            
            // Send the request
            byte[] sendData = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            
            // Receive the response
            byte[] receiveData = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            
            // Zwiększ timeout z 5 do 20 sekund
            clientSocket.setSoTimeout(20000);
            clientSocket.receive(receivePacket);
            
            // Deserialize the response
            ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Message) ois.readObject();
            
        } catch (SocketTimeoutException e) {
            System.out.println("Server did not respond. Please try again later.");
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error communicating with server: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
    
    public static void main(String[] args) {
        String serverHost = "localhost";
        int serverPort = 9876;
        
        // Allow custom server host and port via command line
        if (args.length >= 1) {
            serverHost = args[0];
        }
        if (args.length >= 2) {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + serverPort);
            }
        }
        
        UDPClient client = new UDPClient(serverHost, serverPort);
        client.start();
    }
}
