package com.movie.movieticket.dto;

public class SeatDTO {
    private Long id;
    private String seatNumber;
    private String row;
    private int seatColumn;
    private boolean booked;
    private String type;
    private double price;
    private Long bookingId;

    // Constructors
    public SeatDTO() {}

    public SeatDTO(Long id, String seatNumber, String row, int seatColumn, 
                   boolean booked, String type, double price, Long bookingId) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.row = row;
        this.seatColumn = seatColumn;
        this.booked = booked;
        this.type = type;
        this.price = price;
        this.bookingId = bookingId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
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

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
}