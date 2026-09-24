package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = Logger.getLogger(PaymentServiceImpl.class.getName());

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private BookingService bookingService;

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }

    @Override
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    @Override
    public List<Payment> getPaymentsByMethod(String paymentMethod) {
        return paymentRepository.findByPaymentMethod(paymentMethod);
    }

    @Override
    public List<Payment> getPaymentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByPaymentDateBetween(start, end);
    }

    @Override
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    @Override
    public List<Payment> getPaymentsByMovieId(Long movieId) {
        return paymentRepository.findByMovieId(movieId);
    }

    @Override
    public List<Payment> getRecentPayments(int limit) {
        return paymentRepository.findAllByOrderByPaymentDateDesc(PageRequest.of(0, limit));
    }

    @Override
    public Double getTotalRevenue() {
        Double revenue = paymentRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public Double getTodayRevenue() {
        Double revenue = paymentRepository.getTodayRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public long getSuccessfulPaymentsCount() {
        return paymentRepository.getSuccessfulPaymentsCount();
    }

    @Override
    public long getFailedPaymentsCount() {
        return paymentRepository.getFailedPaymentsCount();
    }

    @Override
    @Transactional
    public Payment processPayment(Booking booking, String paymentMethod, String paymentDetails) {
        logger.info("Processing payment for booking ID: " + booking.getId() + " with method: " + paymentMethod);
        
        // Create new payment
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionId(generateTransactionId());
        
        // For pay at counter, set as PENDING
        payment.setPaymentNotes("Payment to be made at counter");
        payment.setStatus("PENDING");
        
        // Update booking - keep as PENDING
        booking.setPaymentMethod(paymentMethod);
        booking.setPaymentStatus("PENDING");
        booking.setStatus("RESERVED");
        booking.setPaid(false);
        bookingService.saveBooking(booking);
        
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment refundPayment(Long paymentId, String reason) {
        Payment payment = getPaymentById(paymentId);
        if (payment == null) {
            return null;
        }
        
        // Only refund completed payments
        if (!"COMPLETED".equals(payment.getStatus())) {
            return payment;
        }
        
        // Update payment
        payment.setStatus("REFUNDED");
        payment.setPaymentNotes("Refund reason: " + reason);
        
        // Update booking
        Booking booking = payment.getBooking();
        booking.setPaymentStatus("REFUNDED");
        booking.setPaid(false);
        bookingService.saveBooking(booking);
        
        return paymentRepository.save(payment);
    }

    @Override
    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }
    
    // Helper methods
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}