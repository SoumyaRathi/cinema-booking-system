package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.service.BookingService;
import com.movie.movieticket.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private static final Logger logger = Logger.getLogger(AdminPaymentController.class.getName());

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public String listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String customerEmail,
            Model model) {
        
        try {
            logger.info("Loading admin payments page");
            
            List<Payment> payments;
            
            // Apply filters if provided
            if (status != null && !status.isEmpty()) {
                payments = paymentService.getPaymentsByStatus(status);
                model.addAttribute("selectedStatus", status);
            } else if (method != null && !method.isEmpty()) {
                payments = paymentService.getPaymentsByMethod(method);
                model.addAttribute("selectedMethod", method);
            } else if (startDate != null && endDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                payments = paymentService.getPaymentsByDateRange(startDateTime, endDateTime);
                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);
            } else if (movieId != null) {
                payments = paymentService.getPaymentsByMovieId(movieId);
                model.addAttribute("selectedMovieId", movieId);
            } else {
                // No filters, get all payments
                payments = paymentService.getAllPayments();
            }
            
            // Get summary statistics
            Double totalRevenue = paymentService.getTotalRevenue();
            Double todayRevenue = paymentService.getTodayRevenue();
            long successfulPayments = paymentService.getSuccessfulPaymentsCount();
            long failedPayments = paymentService.getFailedPaymentsCount();
            
            model.addAttribute("payments", payments);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("successfulPayments", successfulPayments);
            model.addAttribute("failedPayments", failedPayments);
            
            return "admin-payments";
        } catch (Exception e) {
            logger.severe("Error loading admin payments: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading payments: " + e.getMessage());
            return "admin-payments";
        }
    }
    
    // FIXED: Add both endpoints for compatibility
    @GetMapping("/{id}")
    public String viewPaymentDetailsShort(@PathVariable Long id, Model model) {
        return viewPaymentDetails(id, model);
    }
    
    @GetMapping("/view/{id}")
    public String viewPaymentDetails(@PathVariable Long id, Model model) {
        try {
            logger.info("Loading payment details for ID: " + id);
            
            Payment payment = paymentService.getPaymentById(id);
            if (payment == null) {
                model.addAttribute("error", "Payment not found");
                return "redirect:/admin/payments";
            }
            
            model.addAttribute("payment", payment);
            model.addAttribute("booking", payment.getBooking());
            
            return "admin-payment-details";
        } catch (Exception e) {
            logger.severe("Error loading payment details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading payment details: " + e.getMessage());
            return "redirect:/admin/payments";
        }
    }
    
    @PostMapping("/view/{id}/update-status")
    public String updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Updating payment status for ID: " + id + " to " + status);
            
            Payment payment = paymentService.getPaymentById(id);
            if (payment == null) {
                redirectAttributes.addFlashAttribute("error", "Payment not found");
                return "redirect:/admin/payments";
            }
            
            // Update payment status
            payment.setStatus(status);
            if (notes != null && !notes.isEmpty()) {
                payment.setPaymentNotes(notes);
            }
            
            // Update booking payment status
            Booking booking = payment.getBooking();
            if ("SUCCESS".equals(status)) {
                booking.setPaymentStatus("COMPLETED");
                booking.setPaid(true);
                booking.setStatus("CONFIRMED");
                booking.setPaymentTime(LocalDateTime.now());
            } else if ("FAILED".equals(status)) {
                booking.setPaymentStatus("FAILED");
                booking.setPaid(false);
            } else if ("REFUNDED".equals(status)) {
                booking.setPaymentStatus("REFUNDED");
                booking.setPaid(false);
            } else {
                booking.setPaymentStatus(status);
            }
            
            bookingService.saveBooking(booking);
            paymentService.savePayment(payment);
            
            redirectAttributes.addFlashAttribute("success", "Payment status updated successfully");
            return "redirect:/admin/payments/view/" + id;
        } catch (Exception e) {
            logger.severe("Error updating payment status: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating payment status: " + e.getMessage());
            return "redirect:/admin/payments/view/" + id;
        }
    }
    
    @PostMapping("/view/{id}/refund")
    public String refundPayment(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Processing refund for payment ID: " + id);
            
            Payment refundedPayment = paymentService.refundPayment(id, reason);
            if (refundedPayment == null) {
                redirectAttributes.addFlashAttribute("error", "Payment not found");
                return "redirect:/admin/payments";
            }
            
            redirectAttributes.addFlashAttribute("success", "Payment refunded successfully");
            return "redirect:/admin/payments/view/" + id;
        } catch (Exception e) {
            logger.severe("Error processing refund: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error processing refund: " + e.getMessage());
            return "redirect:/admin/payments/view/" + id;
        }
    }
}