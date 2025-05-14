package models;

import java.io.Serializable;

public class Room implements Serializable {
    private int roomId;
    private String roomName;
    private int rows;
    private int seatsPerRow;
    private Seat[][] seats;
    
    public Room(int roomId, String roomName, int rows, int seatsPerRow) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seats = new Seat[rows][seatsPerRow];
        initializeSeats();
    }

    private void initializeSeats() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < seatsPerRow; j++) {
                seats[i][j] = new Seat(i + 1, j + 1, SeatStatus.AVAILABLE);
            }
        }
    }

    // Getters
    public int getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getRows() {
        return rows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public Seat[][] getSeats() {
        return seats;
    }

    // Setters
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setSeatsPerRow(int seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public void setSeats(Seat[][] seats) {
        this.seats = seats;
    }

    // Utility methods
    public Seat getSeat(int row, int number) {
        if (row < 1 || row > rows || number < 1 || number > seatsPerRow) {
            throw new IllegalArgumentException("Invalid seat position");
        }
        return seats[row - 1][number - 1];
    }

    public boolean updateSeatStatus(int row, int number, SeatStatus status) {
        Seat seat = getSeat(row, number);
        if (seat != null) {
            seat.setStatus(status);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                ", rows=" + rows +
                ", seatsPerRow=" + seatsPerRow +
                '}';
    }

}
