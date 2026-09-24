package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.User;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    
    Booking saveBooking(Booking booking);
    
    List<Booking> getAllBookings();
    
    Booking getBookingById(Long id);
    
    List<Booking> getBookingsByUser(User user);
    
    void deleteBooking(Long id);
    
    // Payment related methods
    Booking updateBookingPaymentStatus(Long bookingId, String paymentStatus, String paymentMethod);
    
    List<Booking> getBookingsByPaymentStatus(String paymentStatus);
    
    List<Booking> getBookingsByPaymentMethod(String paymentMethod);
    
    // New method for Pay at Counter implementation
    List<Booking> getBookingsByPaymentMethodAndStatus(String paymentMethod, String paymentStatus);
    
    // New method to get valid pending counter payments (filtered by timing)
    List<Booking> getValidPendingCounterPayments();
    
    // New method for No-Show management
    List<Booking> getBookingsByStatus(String status);
    
    // Paginated version for admin pages
    List<Booking> getBookingsByStatus(String status, Pageable pageable);
    
    // Admin dashboard methods
    long getBookingCount();
    
    List<Booking> getRecentBookings(int limit);
    
    long getTodayBookingCount();
    
    long getBookingCountBetweenDates(LocalDateTime start, LocalDateTime end);
    
    List<Booking> getBookingsByMovieId(Long movieId);
    
    List<Booking> getBookingsByScreeningId(Long screeningId);
    
    // Release seats for a booking
    void releaseSeats(Long bookingId);
    
    // New methods for no-show dashboard
    long countNoShowBookings();
    
    long countPendingNoShowBookings();
    
    long countPendingPayments();
    
    long countPendingCounterPayments();
    
    BigDecimal calculateRevenueForPeriod(LocalDateTime start, LocalDateTime end);
    
    List<Booking> findRecentNoShowBookings(int limit);
    
    // Additional methods for no-show management
    List<Booking> getNoShowBookingsByProcessedStatus(boolean processed, Pageable pageable);
    
    List<Booking> getNoShowBookingsByUser(Long userId);
    
    // Add new methods for filtering no-shows
    List<Booking> getNoShowBookingsBetweenDates(LocalDateTime fromDate, LocalDateTime toDate);
    
    List<Booking> getNoShowBookingsBetweenDatesByPaymentStatus(LocalDateTime fromDate, LocalDateTime toDate, boolean isPaid);
    
    // Add method to find potential no-shows
    List<Booking> getPotentialNoShows(LocalDateTime cutoffTime);
    
    // Add method to find no-show bookings by user email
    List<Booking> getNoShowBookingsByUserEmail(String email);
    
    // Add method to find all bookings marked as no-show
    List<Booking> getAllMarkedAsNoShow();
}