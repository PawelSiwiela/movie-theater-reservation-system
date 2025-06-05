package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./cinemadb";
    private static final String USER = "sa";
    private static final String PASS = "";
    
    private Connection connection;
    
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
        }
        return connection;
    }
    
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void initDatabase() throws SQLException {
        Connection conn = getConnection();
        
        // Inicjalizacja tabel
        MovieDAO movieDAO = new MovieDAO(conn);
        movieDAO.createTable();
        
        RoomDAO roomDAO = new RoomDAO(conn);
        roomDAO.createTable();
        
        ScreeningDAO screeningDAO = new ScreeningDAO(conn);
        screeningDAO.createTable();
        
        ReservationDAO reservationDAO = new ReservationDAO(conn);
        reservationDAO.createTable();
    }
}
