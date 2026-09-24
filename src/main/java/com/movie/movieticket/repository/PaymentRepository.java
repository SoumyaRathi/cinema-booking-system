package com.movie.movieticket.repository;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Payment findByBookingId(Long bookingId);
    
    List<Payment> findByStatus(String status);
    
    List<Payment> findByPaymentMethod(String paymentMethod);
    
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN ?1 AND ?2")
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = ?1")
    List<Payment> findByUserId(Long userId);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.screening.movie.id = ?1")
    List<Payment> findByMovieId(Long movieId);
    
    List<Payment> findAllByOrderByPaymentDateDesc(Pageable pageable);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double getTotalRevenue();
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS' AND DATE(p.paymentDate) = CURRENT_DATE")
    Double getTodayRevenue();
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS'")
    long getSuccessfulPaymentsCount();
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'FAILED'")
    long getFailedPaymentsCount();
}