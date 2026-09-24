package com.movie.movieticket.service;

import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.SeatReservation;

import java.util.List;

public interface SeatService {
    
    List<Seat> getAllSeats();
    
    Seat getSeatById(Long id);
    
    List<Seat> getSeatsByScreening(Screening screening);
    
    List<Seat> getSeatsByScreeningId(Long screeningId);
    
    List<Seat> getSeatsByBookingId(Long bookingId);
    
    List<Seat> getSeatsByIds(List<Long> seatIds);
    
    Seat saveSeat(Seat seat);
    
    void deleteSeat(Long id);
    
    void initializeSeatsForScreening(Screening screening);
    
    boolean areSeatsAvailable(List<Long> seatIds);
    
    // New methods for temporary reservations
    void createTemporaryReservations(List<Seat> seats, Long bookingId);
    
    void releaseTemporaryReservations(Long bookingId);
    
    void cleanupExpiredReservations();
    
    // NEW METHODS for admin seat management
    List<Seat> getAvailableSeatsByScreeningId(Long screeningId);
    
    List<Seat> getBookedSeatsByScreeningId(Long screeningId);
    
    int getAvailableSeatsCount(Long screeningId);
    
    int getBookedSeatsCount(Long screeningId);
    
    void updateSeatStatus(Long seatId, boolean isBooked, Long bookingId);
    
    void bulkUpdateSeatStatus(List<Long> seatIds, boolean isBooked, Long bookingId);
}