package com.movie.movieticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screenings")
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
    
    @ManyToOne
    @JoinColumn(name = "cinema_id", nullable = true) // Make nullable temporarily for migration
    private Cinema cinema;

    @Column(nullable = false)
    private LocalDateTime screeningTime;

    @Column(nullable = true) // Changed from nullable = false to nullable = true
    private String theater;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;
    
    @OneToMany(mappedBy = "screening", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();
    
    @OneToMany(mappedBy = "screening", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();

    // Constructors
    public Screening() {
        // Set default theater value
        this.theater = "Main Theater";
    }

    public Screening(Movie movie, Cinema cinema, LocalDateTime screeningTime, String theater, Double price, Integer totalSeats) {
        this.movie = movie;
        this.cinema = cinema;
        this.screeningTime = screeningTime;
        this.theater = theater != null ? theater : (cinema != null ? cinema.getName() : "Main Theater");
        this.price = price;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats; // Initially all seats are available
    }

    // Legacy constructor for backward compatibility
    public Screening(Movie movie, LocalDateTime screeningTime, String theater, double price, int totalSeats) {
        this.movie = movie;
        this.screeningTime = screeningTime;
        this.theater = theater != null ? theater : "Main Theater";
        this.price = price;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }
    
    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
        // Set theater name to match cinema name for consistency
        if (cinema != null) {
            this.theater = cinema.getName();
        }
    }

    public LocalDateTime getScreeningTime() {
        return screeningTime;
    }

    public void setScreeningTime(LocalDateTime screeningTime) {
        this.screeningTime = screeningTime;
    }

    public String getTheater() {
        // Return cinema name if theater is not set or is default
        if (cinema != null && (theater == null || theater.equals("Main Theater"))) {
            return cinema.getName();
        }
        return theater;
    }

    public void setTheater(String theater) {
        this.theater = theater != null ? theater : "Main Theater";
    }
    
    // Alias for getTheater() to support legacy code
    public String getHall() {
        return getTheater();
    }
    
    // Alias for setTheater() to support legacy code
    public void setHall(String hall) {
        this.theater = hall != null ? hall : "Main Theater";
    }

    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }
    
    public List<Seat> getSeats() {
        return seats;
    }
    
    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }
    
    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setScreening(this);
    }
    
    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }

    // Helper methods
    public boolean isAvailable() {
        return availableSeats > 0;
    }

    public boolean bookSeats(int numberOfSeats) {
        if (availableSeats >= numberOfSeats) {
            availableSeats -= numberOfSeats;
            return true;
        }
        return false;
    }
    
    // NEW METHOD: Get cinema name for consistency
    public String getCinemaName() {
        return cinema != null ? cinema.getName() : "Unknown Cinema";
    }
    
    // NEW METHOD: Sync available seats with actual seat data
    public void syncAvailableSeats(List<Seat> seats) {
        if (seats != null) {
            this.totalSeats = seats.size();
            this.availableSeats = (int) seats.stream().filter(seat -> !seat.isBooked()).count();
        }
    }
    
    /**
     * Checks if the screening is available for booking based on current time.
     * A screening is available if:
     * 1. The screening date is in the future, or
     * 2. The screening is today but the time hasn't passed yet
     * 
     * @return true if the screening is available for booking, false otherwise
     */
    public boolean isTimeAvailable() {
        LocalDateTime now = LocalDateTime.now();
        
        // If screening time is in the future
        return screeningTime.isAfter(now);
    }

    /**
     * Checks if the screening is fully available for booking.
     * A screening is fully available if:
     * 1. It is time-available (not in the past)
     * 2. It has available seats
     * 
     * @return true if the screening is fully available for booking, false otherwise
     */
    public boolean isFullyAvailable() {
        return isTimeAvailable() && isAvailable();
    }
    
    /**
     * Gets the screening date as a LocalDate object.
     * 
     * @return the screening date
     */
    public LocalDate getScreeningDate() {
        return screeningTime.toLocalDate();
    }
    
    /**
     * Gets the start time as a formatted string.
     * 
     * @return the start time
     */
    public String getStartTime() {
        return screeningTime.toLocalTime().toString();
    }
    
    /**
     * Initializes seats for this screening based on the total seats.
     * This method creates seat objects for each seat in the screening.
     */
    public void initializeSeats() {
        // Clear existing seats if any
        this.seats.clear();
        
        // Get the total seats to create
        int totalSeatsToCreate = this.totalSeats;
        if (totalSeatsToCreate <= 0 && this.cinema != null) {
            totalSeatsToCreate = this.cinema.getCapacity();
        }
        if (totalSeatsToCreate <= 0) {
            totalSeatsToCreate = 100; // Default fallback
        }
        
        // Calculate optimal layout for the total seats
        int seatsPerRow = 10;
        int totalRows = (int) Math.ceil((double) totalSeatsToCreate / seatsPerRow);
        
        // Ensure we don't exceed 26 rows (A-Z)
        if (totalRows > 26) {
            seatsPerRow = (int) Math.ceil((double) totalSeatsToCreate / 26);
            totalRows = 26;
        }
        
        int seatCount = 0;
        for (int rowIndex = 0; rowIndex < totalRows && seatCount < totalSeatsToCreate; rowIndex++) {
            // Convert row index to letter (A, B, C, etc.)
            char rowLetter = (char) ('A' + rowIndex);
            String row = String.valueOf(rowLetter);
            
            // Calculate seats for this row
            int seatsInThisRow = Math.min(seatsPerRow, totalSeatsToCreate - seatCount);
            
            for (int col = 1; col <= seatsInThisRow; col++) {
                Seat seat = new Seat();
                seat.setScreening(this);
                seat.setRow(row);
                seat.setSeatColumn(col);
                seat.setSeatNumber(row + col);
                seat.setBooked(false);
                seat.setPrice(this.price);
                seat.setType("standard");
                
                this.addSeat(seat);
                seatCount++;
            }
        }
        
        // Update totals
        this.totalSeats = seatCount;
        this.availableSeats = seatCount;
    }
}
