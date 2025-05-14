package models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Screening implements Serializable {
    private int screeningId;
    private Movie movie;
    private Room room;
    private LocalDateTime screeningTime;
    private double ticketPrice;
    private boolean[][] availableSeats;

    public Screening(int screeningId, Movie movie, Room room, LocalDateTime screeningTime, double ticketPrice) {
        this.screeningId = screeningId;
        this.movie = movie;
        this.room = room;
        this.screeningTime = screeningTime;
        this.ticketPrice = ticketPrice;
        this.availableSeats = new boolean[room.getRows()][room.getSeatsPerRow()];
        initializeSeats();
    }

    private void initializeSeats() {
        for (int i = 0; i < room.getRows(); i++) {
            for (int j = 0; j < room.getSeatsPerRow(); j++) {
                availableSeats[i][j] = true; // All seats are initially available
            }
        }
    }

    // Getters
    public int getScreeningId() {
        return screeningId;
    }

    public Movie getMovie() {
        return movie;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDateTime getScreeningTime() {
        return screeningTime;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public boolean[][] getAvailableSeats() {
        return availableSeats;
    }

    // Setters
    public void setScreeningId(int screeningId) {
        this.screeningId = screeningId;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setScreeningTime(LocalDateTime screeningTime) {
        this.screeningTime = screeningTime;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public void setAvailableSeats(boolean[][] availableSeats) {
        this.availableSeats = availableSeats;
    }

    // Utility methods
    public boolean isSeatAvailable(int row, int seatNumber) {
        if (row < 1 || row > room.getRows() || seatNumber < 1 || seatNumber > room.getSeatsPerRow()) {
            throw new IllegalArgumentException("Invalid seat position");
        }
        return availableSeats[row - 1][seatNumber - 1];
    }

    public void updateSeatStatus(int row, int seatNumber, boolean isAvailable) {
        if (row < 1 || row > room.getRows() || seatNumber < 1 || seatNumber > room.getSeatsPerRow()) {
            throw new IllegalArgumentException("Invalid seat position");
        }
        availableSeats[row - 1][seatNumber - 1] = isAvailable;
    }

    @Override
    public String toString() {
        return "Screening{" +
                "screeningId=" + screeningId +
                ", movie=" + movie +
                ", room=" + room +
                ", screeningTime=" + screeningTime +
                ", ticketPrice=" + ticketPrice +
                '}';
    }

}
