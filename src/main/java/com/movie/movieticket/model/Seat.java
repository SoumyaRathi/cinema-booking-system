package com.movie.movieticket.model;

import jakarta.persistence.*;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(name = "row_letter", nullable = false)
    private String row;

    @Column(name = "seat_column", nullable = false)
    private int seatColumn;

    @Column(name = "is_booked", nullable = false)
    private boolean isBooked = false;

    @Column(name = "booked", nullable = false)
    private boolean booked = false;  // Map to the booked column as well

    @Column(name = "number", nullable = false)
    private int number;  // Add the required number field

    @Column(name = "booking_id", nullable = true)
    private Long bookingId;

    @Column(nullable = false)
    private String type = "standard"; // standard, premium, accessible

    @Column(nullable = false)
    private double price;

    // Constructors
    public Seat() {
    }

    public Seat(Screening screening, String seatNumber) {
        this.screening = screening;
        this.seatNumber = seatNumber;
        // Parse row and column from seatNumber (e.g., "A1" -> row="A", seatColumn=1)
        if (seatNumber != null && seatNumber.length() >= 2) {
            this.row = seatNumber.substring(0, 1);
            try {
                this.seatColumn = Integer.parseInt(seatNumber.substring(1));
                this.number = this.seatColumn;  // Set number to match seatColumn as a default
            } catch (NumberFormatException e) {
                this.seatColumn = 0;
                this.number = 0;
            }
        }
        this.isBooked = false;
        this.booked = false;
    }

    public Seat(Screening screening, String seatNumber, String row, int seatColumn) {
        this.screening = screening;
        this.seatNumber = seatNumber;
        this.row = row;
        this.seatColumn = seatColumn;
        this.number = seatColumn;  // Set number to match seatColumn as a default
        this.isBooked = false;
        this.booked = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Screening getScreening() {
        return screening;
    }

    public void setScreening(Screening screening) {
        this.screening = screening;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public int getSeatColumn() {
        return seatColumn;
    }

    public void setSeatColumn(int seatColumn) {
        this.seatColumn = seatColumn;
        this.number = seatColumn;  // Update number when seatColumn is updated
    }

    public boolean isBooked() {
        return isBooked;
    }

    public void setBooked(boolean booked) {
        this.isBooked = booked;
        this.booked = booked;  // Update both fields
    }

    public boolean getBooked() {
        return booked;
    }

    public void setIsBooked(boolean isBooked) {
        this.isBooked = isBooked;
        this.booked = isBooked;  // Update both fields
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Seat{" +
                "id=" + id +
                ", seatNumber='" + seatNumber + '\'' +
                ", row='" + row + '\'' +
                ", seatColumn=" + seatColumn +
                ", number=" + number +
                ", isBooked=" + isBooked +
                ", booked=" + booked +
                ", type='" + type + '\'' +
                ", price=" + price +
                '}';
    }
}