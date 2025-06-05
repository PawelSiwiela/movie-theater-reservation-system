package dao;

import models.Movie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {
    private Connection connection;
    
    public MovieDAO(Connection connection) {
        this.connection = connection;
    }
    
    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS movies (" +
                     "movieId INT PRIMARY KEY, " +
                     "title VARCHAR(255), " +
                     "duration INT, " +
                     "description TEXT, " +
                     "genre VARCHAR(100), " +
                     "director VARCHAR(100), " +
                     "releaseYear INT, " +
                     "language VARCHAR(50))";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    public void insert(Movie movie) throws SQLException {
        String sql = "INSERT INTO movies (movieId, title, duration, description, genre, director, releaseYear, language) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, movie.getMovieId());
            pstmt.setString(2, movie.getTitle());
            pstmt.setInt(3, movie.getDuration());
            pstmt.setString(4, movie.getDescription());
            pstmt.setString(5, movie.getGenre());
            pstmt.setString(6, movie.getDirector());
            pstmt.setInt(7, movie.getReleaseYear());
            pstmt.setString(8, movie.getLanguage());
            
            pstmt.executeUpdate();
        }
    }
    
    public List<Movie> findAll() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Movie movie = new Movie(
                    rs.getInt("movieId"),
                    rs.getString("title"),
                    rs.getInt("duration"),
                    rs.getString("description"),
                    rs.getString("genre"),
                    rs.getString("director"),
                    rs.getInt("releaseYear"),
                    rs.getString("language")
                );
                movies.add(movie);
            }
        }
        
        return movies;
    }
    
    public Movie findById(int id) throws SQLException {
        String sql = "SELECT * FROM movies WHERE movieId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Movie(
                        rs.getInt("movieId"),
                        rs.getString("title"),
                        rs.getInt("duration"),
                        rs.getString("description"),
                        rs.getString("genre"),
                        rs.getString("director"),
                        rs.getInt("releaseYear"),
                        rs.getString("language")
                    );
                }
            }
        }
        
        return null;
    }
    
    public void update(Movie movie) throws SQLException {
        String sql = "UPDATE movies SET title = ?, duration = ?, description = ?, " +
                     "genre = ?, director = ?, releaseYear = ?, language = ? " +
                     "WHERE movieId = ?";
                     
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, movie.getTitle());
            pstmt.setInt(2, movie.getDuration());
            pstmt.setString(3, movie.getDescription());
            pstmt.setString(4, movie.getGenre());
            pstmt.setString(5, movie.getDirector());
            pstmt.setInt(6, movie.getReleaseYear());
            pstmt.setString(7, movie.getLanguage());
            pstmt.setInt(8, movie.getMovieId());
            
            pstmt.executeUpdate();
        }
    }
    
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM movies WHERE movieId = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
