package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    
    Payment savePayment(Payment payment);
    
    Payment getPaymentById(Long id);
    
    Payment getPaymentByBookingId(Long bookingId);
    
    List<Payment> getAllPayments();
    
    List<Payment> getPaymentsByStatus(String status);
    
    List<Payment> getPaymentsByMethod(String paymentMethod);
    
    List<Payment> getPaymentsByDateRange(LocalDateTime start, LocalDateTime end);
    
    List<Payment> getPaymentsByUserId(Long userId);
    
    List<Payment> getPaymentsByMovieId(Long movieId);
    
    List<Payment> getRecentPayments(int limit);
    
    Double getTotalRevenue();
    
    Double getTodayRevenue();
    
    long getSuccessfulPaymentsCount();
    
    long getFailedPaymentsCount();
    
    Payment processPayment(Booking booking, String paymentMethod, String paymentDetails);
    
    Payment refundPayment(Long paymentId, String reason);
    
    void deletePayment(Long id);
}