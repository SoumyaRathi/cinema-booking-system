package com.movie.movieticket.repository;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUser(User user);
    
    List<Booking> findByUserOrderByBookingTimeDesc(User user);
    
    List<Booking> findByStatus(String status);
    
    List<Booking> findByStatusOrderByBookingTimeDesc(String status, Pageable pageable);
    
    List<Booking> findByPaid(boolean paid);
    
    List<Booking> findByPaymentMethod(String paymentMethod);
    
    List<Booking> findByPaymentMethodAndPaid(String paymentMethod, boolean paid);
    
    @Query("SELECT b FROM Booking b WHERE b.screening.movie.id = :movieId")
    List<Booking> findByMovieId(@Param("movieId") Long movieId);
    
    @Query("SELECT b FROM Booking b WHERE b.screening.id = :screeningId")
    List<Booking> findByScreeningId(@Param("screeningId") Long screeningId);
    
    @Query("SELECT COUNT(b) FROM Booking b")
    long countBookings();
    
    List<Booking> findTop5ByOrderByBookingTimeDesc();
    
    @Query("SELECT b FROM Booking b WHERE b.bookingTime BETWEEN :startDate AND :endDate")
    List<Booking> findByBookingTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingTime BETWEEN :startDate AND :endDate")
    long countByBookingTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // No-show related queries
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.screening.screeningTime < :currentTime AND b.markedAsNoShow = false")
    List<Booking> findPendingNoShows(@Param("currentTime") LocalDateTime currentTime);
    
    List<Booking> findByMarkedAsNoShowTrueAndNoShowProcessedFalse();
    
    long countByStatus(String status);
    
    long countByMarkedAsNoShowTrueAndNoShowProcessedFalse();
    
    long countByPaidFalseAndStatusNot(String status);
    
    long countByPaidFalseAndPaymentMethod(String paymentMethod);
    
    List<Booking> findByBookingTimeBetweenAndPaidTrue(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.markedAsNoShow = true ORDER BY b.bookingTime DESC")
    List<Booking> findNoShowBookingsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.markedAsNoShow = true AND b.noShowProcessed = :processed")
    List<Booking> findByMarkedAsNoShowAndProcessed(@Param("processed") boolean processed, Pageable pageable);
    
    // Add new methods for filtering no-shows
    @Query("SELECT b FROM Booking b WHERE b.status = 'NO_SHOW' AND b.screening.screeningTime BETWEEN :fromDate AND :toDate")
    List<Booking> findByStatusNoShowAndScreeningTimeBetween(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'NO_SHOW' AND b.screening.screeningTime BETWEEN :fromDate AND :toDate AND b.paid = :isPaid")
    List<Booking> findByStatusNoShowAndScreeningTimeBetweenAndPaid(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("isPaid") boolean isPaid);
    
    // Add new method to find no-show bookings by user email
    @Query("SELECT b FROM Booking b WHERE b.user.email = :email AND (b.markedAsNoShow = true OR b.status = 'NO_SHOW')")
    List<Booking> findNoShowBookingsByUserEmail(@Param("email") String email);
    
    // Add method to find all bookings with markedAsNoShow = true
    @Query("SELECT b FROM Booking b WHERE b.markedAsNoShow = true")
    List<Booking> findAllMarkedAsNoShow();
    
    // ADDED: New method for valid pending counter payments
    @Query("SELECT b FROM Booking b WHERE b.paymentMethod = 'PAY_AT_COUNTER' " +
           "AND b.paymentStatus = 'PENDING' " +
           "AND b.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "AND (b.markedAsNoShow = false OR b.markedAsNoShow IS NULL) " +
           "AND b.screening.screeningTime > :fifteenMinutesFromNow")
    List<Booking> findValidPendingCounterPayments(@Param("fifteenMinutesFromNow") LocalDateTime fifteenMinutesFromNow);
    
    // ADDED: Method to get bookings by payment method and status (for backward compatibility)
    @Query("SELECT b FROM Booking b WHERE b.paymentMethod = :paymentMethod AND b.paymentStatus = :status")
    List<Booking> findByPaymentMethodAndPaymentStatus(@Param("paymentMethod") String paymentMethod, @Param("status") String status);
}