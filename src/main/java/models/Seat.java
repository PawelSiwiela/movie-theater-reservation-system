package models;

import java.io.Serializable;

public class Seat implements Serializable {
    private int row;
    private int number;
    private SeatStatus status;

    public Seat(int row, int number, SeatStatus status) {
        this.row = row;
        this.number = number;
        this.status = status;
    }

    // Getters
    public int getRow() {
        return row;
    }

    public int getNumber() {
        return number;
    }

    public SeatStatus getStatus() {
        return status;
    }

    // Setters
    public void setRow(int row) {
        this.row = row;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Seat{" +
                "row=" + row +
                ", number=" + number +
                ", status=" + status +
                '}';
    }
}
