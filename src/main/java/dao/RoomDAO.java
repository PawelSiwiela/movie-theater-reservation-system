package dao;

import models.Room;
import models.Seat;
import models.SeatStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {
    private Connection connection;
    
    public RoomDAO(Connection connection) {
        this.connection = connection;
    }
    
    public void createTable() throws SQLException {
        // Tabela dla sal
        String roomSql = "CREATE TABLE IF NOT EXISTS rooms (" +
                         "roomId INT PRIMARY KEY, " +
                         "roomName VARCHAR(100), " +
                         "rows INT, " +
                         "seatsPerRow INT)";
        
        // Tabela dla miejsc (opcjonalnie - można też przechowywać miejsca w pamięci)
        String seatsSql = "CREATE TABLE IF NOT EXISTS seats (" +
                          "seatId INT AUTO_INCREMENT PRIMARY KEY, " +
                          "roomId INT, " +
                          "seat_row INT, " +  // Changed 'row' to 'seat_row' to avoid reserved keyword
                          "number INT, " +
                          "status VARCHAR(20), " +
                          "FOREIGN KEY (roomId) REFERENCES rooms(roomId))";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(roomSql);
            stmt.execute(seatsSql);
        }
    }
    
    public void insert(Room room) throws SQLException {
        // Wstaw informacje o sali
        String sql = "INSERT INTO rooms (roomId, roomName, rows, seatsPerRow) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, room.getRoomId());
            pstmt.setString(2, room.getRoomName());
            pstmt.setInt(3, room.getRows());
            pstmt.setInt(4, room.getSeatsPerRow());
            pstmt.executeUpdate();
        }
        
        // Opcjonalnie: zapisz informacje o miejscach
        // Można to pominąć, jeśli miejsca są inicjalizowane dynamicznie
        insertSeats(room);
    }
    
    private void insertSeats(Room room) throws SQLException {
        String sql = "INSERT INTO seats (roomId, seat_row, number, status) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < room.getRows(); i++) {
                for (int j = 0; j < room.getSeatsPerRow(); j++) {
                    Seat seat = room.getSeats()[i][j];
                    pstmt.setInt(1, room.getRoomId());
                    pstmt.setInt(2, seat.getRow());
                    pstmt.setInt(3, seat.getNumber());
                    pstmt.setString(4, seat.getStatus().name());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
    }
    
    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM rooms";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int roomId = rs.getInt("roomId");
                String roomName = rs.getString("roomName");
                int rows = rs.getInt("rows");
                int seatsPerRow = rs.getInt("seatsPerRow");
                
                Room room = new Room(roomId, roomName, rows, seatsPerRow);
                // Opcjonalnie: wczytaj miejsca z bazy danych
                loadSeats(room);
                rooms.add(room);
            }
        }
        
        return rooms;
    }
    
    private void loadSeats(Room room) throws SQLException {
        String sql = "SELECT * FROM seats WHERE roomId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, room.getRoomId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int row = rs.getInt("seat_row"); // Change here
                    int number = rs.getInt("number");
                    SeatStatus status = SeatStatus.valueOf(rs.getString("status"));
                    
                    // Aktualizuj miejsce w sali
                    if (row > 0 && row <= room.getRows() && 
                        number > 0 && number <= room.getSeatsPerRow()) {
                        room.updateSeatStatus(row, number, status);
                    }
                }
            }
        }
    }
    
    public Room findById(int id) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE roomId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int roomId = rs.getInt("roomId");
                    String roomName = rs.getString("roomName");
                    int rows = rs.getInt("rows");
                    int seatsPerRow = rs.getInt("seatsPerRow");
                    
                    Room room = new Room(roomId, roomName, rows, seatsPerRow);
                    loadSeats(room);
                    return room;
                }
            }
        }
        
        return null;
    }
    
    public void update(Room room) throws SQLException {
        String sql = "UPDATE rooms SET roomName = ?, rows = ?, seatsPerRow = ? WHERE roomId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getRoomName());
            pstmt.setInt(2, room.getRows());
            pstmt.setInt(3, room.getSeatsPerRow());
            pstmt.setInt(4, room.getRoomId());
            
            pstmt.executeUpdate();
        }
        
        // Aktualizuj miejsca - najpierw usuń stare
        deleteSeats(room.getRoomId());
        // Następnie wstaw nowe
        insertSeats(room);
    }
    
    private void deleteSeats(int roomId) throws SQLException {
        String sql = "DELETE FROM seats WHERE roomId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate();
        }
    }
    
    public void delete(int id) throws SQLException {
        // Najpierw usuń miejsca
        deleteSeats(id);
        
        // Następnie usuń salę
        String sql = "DELETE FROM rooms WHERE roomId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
