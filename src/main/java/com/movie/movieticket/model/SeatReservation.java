package com.movie.movieticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_reservations")
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private LocalDateTime reservationTime;

    // Constructors
    public SeatReservation() {
        this.reservationTime = LocalDateTime.now();
    }

    public SeatReservation(Seat seat, Booking booking) {
        this();
        this.seat = seat;
        this.booking = booking;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(LocalDateTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    @Override
    public String toString() {
        return "SeatReservation{" +
                "id=" + id +
                ", seat=" + (seat != null ? seat.getSeatNumber() : "null") +
                ", booking=" + (booking != null ? booking.getId() : "null") +
                ", reservationTime=" + reservationTime +
                '}';
    }
}