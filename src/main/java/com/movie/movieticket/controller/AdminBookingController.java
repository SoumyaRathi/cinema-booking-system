package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Notification;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {
    
    private static final Logger logger = Logger.getLogger(AdminBookingController.class.getName());

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private ScreeningService screeningService;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NoShowService noShowService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AdminNotificationService adminNotificationService;

    // Helper method to check if booking is basically invalid
    private boolean isBookingBasicallyInvalid(Booking booking) {
        return "CANCELLED".equals(booking.getStatus()) || 
               "NO_SHOW".equals(booking.getStatus()) ||
               Boolean.TRUE.equals(booking.getMarkedAsNoShow());
    }

    // FIXED: Use relative paths without context path
    private String buildNotificationUrl(String path) {
        // Return relative path without context path - Spring Boot will handle routing
        return path;
    }
    
    @GetMapping
    public String listBookings(
            @RequestParam(required = false) String status,
            Model model) {
        try {
            logger.info("Loading bookings for admin page with status filter: " + status);
            
            List<Booking> bookings;
            
            // Apply status filter if provided
            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                if ("paid".equals(status)) {
                    bookings = bookingService.getBookingsByPaymentStatus("COMPLETED");
                } else if ("unpaid".equals(status)) {
                    bookings = bookingService.getBookingsByPaymentStatus("PENDING");
                } else {
                    // Filter by booking status (RESERVED, CONFIRMED, CANCELLED, NO_SHOW)
                    bookings = bookingService.getBookingsByStatus(status);
                }
                model.addAttribute("selectedStatus", status);
            } else {
                // Get all bookings
                bookings = bookingService.getAllBookings();
            }
            
            model.addAttribute("bookings", bookings);
            
            // Add counter for pending counter payments
            List<Booking> pendingCounterPayments = bookingService.getValidPendingCounterPayments();
            model.addAttribute("pendingCounterPayments", pendingCounterPayments.size());
            
            // Add notifications
            model.addAttribute("unreadNotificationsCount", adminNotificationService.getUnreadCount());
            model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
            
            // Add this line right before the return statement
            model.addAttribute("active", "bookings");
            
            return "bookings-admin";
        } catch (Exception e) {
            logger.severe("Error loading bookings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            model.addAttribute("exception", e);
            return "error";
        }
    }
    
    @GetMapping("/counter-payments")
    public String listCounterPayments(Model model) {
        try {
            logger.info("Loading pending counter payments for admin page");
            
            // Get valid pending counter payments (not expired, not no-show, not cancelled)
            List<Booking> pendingPayments = bookingService.getValidPendingCounterPayments();
            
            model.addAttribute("bookings", pendingPayments);
            model.addAttribute("counterPaymentsView", true);
            
            // Add notifications
            model.addAttribute("unreadNotificationsCount", adminNotificationService.getUnreadCount());
            model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
            
            return "counter-payments-admin";
        } catch (Exception e) {
            logger.severe("Error loading counter payments: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading counter payments: " + e.getMessage());
            model.addAttribute("exception", e);
            return "error";
        }
    }

    // FIXED: Add both endpoints for compatibility
    @GetMapping("/{id}")
    public String viewBookingShort(@PathVariable Long id, Model model) {
        return viewBooking(id, model);
    }

    @GetMapping("/view/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        try {
            logger.info("Viewing booking ID: " + id);
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + id);
                model.addAttribute("error", "Booking not found");
                return "error";
            }
            
            // Log detailed booking information for debugging
            logger.info("Retrieved booking details - ID: " + booking.getId() + 
                       ", Confirmation Code: " + booking.getConfirmationCode() + 
                       ", Customer: " + booking.getUser().getFirstName() + " " + booking.getUser().getLastName() +
                       ", Customer ID: " + booking.getUser().getId());
            
            List<Seat> seats = seatService.getSeatsByBookingId(booking.getId());
            logger.info("Retrieved " + seats.size() + " seats for booking ID: " + booking.getId());
             Payment payment = paymentService.getPaymentByBookingId(booking.getId());
            if (payment != null) {
                logger.info("Retrieved payment ID: " + payment.getId() + " for booking ID: " + booking.getId());
            } else {
                logger.info("No payment found for booking ID: " + booking.getId());
            }
            model.addAttribute("booking", booking);
            model.addAttribute("seats", seats);
            model.addAttribute("payment", payment);
            model.addAttribute("user", booking.getUser());
            
            // Add booking ID and confirmation code to the model for verification in the view
            model.addAttribute("bookingId", booking.getId());
            model.addAttribute("confirmationCode", booking.getConfirmationCode());
            
            // CRITICAL FIX: Check if booking can still be processed
            if ("PAY_AT_COUNTER".equals(booking.getPaymentMethod()) && !booking.isPaid()) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime fifteenMinBeforeScreening = booking.getScreening().getScreeningTime().minusMinutes(15);
                boolean canStillProcess = now.isBefore(fifteenMinBeforeScreening) && 
                                        !"CANCELLED".equals(booking.getStatus()) && 
                                        !"NO_SHOW".equals(booking.getStatus()) &&
                                        !Boolean.TRUE.equals(booking.getMarkedAsNoShow());
                
                model.addAttribute("canStillProcess", canStillProcess);
                model.addAttribute("fifteenMinCutoff", fifteenMinBeforeScreening);
                  // Add timing info for debugging
                logger.info("Booking ID: " + booking.getId() + 
                           " - Current time: " + now + 
                           " - 15-min cutoff: " + fifteenMinBeforeScreening + 
                           " - Can still process: " + canStillProcess +
                           " - Status: " + booking.getStatus() +
                           " - Marked as no-show: " + booking.getMarkedAsNoShow());
            }
            
            // Add notifications
            model.addAttribute("unreadNotificationsCount", adminNotificationService.getUnreadCount());
            model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
            
            return "view-booking-admin";
        } catch (Exception e) {
            logger.severe("Error loading booking details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading booking: " + e.getMessage());
            model.addAttribute("exception", e);
            return "error";
        }
    }

    @PostMapping("/update-status/{id}")
    public String updateBookingStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "adminNotes", required = false) String adminNotes,
            RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Updating status for booking ID: " + id + " to " + status);
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/bookings";
            }
            
            // CRITICAL FIX: Only check 15-minute cutoff for payment processing
            if ("PAY_AT_COUNTER".equals(booking.getPaymentMethod()) && "CONFIRMED".equals(status)) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime fifteenMinBeforeScreening = booking.getScreening().getScreeningTime().minusMinutes(15);
                
                if (now.isAfter(fifteenMinBeforeScreening)) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot process payment - booking is past the 15-minute cutoff before screening");
                    return "redirect:/admin/bookings";
                }
            }
            
            // Check basic validity (but not timing for non-payment operations)
            if (isBookingBasicallyInvalid(booking) && !"NO_SHOW".equals(status)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot update status for a booking that is cancelled or already marked as no-show");
                return "redirect:/admin/bookings";
            }
            
            // Get current admin username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            
            booking.setStatus(status);
            booking.setAdminNotes(adminNotes);
            booking.setProcessedBy(adminUsername);
            
            // If marking as paid
            if ("CONFIRMED".equals(status)) {
                booking.setPaid(true);
                booking.setPaymentStatus("COMPLETED");
                booking.setPaymentTime(LocalDateTime.now());
                  // Check if payment already exists before creating a new one
                Payment payment = paymentService.getPaymentByBookingId(id);
                if (payment == null) {
                    logger.info("Creating new payment record for booking ID: " + id);
                    payment = new Payment();
                    payment.setBooking(booking);
                    payment.setPaymentMethod(booking.getPaymentMethod());
                    payment.setAmount(booking.getTotalAmount());
                    payment.setPaymentDate(LocalDateTime.now());
                    payment.setStatus("SUCCESS");
                    payment.setTransactionId("COUNTER-" + System.currentTimeMillis());
                    payment.setProcessedBy(adminUsername);
                    payment.setPaymentNotes("Processed by admin at counter");
                    paymentService.savePayment(payment); 
                    // Create notification for payment
                    adminNotificationService.notifyPayment(payment);
                } else { 
                    logger.info("Payment record already exists for booking ID: " + id + ", updating existing payment");
                    payment.setStatus("SUCCESS");
                    payment.setPaymentDate(LocalDateTime.now());
                    payment.setProcessedBy(adminUsername);
                    payment.setPaymentNotes("Updated by admin at counter");
                    paymentService.savePayment(payment);
                }
            }
            
            // If marking as no-show
            if ("NO_SHOW".equals(status)) {
                noShowService.markAsNoShow(booking);
                adminNotificationService.notifyNoShow(booking);
            }
            
            // If cancelling, release seats
            if ("CANCELLED".equals(status)) {
                bookingService.releaseSeats(booking.getId());
                
                // FIXED: Create notification for cancellation with relative path
                String notificationMessage = "Booking " + booking.getConfirmationCode() + " for " + 
                                            booking.getScreening().getMovie().getTitle() + " has been cancelled";
                
                adminNotificationService.createNotification(
                    "Booking Cancelled: " + booking.getConfirmationCode(),
                    notificationMessage,
                    "WARNING",
                    "BOOKING",
                    booking.getId(),
                    buildNotificationUrl("/admin/bookings/view/" + booking.getId())
                );
            }
            
            bookingService.saveBooking(booking);
            logger.info("Successfully updated status for booking ID: " + id);
            
            redirectAttributes.addFlashAttribute("success", "Booking status updated successfully");
        } catch (Exception e) {
            logger.severe("Error updating booking status: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating booking status: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings";
    }
    
    @PostMapping("/process-counter-payment/{id}")
    public String processCounterPayment(
            @PathVariable Long id,
            @RequestParam(value = "paymentNotes", required = false) String paymentNotes,
            RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Processing counter payment for booking ID: " + id);
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/bookings/counter-payments";
            }
            
            // CRITICAL FIX: Check if booking is past 15-minute cutoff
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fifteenMinBeforeScreening = booking.getScreening().getScreeningTime().minusMinutes(15);
            
            if (now.isAfter(fifteenMinBeforeScreening)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot process payment - booking is past the 15-minute cutoff before screening");
                return "redirect:/admin/bookings/counter-payments";
            }
            // Check basic validity
            if (isBookingBasicallyInvalid(booking)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot process payment for a booking that is cancelled or marked as no-show");
                return "redirect:/admin/bookings/counter-payments";
            }
            
            // Get current admin username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            
            // Check if payment already exists
            Payment existingPayment = paymentService.getPaymentByBookingId(id);
            Payment payment;
            
            if (existingPayment != null) {
                logger.info("Payment already exists for booking ID: " + id + ", updating existing payment");
                // Update existing payment
                existingPayment.setStatus("SUCCESS");
                existingPayment.setPaymentDate(LocalDateTime.now());
                existingPayment.setProcessedBy(adminUsername);
                existingPayment.setPaymentNotes(paymentNotes != null ? paymentNotes : "Updated by admin at counter");
                payment = paymentService.savePayment(existingPayment);
            } else {
                logger.info("Creating new payment for booking ID: " + id);
                // Create new payment record
                payment = new Payment();
                payment.setBooking(booking);
                payment.setPaymentMethod("PAY_AT_COUNTER");
                payment.setAmount(booking.getTotalAmount());
                payment.setPaymentDate(LocalDateTime.now());
                payment.setStatus("SUCCESS");
                payment.setTransactionId("COUNTER-" + System.currentTimeMillis());
                payment.setProcessedBy(adminUsername);
                payment.setPaymentNotes(paymentNotes != null ? paymentNotes : "Processed by admin at counter");
                payment = paymentService.savePayment(payment);
            }
            
            // Update booking
            booking.setStatus("CONFIRMED");
            booking.setPaid(true);
            booking.setPaymentStatus("COMPLETED");
            booking.setPaymentTime(LocalDateTime.now());
            booking.setProcessedBy(adminUsername);
            booking.setAdminNotes(paymentNotes);
            
            bookingService.saveBooking(booking);
            
            // Create notification for payment
            adminNotificationService.notifyPayment(payment);
            
            logger.info("Successfully processed counter payment for booking ID: " + id);
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully");
        } catch (Exception e) {
            logger.severe("Error processing counter payment: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error processing payment: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings/counter-payments";
    }

    @GetMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Cancelling booking ID: " + id);
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/bookings";
            }
            
            // Get current admin username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            
            // Add admin note before releasing seats
            String cancelNote = "Cancelled by admin " + adminUsername + " on " + LocalDateTime.now();
            booking.setAdminNotes(booking.getAdminNotes() != null ? 
                booking.getAdminNotes() + " | " + cancelNote : cancelNote);
            bookingService.saveBooking(booking);
            
            // Release seats - this will also update the booking status to CANCELLED
            bookingService.releaseSeats(booking.getId());
            
            // Send cancellation email
            try {
                emailService.sendBookingCancellationEmail(booking);
                logger.info("Sent cancellation email for booking ID: " + id);
            } catch (Exception e) {
                logger.warning("Failed to send cancellation email: " + e.getMessage());
                // Continue with cancellation even if email fails
            } 
            
            // FIXED: Create notification for cancellation with relative path
            String notificationMessage = "Booking " + booking.getConfirmationCode() + " for " + 
                                        booking.getScreening().getMovie().getTitle() + " has been cancelled";
            
            adminNotificationService.createNotification(
                "Booking Cancelled: " + booking.getConfirmationCode(),
                notificationMessage,
                "WARNING",
                "BOOKING",
                booking.getId(),
                buildNotificationUrl("/admin/bookings/view/" + booking.getId())
            );
            logger.info("Successfully cancelled booking ID: " + id);
            
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully and seats released!");
        } catch (Exception e) {
            logger.severe("Error cancelling booking: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/delete/{id}")
    public String deleteBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Deleting booking ID: " + id);
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/bookings";
            }
            
            // Only allow deletion of cancelled bookings
            if (!"CANCELLED".equals(booking.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Only cancelled bookings can be deleted");
                return "redirect:/admin/bookings";
            }
            // Release seats if they're still associated with this booking
            bookingService.releaseSeats(booking.getId());
            
            // FIXED: Create notification for deletion with relative path
            String notificationMessage = "Booking " + booking.getConfirmationCode() + " for " + 
                                        booking.getScreening().getMovie().getTitle() + " has been deleted";
            
            adminNotificationService.createNotification(
                "Booking Deleted: " + booking.getConfirmationCode(),
                notificationMessage,
                "ERROR",
                "BOOKING",
                null,
                buildNotificationUrl("/admin/bookings")
            );
            
            bookingService.deleteBooking(id);
            logger.info("Successfully deleted booking ID: " + id);
            redirectAttributes.addFlashAttribute("success", "Booking deleted successfully");
        } catch (Exception e) {
            logger.severe("Error deleting booking: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error deleting booking: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }
    
    @GetMapping("/mark-no-show/{id}")
    public String markAsNoShow(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Marking booking ID: " + id + " as no-show");
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/bookings";
            }
            
            // Check if booking is already marked as no-show
            if (booking.getMarkedAsNoShow()) {
                redirectAttributes.addFlashAttribute("warning", "Booking is already marked as no-show");
                return "redirect:/admin/bookings";
            }
            
            // Get current admin username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            
            // Mark as no-show
            booking = noShowService.markAsNoShow(booking);
            booking.setProcessedBy(adminUsername);
            booking.setAdminNotes("Manually marked as no-show by admin");
            bookingService.saveBooking(booking);
            
            // Create notification for no-show
            adminNotificationService.notifyNoShow(booking);
            
            redirectAttributes.addFlashAttribute("success", "Booking successfully marked as no-show and seats released");
        } catch (Exception e) {
            logger.severe("Error marking booking as no-show: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error marking booking as no-show: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }
    
    // CRITICAL FIX: Updated process payment page
    @GetMapping("/process-payment/{id}")
    public String showProcessPaymentPage(@PathVariable("id") Long id, Model model) {
        try {
            logger.info("Loading process payment page for booking ID: " + id);
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + id);
                model.addAttribute("error", "Booking not found");
                return "error";
            }
            
            // CRITICAL FIX: Check if booking is past 15-minute cutoff
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fifteenMinBeforeScreening = booking.getScreening().getScreeningTime().minusMinutes(15);
            
            if (now.isAfter(fifteenMinBeforeScreening)) {
                model.addAttribute("error", 
                    "Cannot process payment - booking is past the 15-minute cutoff before screening");
                return "error";
            }
            
            // Check basic validity
            if (isBookingBasicallyInvalid(booking)) {
                model.addAttribute("error", 
                    "Cannot process payment for a booking that is cancelled or marked as no-show");
                return "error";
            }
            
            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(booking.getId());
            
            // Get payment information if exists
            Payment payment = paymentService.getPaymentByBookingId(booking.getId());
            
            model.addAttribute("booking", booking);
            model.addAttribute("seats", seats);
            model.addAttribute("payment", payment);
            model.addAttribute("canStillProcess", true);
            model.addAttribute("fifteenMinCutoff", fifteenMinBeforeScreening);
            
            // Add notifications
            model.addAttribute("unreadNotificationsCount", adminNotificationService.getUnreadCount());
            model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
            
            return "process-payment";
        } catch (Exception e) {
            logger.severe("Error loading process payment page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading payment details: " + e.getMessage());
            return "error";
        }
    }
    
    // Redirect to the new notification controller
    @GetMapping("/notifications")
    public String redirectToNotifications() {
        return "redirect:/admin/notifications";
    }
    
    @PostMapping("/cancel/{id}")
    public String adminCancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Admin cancelling booking ID: " + id);
            
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/bookings";
            }
            
            // Get current admin username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            
            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(id);
            logger.info("Found " + seats.size() + " seats to release for booking ID: " + id);
            
            // Update seats to be available again
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBookingId(null);
                seatService.saveSeat(seat);
                logger.info("Released seat ID: " + seat.getId() + " for booking ID: " + id);
            }
            // Update booking status
            booking.setStatus("CANCELLED");
            booking.setAdminNotes(booking.getAdminNotes() != null ? 
                booking.getAdminNotes() + " | Cancelled by admin " + adminUsername + " on " + LocalDateTime.now() : 
                "Cancelled by admin " + adminUsername + " on " + LocalDateTime.now());
            bookingService.saveBooking(booking);
            
            // Update available seats count in screening
            if (booking.getScreening() != null) {
                Screening screening = booking.getScreening();
                screening.setAvailableSeats(screening.getAvailableSeats() + seats.size());
                screeningService.saveScreening(screening);
                logger.info("Updated screening ID: " + screening.getId() + 
                           " available seats to " + screening.getAvailableSeats());
            }
            
            // Send cancellation email
            try {
                emailService.sendBookingCancellationEmail(booking);
                logger.info("Sent cancellation email for booking ID: " + id);
            } catch (Exception e) {
                logger.warning("Failed to send cancellation email: " + e.getMessage());
                // Continue with cancellation even if email fails
            }
            
            // FIXED: Create notification for cancellation with relative path
            String notificationMessage = "Booking " + booking.getConfirmationCode() + " for " + 
                                        booking.getScreening().getMovie().getTitle() + " has been cancelled by admin";
            
            adminNotificationService.createNotification(
                "Booking Cancelled: " + booking.getConfirmationCode(),
                notificationMessage,
                "WARNING",
                "BOOKING",
                booking.getId(),
                buildNotificationUrl("/admin/bookings/view/" + booking.getId())
            );
            
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully and seats released");
            return "redirect:/admin/bookings";
        } catch (Exception e) {
            logger.severe("Error cancelling booking: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
            return "redirect:/admin/bookings";
        }
    }
}