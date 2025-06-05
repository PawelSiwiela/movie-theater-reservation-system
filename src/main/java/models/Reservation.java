package models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reservation implements Serializable {
    private String reservationId;
    private Screening screening;
    private List<Seat> reservedSeats;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime reservationTime;
    private ReservationStatus status;
    private double totalPrice;

    // Constructor
    public Reservation(Screening screening, List<Seat> reservedSeats, 
                       String customerName, String customerEmail, String customerPhone) {
        this.reservationId = UUID.randomUUID().toString();
        this.screening = screening;
        this.reservedSeats = new ArrayList<>(reservedSeats);
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.reservationTime = LocalDateTime.now();
        this.status = ReservationStatus.PENDING;
        calculateTotalPrice();
    }

    // Calculate total price based on number of seats and screening price
    private void calculateTotalPrice() {
        this.totalPrice = screening.getTicketPrice() * reservedSeats.size();
    }

    // Getters
    public String getReservationId() {
        return reservationId;
    }

    public Screening getScreening() {
        return screening;
    }

    public List<Seat> getReservedSeats() {
        return reservedSeats;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    // Setters
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public void setScreening(Screening screening) {
        this.screening = screening;
        calculateTotalPrice(); // Recalculate price if screening changes
    }

    public void setReservedSeats(List<Seat> reservedSeats) {
        this.reservedSeats = new ArrayList<>(reservedSeats);
        calculateTotalPrice(); // Recalculate price if seats change
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public void setReservationTime(LocalDateTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    // Utility methods
    public void confirmReservation() {
        this.status = ReservationStatus.CONFIRMED;
        
        // Update seat status in the screening
        for (Seat seat : reservedSeats) {
            screening.updateSeatStatus(seat.getRow(), seat.getNumber(), false);
            seat.setStatus(SeatStatus.OCCUPIED);
        }
    }

    public void cancelReservation() {
        this.status = ReservationStatus.CANCELLED;
        
        // Free up the seats
        for (Seat seat : reservedSeats) {
            screening.updateSeatStatus(seat.getRow(), seat.getNumber(), true);
            seat.setStatus(SeatStatus.AVAILABLE);
        }
    }
    
    public boolean addSeat(Seat seat) {
        if (screening.isSeatAvailable(seat.getRow(), seat.getNumber())) {
            reservedSeats.add(seat);
            seat.setStatus(SeatStatus.RESERVED);
            screening.updateSeatStatus(seat.getRow(), seat.getNumber(), false);
            calculateTotalPrice();
            return true;
        }
        return false;
    }
    
    public boolean removeSeat(Seat seat) {
        boolean removed = reservedSeats.removeIf(s -> 
            s.getRow() == seat.getRow() && s.getNumber() == seat.getNumber());
            
        if (removed) {
            seat.setStatus(SeatStatus.AVAILABLE);
            screening.updateSeatStatus(seat.getRow(), seat.getNumber(), true);
            calculateTotalPrice();
        }
        return removed;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", screening=" + screening.getScreeningId() +
                ", movie='" + screening.getMovie().getTitle() + '\'' +
                ", seats=" + reservedSeats.size() +
                ", customer='" + customerName + '\'' +
                ", status=" + status +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
