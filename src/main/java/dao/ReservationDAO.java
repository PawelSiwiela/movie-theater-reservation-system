package dao;

import models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    private Connection connection;
    private ScreeningDAO screeningDAO;
    
    public ReservationDAO(Connection connection) {
        this.connection = connection;
        this.screeningDAO = new ScreeningDAO(connection);
    }
    
    public void createTable() throws SQLException {
        // Tabela dla rezerwacji
        String reservationSql = "CREATE TABLE IF NOT EXISTS reservations (" +
                                "reservationId VARCHAR(36) PRIMARY KEY, " +
                                "screeningId INT, " +
                                "customerName VARCHAR(255), " +
                                "customerEmail VARCHAR(255), " +
                                "customerPhone VARCHAR(50), " +
                                "reservationTime TIMESTAMP, " +
                                "status VARCHAR(20), " +
                                "totalPrice DOUBLE, " +
                                "FOREIGN KEY (screeningId) REFERENCES screenings(screeningId))";
        
        // For the reserved_seats table
        String reservedSeatsSql = "CREATE TABLE IF NOT EXISTS reserved_seats (" +
                                  "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                  "reservationId VARCHAR(36), " +
                                  "seat_row INT, " +  // Changed here
                                  "number INT, " +
                                  "FOREIGN KEY (reservationId) REFERENCES reservations(reservationId))";
        
        // Tabela dla miejsc
        String seatsSql = "CREATE TABLE IF NOT EXISTS seats (" +
                          "seatId INT AUTO_INCREMENT PRIMARY KEY, " +
                          "roomId INT, " +
                          "seat_row INT, " +  // Zmiana nazwy kolumny
                          "number INT, " +
                          "status VARCHAR(20), " +
                          "FOREIGN KEY (roomId) REFERENCES rooms(roomId))";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(reservationSql);
            stmt.execute(reservedSeatsSql);
            stmt.execute(seatsSql);
        }
    }
    
    public void insert(Reservation reservation) throws SQLException {
        System.out.println("Inserting reservation: " + reservation.getReservationId());
        connection.setAutoCommit(false);
        
        try {
            // Wstaw główny rekord rezerwacji
            String sql = "INSERT INTO reservations (reservationId, screeningId, customerName, " +
                         "customerEmail, customerPhone, reservationTime, status, totalPrice) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, reservation.getReservationId());
                pstmt.setInt(2, reservation.getScreening().getScreeningId());
                pstmt.setString(3, reservation.getCustomerName());
                pstmt.setString(4, reservation.getCustomerEmail());
                pstmt.setString(5, reservation.getCustomerPhone());
                pstmt.setTimestamp(6, Timestamp.valueOf(reservation.getReservationTime()));
                pstmt.setString(7, reservation.getStatus().name());
                pstmt.setDouble(8, reservation.getTotalPrice());
                
                pstmt.executeUpdate();
            }
            
            // Wstaw zarezerwowane miejsca
            insertReservedSeats(reservation);
            
            connection.commit();
            System.out.println("Reservation inserted successfully");
        } catch (SQLException e) {
            System.err.println("Error inserting reservation: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState() + ", Error Code: " + e.getErrorCode());
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    private void insertReservedSeats(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reserved_seats (reservationId, seat_row, number) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Seat seat : reservation.getReservedSeats()) {
                pstmt.setString(1, reservation.getReservationId());
                pstmt.setInt(2, seat.getRow());
                pstmt.setInt(3, seat.getNumber());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    public List<Reservation> findAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Reservation reservation = buildReservationFromResultSet(rs);
                if (reservation != null) {
                    reservations.add(reservation);
                }
            }
        }
        
        return reservations;
    }
    
    public Reservation findById(String id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE reservationId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return buildReservationFromResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    private Reservation buildReservationFromResultSet(ResultSet rs) throws SQLException {
        String reservationId = rs.getString("reservationId");
        int screeningId = rs.getInt("screeningId");
        String customerName = rs.getString("customerName");
        String customerEmail = rs.getString("customerEmail");
        String customerPhone = rs.getString("customerPhone");
        LocalDateTime reservationTime = rs.getTimestamp("reservationTime").toLocalDateTime();
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
        
        // Pobierz seans
        Screening screening = screeningDAO.findById(screeningId);
        if (screening == null) return null;
        
        // Pobierz zarezerwowane miejsca
        List<Seat> reservedSeats = findReservedSeatsByReservationId(reservationId);
        
        // Utwórz obiekt rezerwacji
        Reservation reservation = new Reservation(screening, reservedSeats, customerName, customerEmail, customerPhone);
        // Ustaw pola, które nie są ustawiane w konstruktorze
        reservation.setReservationId(reservationId);
        reservation.setReservationTime(reservationTime);
        reservation.setStatus(status);
        
        return reservation;
    }
    
    private List<Seat> findReservedSeatsByReservationId(String reservationId) throws SQLException {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT * FROM reserved_seats WHERE reservationId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reservationId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int row = rs.getInt("seat_row");  // Changed here
                    int number = rs.getInt("number");
                    
                    seats.add(new Seat(row, number, SeatStatus.RESERVED));
                }
            }
        }
        
        return seats;
    }
    
    public void updateStatus(String id, ReservationStatus status) throws SQLException {
        // Dodaj więcej logowania, aby sprawdzić, co się dzieje
        System.out.println("Updating reservation status in database. ID: " + id + ", New status: " + status);
        
        String sql = "UPDATE reservations SET status = ? WHERE reservationId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, id);
            
            int updatedRows = pstmt.executeUpdate();
            System.out.println("Updated " + updatedRows + " rows in database");
            
            if (updatedRows == 0) {
                System.out.println("Warning: No rows were updated for ID: " + id);
                // Spróbuj znaleźć rekord w bazie
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM reservations WHERE reservationId = ?")) {
                    checkStmt.setString(1, id);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            System.out.println("Record exists in database but was not updated");
                        } else {
                            System.out.println("Record does not exist in database");
                        }
                    }
                }
            }
        }
    }
    
    public void delete(String id) throws SQLException {
        connection.setAutoCommit(false);
        
        try {
            // Najpierw usuń powiązane miejsca
            String seatsSQL = "DELETE FROM reserved_seats WHERE reservationId = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(seatsSQL)) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }
            
            // Następnie usuń rezerwację
            String reservationSQL = "DELETE FROM reservations WHERE reservationId = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(reservationSQL)) {
                pstmt.setString(1, id);
                pstmt.executeUpdate();
            }
            
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
