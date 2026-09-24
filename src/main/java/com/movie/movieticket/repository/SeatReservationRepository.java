package com.movie.movieticket.repository;

import com.movie.movieticket.model.SeatReservation;
import com.movie.movieticket.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
    
    List<SeatReservation> findByBookingId(Long bookingId);
    
    List<SeatReservation> findByBooking(Booking booking);
    
    List<SeatReservation> findByReservationTimeBefore(LocalDateTime time);
    
    @Modifying
    @Transactional
    void deleteByBookingId(Long bookingId);
    
    @Modifying
    @Transactional
    void deleteByBooking(Booking booking);
    
    @Query("SELECT sr FROM SeatReservation sr WHERE sr.reservationTime < :cutoffTime")
    List<SeatReservation> findExpiredReservations(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatReservation sr WHERE sr.reservationTime < :cutoffTime")
    void deleteExpiredReservations(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    boolean existsByBookingId(Long bookingId);
    
    boolean existsByBooking(Booking booking);
    
    @Query("SELECT COUNT(sr) FROM SeatReservation sr WHERE sr.booking.id = :bookingId")
    long countByBookingId(@Param("bookingId") Long bookingId);
}