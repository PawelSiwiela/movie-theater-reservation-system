package dao;

import models.Movie;
import models.Room;
import models.Screening;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScreeningDAO {
    private Connection connection;
    private MovieDAO movieDAO;
    private RoomDAO roomDAO;
    
    public ScreeningDAO(Connection connection) {
        this.connection = connection;
        this.movieDAO = new MovieDAO(connection);
        this.roomDAO = new RoomDAO(connection);
    }
    
    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS screenings (" +
                     "screeningId INT PRIMARY KEY, " +
                     "movieId INT, " +
                     "roomId INT, " +
                     "screeningTime TIMESTAMP, " +
                     "ticketPrice DOUBLE, " +
                     "FOREIGN KEY (movieId) REFERENCES movies(movieId), " +
                     "FOREIGN KEY (roomId) REFERENCES rooms(roomId))";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    public void insert(Screening screening) throws SQLException {
        String sql = "INSERT INTO screenings (screeningId, movieId, roomId, screeningTime, ticketPrice) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, screening.getScreeningId());
            pstmt.setInt(2, screening.getMovie().getMovieId());
            pstmt.setInt(3, screening.getRoom().getRoomId());
            pstmt.setTimestamp(4, Timestamp.valueOf(screening.getScreeningTime()));
            pstmt.setDouble(5, screening.getTicketPrice());
            
            pstmt.executeUpdate();
        }
    }
    
    public List<Screening> findAll() throws SQLException {
        List<Screening> screenings = new ArrayList<>();
        String sql = "SELECT * FROM screenings";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int screeningId = rs.getInt("screeningId");
                int movieId = rs.getInt("movieId");
                int roomId = rs.getInt("roomId");
                LocalDateTime screeningTime = rs.getTimestamp("screeningTime").toLocalDateTime();
                double ticketPrice = rs.getDouble("ticketPrice");
                
                Movie movie = movieDAO.findById(movieId);
                Room room = roomDAO.findById(roomId);
                
                if (movie != null && room != null) {
                    Screening screening = new Screening(screeningId, movie, room, screeningTime, ticketPrice);
                    screenings.add(screening);
                }
            }
        }
        
        return screenings;
    }
    
    public List<Screening> findByMovieId(int movieId) throws SQLException {
        List<Screening> screenings = new ArrayList<>();
        String sql = "SELECT * FROM screenings WHERE movieId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int screeningId = rs.getInt("screeningId");
                    int roomId = rs.getInt("roomId");
                    LocalDateTime screeningTime = rs.getTimestamp("screeningTime").toLocalDateTime();
                    double ticketPrice = rs.getDouble("ticketPrice");
                    
                    Movie movie = movieDAO.findById(movieId);
                    Room room = roomDAO.findById(roomId);
                    
                    if (movie != null && room != null) {
                        Screening screening = new Screening(screeningId, movie, room, screeningTime, ticketPrice);
                        screenings.add(screening);
                    }
                }
            }
        }
        
        return screenings;
    }
    
    public Screening findById(int id) throws SQLException {
        String sql = "SELECT * FROM screenings WHERE screeningId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int movieId = rs.getInt("movieId");
                    int roomId = rs.getInt("roomId");
                    LocalDateTime screeningTime = rs.getTimestamp("screeningTime").toLocalDateTime();
                    double ticketPrice = rs.getDouble("ticketPrice");
                    
                    Movie movie = movieDAO.findById(movieId);
                    Room room = roomDAO.findById(roomId);
                    
                    if (movie != null && room != null) {
                        return new Screening(id, movie, room, screeningTime, ticketPrice);
                    }
                }
            }
        }
        
        return null;
    }
    
    public void update(Screening screening) throws SQLException {
        String sql = "UPDATE screenings SET movieId = ?, roomId = ?, screeningTime = ?, ticketPrice = ? " +
                     "WHERE screeningId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, screening.getMovie().getMovieId());
            pstmt.setInt(2, screening.getRoom().getRoomId());
            pstmt.setTimestamp(3, Timestamp.valueOf(screening.getScreeningTime()));
            pstmt.setDouble(4, screening.getTicketPrice());
            pstmt.setInt(5, screening.getScreeningId());
            
            pstmt.executeUpdate();
        }
    }
    
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM screenings WHERE screeningId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
