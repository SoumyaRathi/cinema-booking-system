package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger logger = Logger.getLogger(BookingController.class.getName());

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NoShowService noShowService;
    
    @Autowired
    private AdminNotificationService adminNotificationService;

    // Root bookings URL
    @GetMapping({"", "/"})
    public String redirectToMyBookings() {
        logger.info("Redirecting from /bookings to /bookings/my-bookings");
        return "redirect:/bookings/my-bookings";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model) {
        try {
            logger.info("Loading user bookings");
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);

            if (currentUser == null) {
                logger.warning("User not found: " + currentUsername);
                return "redirect:/login";
            }
            
            // Check if user is blocked - with null safety
            Boolean isBlocked = currentUser.getIsBlocked();
            if ((isBlocked != null && isBlocked) || currentUser.isTemporarilyBlocked()) {
                model.addAttribute("warning", "Your account is currently blocked from making new bookings due to previous no-shows.");
            }

            // Get user's bookings
            List<Booking> bookings = bookingService.getBookingsByUser(currentUser);
            
            // Load seats for each booking
            for (Booking booking : bookings) {
                List<Seat> seats = seatService.getSeatsByBookingId(booking.getId());
                booking.setNumberOfSeats(seats.size());
            }
            
            model.addAttribute("bookings", bookings);
            return "my-bookings";
        } catch (Exception e) {
            logger.severe("Error loading user bookings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading your bookings: " + e.getMessage());
            return "my-bookings";
        }
    }

    @GetMapping("/screening/{id}")
    public String bookTickets(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            logger.info("=== BOOKING CONTROLLER CALLED ===");
            logger.info("Received request for screening ID: " + id);
            logger.info("Full URL path: /bookings/screening/" + id);
            
            // Validate the ID parameter
            if (id == null || id <= 0) {
                logger.warning("Invalid screening ID: " + id);
                redirectAttributes.addFlashAttribute("error", "Invalid screening ID");
                return "redirect:/movies";
            }
            
            Screening screening = screeningService.getScreeningById(id);
            if (screening == null) {
                logger.warning("Screening not found with ID: " + id);
                redirectAttributes.addFlashAttribute("error", "Screening not found");
                return "redirect:/movies";
            }
            
            logger.info("Screening found: " + screening.getMovie().getTitle() + " at " + screening.getScreeningTime());
            
            // Check if screening is available based on time
            if (!screening.isTimeAvailable()) {
                logger.warning("Screening with ID: " + id + " is no longer available for booking (time passed)");
                redirectAttributes.addFlashAttribute("error", "This screening is not available for booking.");
                return "redirect:/movies";
            }
            
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warning("User not authenticated");
                return "redirect:/login";
            }
            
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);
            
            if (currentUser == null) {
                logger.warning("User not found: " + currentUsername);
                return "redirect:/login";
            }
            
            logger.info("User found: " + currentUser.getEmail());
            
            // Check if user is blocked from booking - with null safety
            Boolean isBlocked = currentUser.getIsBlocked();
            LocalDateTime blockedUntil = currentUser.getBlockedUntil();
            
            // Log user status for debugging
            logger.info("User booking check - ID: " + currentUser.getId() + 
                       ", Email: " + currentUser.getEmail() + 
                       ", IsBlocked: " + isBlocked + 
                       ", BlockedUntil: " + blockedUntil);
            
            if (isBlocked != null && isBlocked) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your account is permanently blocked from making online bookings due to multiple no-shows. " +
                    "Please visit the cinema to purchase tickets in person.");
                return "redirect:/movies";
            }
            
            if (blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your account is temporarily blocked from making online bookings until " + 
                    blockedUntil.toLocalDate() + " due to previous no-shows.");
                return "redirect:/movies";
            }

            // Calculate max seats that can be booked
            int maxSeats = screening.getAvailableSeats();
            
            model.addAttribute("screening", screening);
            model.addAttribute("movie", screening.getMovie());
            model.addAttribute("maxSeats", maxSeats);
            
            logger.info("Successfully loaded booking page for screening ID: " + id);
            return "book-tickets";
        } catch (Exception e) {
            logger.severe("Error loading booking page: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading booking page: " + e.getMessage());
            return "redirect:/movies";
        }
    }

    @PostMapping("/screening/{id}")
    public String processBooking(@PathVariable Long id, 
                                @RequestParam("numberOfSeats") int numberOfSeats,
                                RedirectAttributes redirectAttributes) {
        try {
            logger.info("Processing booking for screening ID: " + id + " with " + numberOfSeats + " seats");
            
            if (id == null || id <= 0) {
                logger.warning("Invalid screening ID: " + id);
                redirectAttributes.addFlashAttribute("error", "Invalid screening ID");
                return "redirect:/movies";
            }
            Screening screening = screeningService.getScreeningById(id);
            if (screening == null) {
                logger.warning("Screening not found with ID: " + id);
                redirectAttributes.addFlashAttribute("error", "Screening not found");
                return "redirect:/movies";
            }

            // Check if screening is available based on time
            if (!screening.isTimeAvailable()) {
                logger.warning("Screening with ID: " + id + " is no longer available for booking (time passed)");
                redirectAttributes.addFlashAttribute("error", "This screening is not available for booking.");
                return "redirect:/movies";
            }

            // Check if enough seats are available
            if (screening.getAvailableSeats() < numberOfSeats) {
                logger.warning("Not enough seats available for screening ID: " + id);
                redirectAttributes.addFlashAttribute("error", "Not enough seats available");
                return "redirect:/bookings/screening/" + id;
            }

            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warning("User not authenticated");
                return "redirect:/login";
            }
            
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);

            if (currentUser == null) {
                logger.warning("User not found: " + currentUsername);
                return "redirect:/login";
            }  
            
            // Check if user is blocked from booking - with null safety
            Boolean isBlocked = currentUser.getIsBlocked();
            LocalDateTime blockedUntil = currentUser.getBlockedUntil();
            
            if (isBlocked != null && isBlocked) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your account is permanently blocked from making online bookings due to multiple no-shows.");
                return "redirect:/movies";
            }
            
            if (blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your account is temporarily blocked from making online bookings until " + 
                    blockedUntil.toLocalDate() + " due to previous no-shows.");
                return "redirect:/movies";
            }
            
            // Redirect to seat selection
            redirectAttributes.addFlashAttribute("numberOfSeats", numberOfSeats);
            return "redirect:/bookings/select-seats/" + id;
        } catch (Exception e) {
            logger.severe("Error processing booking: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error processing booking: " + e.getMessage());
            return "redirect:/movies";
        }
    }
    
    @GetMapping("/select-seats/{screeningId}")
    public String selectSeats(@PathVariable Long screeningId, 
                             Model model,
                             RedirectAttributes redirectAttributes,
                             @ModelAttribute("numberOfSeats") Integer numberOfSeats) {
        try {
            logger.info("Loading seat selection for screening ID: " + screeningId);
            
            if (screeningId == null || screeningId <= 0) {
                logger.warning("Invalid screening ID: " + screeningId);
                redirectAttributes.addFlashAttribute("error", "Invalid screening ID");
                return "redirect:/movies";
            }
            
            Screening screening = screeningService.getScreeningById(screeningId);
            if (screening == null) {
                logger.warning("Screening not found with ID: " + screeningId);
                redirectAttributes.addFlashAttribute("error", "Screening not found");
                return "redirect:/movies";
            }
            
            // Check if screening is available based on time
            if (!screening.isTimeAvailable()) {
                logger.warning("Screening with ID: " + screeningId + " is no longer available for booking (time passed)");
                redirectAttributes.addFlashAttribute("error", "This screening is not available for booking.");
                return "redirect:/movies";
            }

            // If numberOfSeats is not in the model, default to 1
            if (numberOfSeats == null) {
                numberOfSeats = 1;
                logger.info("No seat count provided, defaulting to 1");
            }
            
            // Get all seats for this screening
            List<Seat> seats = seatService.getSeatsByScreening(screening);
            
            // Calculate available seats count
            long availableSeatsCount = seats.stream()
                .filter(seat -> !seat.isBooked())
                .count();
            
            // Group seats by row for easier display
            Map<String, List<Seat>> seatRows = seats.stream()
                .sorted(Comparator.comparing(Seat::getRow)
                        .thenComparing(Seat::getSeatColumn))
                .collect(Collectors.groupingBy(Seat::getRow));

            model.addAttribute("screening", screening);
            model.addAttribute("seatRows", seatRows);
            model.addAttribute("numberOfSeats", numberOfSeats);
            model.addAttribute("availableSeatsCount", availableSeatsCount);
            model.addAttribute("totalSeats", seats.size());
            return "select-seats";
        } catch (Exception e) {
            logger.severe("Error loading seat selection: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading seat selection: " + e.getMessage());
            return "redirect:/movies";
        }
    }
    
    @PostMapping("/reserve-seats")
    public String reserveSeats(@RequestParam Long screeningId,
                              @RequestParam(required = false) List<Long> seatIds,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            logger.info("Processing seat reservation for screening ID: " + screeningId);
            
            // If seatIds is null or empty, the user might have clicked cancel
            if (seatIds == null || seatIds.isEmpty()) {
                logger.info("No seats selected or user cancelled - redirecting to movies page");
                return "redirect:/movies";
            }

            Screening screening = screeningService.getScreeningById(screeningId);
            if (screening == null) {
                logger.warning("Screening not found with ID: " + screeningId);
                redirectAttributes.addFlashAttribute("error", "Screening not found.");
                return "redirect:/movies";
            }
            
            // Check if screening is available based on time
            if (!screening.isTimeAvailable()) {
                logger.warning("Screening with ID: " + screeningId + " is no longer available for booking (time passed)");
                redirectAttributes.addFlashAttribute("error", "This screening is not available for booking.");
                return "redirect:/movies";
            }

            // Get the seats
            List<Seat> selectedSeats = seatService.getSeatsByIds(seatIds);
            
            // Check if any of the selected seats are already booked
            boolean anyBooked = selectedSeats.stream()
                    .anyMatch(Seat::isBooked);
            
            if (anyBooked) {
                logger.warning("Some selected seats are already booked");
                redirectAttributes.addFlashAttribute("error", "Some of the selected seats are already booked. Please try again.");
                return "redirect:/bookings/select-seats/" + screeningId;
            }
            
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);

            if (currentUser == null) {
                return "redirect:/login";
            }
            
            // Check if user is blocked from booking - with null safety
            Boolean isBlocked = currentUser.getIsBlocked();
            LocalDateTime blockedUntil = currentUser.getBlockedUntil();
            
            if (isBlocked != null && isBlocked) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your account is permanently blocked from making online bookings due to multiple no-shows.");
                return "redirect:/movies";
            }
            
            if (blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your account is temporarily blocked from making online bookings until " + 
                    blockedUntil.toLocalDate() + " due to previous no-shows.");
                return "redirect:/movies";
            }

            // Calculate total price
            Double totalPrice = selectedSeats.size() * screening.getPrice();

            // Create booking
            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setScreening(screening);
            booking.setBookingTime(LocalDateTime.now());
            booking.setBookingDate(LocalDate.now());
            booking.setTotalPrice(totalPrice);
            booking.setTotalAmount(totalPrice);
            booking.setStatus("RESERVED");
            booking.setPaymentStatus("PENDING");
            booking.setNumberOfSeats(selectedSeats.size());
            booking.setPaid(false);
            // Generate confirmation code
            String confirmationCode = generateConfirmationCode();
            booking.setConfirmationCode(confirmationCode);
            
            // Set expiration time to 30 minutes before screening
            booking.setExpirationTime(screening.getScreeningTime().minusMinutes(30));
            
            Booking savedBooking = bookingService.saveBooking(booking);

            // Update seats
            for (Seat seat : selectedSeats) {
                seat.setBooked(true);
                seat.setBookingId(savedBooking.getId());
                seatService.saveSeat(seat);
            }
            
            // Update available seats count in screening
            screening.setAvailableSeats(screening.getAvailableSeats() - selectedSeats.size());
            screeningService.saveScreening(screening);
            
            // Notify admin about new booking
            adminNotificationService.notifyNewBooking(savedBooking);

            // Instead of redirecting to payment, show the confirmation page first
            model.addAttribute("booking", savedBooking);
            model.addAttribute("screening", screening);
            model.addAttribute("seats", selectedSeats);
            model.addAttribute("totalPrice", totalPrice);
            
            return "confirm-booking";
        } catch (Exception e) {
            logger.severe("Error reserving seats: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error reserving seats: " + e.getMessage());
            return "redirect:/movies";
        }
    }
    
    @GetMapping("/cancel-reservation/{bookingId}")
    public String cancelReservation(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Cancelling reservation for booking ID: " + bookingId);
            
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + bookingId);
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/movies";
            }
            
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);
            
            // Check if booking belongs to current user
            if (currentUser == null || !booking.getUser().getId().equals(currentUser.getId())) {
                logger.warning("Unauthorized attempt to cancel reservation for booking ID: " + bookingId);
                redirectAttributes.addFlashAttribute("error", "You are not authorized to cancel this reservation.");
                return "redirect:/movies";
            }
            
            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(bookingId);
            
            // Release the seats
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBookingId(null);
                seatService.saveSeat(seat);
            } 
            // Update screening available seats
            Screening screening = booking.getScreening();
            screening.setAvailableSeats(screening.getAvailableSeats() + booking.getNumberOfSeats());
            screeningService.saveScreening(screening);
            
            // Delete the booking
            bookingService.deleteBooking(bookingId);
            
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled successfully");
            return "redirect:/movies";
        } catch (Exception e) {
            logger.severe("Error cancelling reservation: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error cancelling reservation: " + e.getMessage());
            return "redirect:/movies";
        }
    }
    
    // Helper method to generate a random confirmation code
    private String generateConfirmationCode() {
        // Generate a random 6-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    @GetMapping("/payment/{bookingId}")
    public String showPaymentForm(@PathVariable Long bookingId, Model model) {
        try {
            logger.info("Loading payment form for booking ID: " + bookingId);
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + bookingId);
                model.addAttribute("error", "Booking not found");
                return "error";
            }
            
            // Check if booking belongs to current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);

            // Check if booking belongs to current user
            if (currentUser == null || !booking.getUser().getId().equals(currentUser.getId())) {
                logger.warning("Unauthorized attempt to access payment for booking ID: " + bookingId);
                model.addAttribute("error", "You are not authorized to access this booking.");
                return "error";
            }
            
            // Check if booking is expired
            if (booking.getExpirationTime() != null && booking.getExpirationTime().isBefore(LocalDateTime.now())) {
                // Mark as no-show instead of just cancelling
                noShowService.markAsNoShow(booking);
                model.addAttribute("error", "This booking has expired and has been marked as a no-show.");
                return "error";
            }
            
            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(bookingId);
            
            model.addAttribute("booking", booking);
            model.addAttribute("seats", seats);
            model.addAttribute("screening", booking.getScreening());
            model.addAttribute("movie", booking.getScreening().getMovie());
            
            return "payment-form";
        } catch (Exception e) {
            logger.severe("Error loading payment form: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading payment form: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/process-payment")
    public String processPayment(
            @RequestParam Long bookingId,
            RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Processing payment for booking ID: " + bookingId + " with Pay at Counter method");
            
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + bookingId);
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/bookings/my-bookings";
            }  
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);
            
            if (currentUser == null || !booking.getUser().getId().equals(currentUser.getId())) {
                logger.warning("Unauthorized attempt to process payment for booking ID: " + bookingId);
                redirectAttributes.addFlashAttribute("error", "You are not authorized to process payment for this booking.");
                return "redirect:/bookings/my-bookings";
            }
            
            // Check if booking is expired
            if (booking.getExpirationTime() != null && booking.getExpirationTime().isBefore(LocalDateTime.now())) {
                // Mark as no-show instead of just cancelling
                noShowService.markAsNoShow(booking);
                redirectAttributes.addFlashAttribute("error", "This booking has expired and has been marked as a no-show.");
                return "redirect:/bookings/my-bookings";
            }
            
            // Set payment method to PAY_AT_COUNTER
            String paymentMethod = "PAY_AT_COUNTER";
            String paymentDetails = "Payment to be made at counter";
            
            // Process payment - keep status as PENDING
            Payment payment = paymentService.processPayment(booking, paymentMethod, paymentDetails);
            
            // Keep booking status as PENDING until admin confirms payment
            booking.setPaymentStatus("PENDING");
            booking.setStatus("RESERVED");
            booking.setPaid(false);
            bookingService.saveBooking(booking);
            
            // Send confirmation email with receipt
            emailService.sendBookingConfirmationWithReceipt(booking, payment);
            
            redirectAttributes.addFlashAttribute("success", "Booking confirmed! Please pay at the counter at least 30 minutes before showtime.");
            return "redirect:/bookings/confirmation/" + bookingId;
        } catch (Exception e) {
            logger.severe("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error processing payment: " + e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    @GetMapping("/confirmation/{bookingId}")
    public String bookingConfirmation(@PathVariable Long bookingId, Model model) {
        try {
            logger.info("Loading booking confirmation for booking ID: " + bookingId);
            // Fetch the latest booking data from the database
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + bookingId);
                model.addAttribute("error", "Booking not found");
                return "error";
            }
            
            // Check if booking belongs to current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);
            
            // Check if booking belongs to current user
            if (currentUser == null || !booking.getUser().getId().equals(currentUser.getId())) {
                logger.warning("Unauthorized attempt to access booking confirmation for booking ID: " + bookingId);
                model.addAttribute("error", "You are not authorized to access this booking.");
                return "error";
            }
            
            // FIXED: Only check for no-show if the booking is not paid and not already marked as no-show
            // This prevents paid bookings from being marked as no-shows when viewing details
            if (!booking.isPaid() && !Boolean.TRUE.equals(booking.getMarkedAsNoShow()) && 
                booking.getExpirationTime() != null && booking.getExpirationTime().isBefore(LocalDateTime.now())) {
                // Mark as no-show only for unpaid bookings that have expired
                noShowService.markAsNoShow(booking);
                model.addAttribute("error", "This booking has expired and has been marked as a no-show.");
                return "error";
            }
            
            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(bookingId);
            
            // Get payment information
            Payment payment = paymentService.getPaymentByBookingId(bookingId);
            
            // Log the payment status for debugging
            logger.info("Booking ID: " + bookingId + 
                       ", Payment Status: " + booking.getPaymentStatus() + 
                       ", Is Paid: " + booking.isPaid() + 
                       ", Status: " + booking.getStatus());
            
            if (payment != null) {
                logger.info("Payment ID: " + payment.getId() + 
                           ", Payment Status: " + payment.getStatus());
            }
            
            model.addAttribute("booking", booking);
            model.addAttribute("seats", seats);
            model.addAttribute("payment", payment);
            
            return "booking-confirmation";
        } catch (Exception e) {
            logger.severe("Error loading booking confirmation: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading booking confirmation: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Cancelling booking ID: " + bookingId);
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                logger.warning("Booking not found with ID: " + bookingId);
                redirectAttributes.addFlashAttribute("error", "Booking not found.");
                return "redirect:/bookings/my-bookings";
            }

            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByEmail(currentUsername);

            // Check if booking belongs to current user
            if (currentUser == null || !booking.getUser().getId().equals(currentUser.getId())) {
                logger.warning("Unauthorized attempt to cancel booking ID: " + bookingId);
                redirectAttributes.addFlashAttribute("error", "You are not authorized to cancel this booking.");
                return "redirect:/bookings/my-bookings";
            }
            
            // Check if booking is expired
            if (booking.getExpirationTime() != null && booking.getExpirationTime().isBefore(LocalDateTime.now())) {
                // Mark as no-show instead of just cancelling
                noShowService.markAsNoShow(booking);
                redirectAttributes.addFlashAttribute("error", "This booking has expired and has been marked as a no-show.");
                return "redirect:/bookings/my-bookings";
            }
            
            // Check if the booking can be canceled (not too close to screening time)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
            
            // Only allow cancellation if screening is at least 2 hours away
            if (screeningTime.minusHours(2).isBefore(now)) {
                logger.warning("Attempt to cancel booking too close to screening time: " + bookingId);
                redirectAttributes.addFlashAttribute("error", 
                    "Bookings can only be canceled at least 2 hours before the screening time.");
                return "redirect:/bookings/my-bookings";
            }

            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(bookingId);
            
            // Update seats to be available again
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBookingId(null);
                seatService.saveSeat(seat);
            }
            
            // Update available seats count in screening
            Screening screening = booking.getScreening();
            screening.setAvailableSeats(screening.getAvailableSeats() + seats.size());
            screeningService.saveScreening(screening);

            // Update booking status
            booking.setStatus("CANCELLED");
            booking.setAdminNotes(booking.getAdminNotes() != null ? 
                booking.getAdminNotes() + " | Cancelled by user on " + now : 
                "Cancelled by user on " + now);
            bookingService.saveBooking(booking);
            
            // Send email notification about cancellation
            try {
                emailService.sendBookingCancellationEmail(booking);
            } catch (Exception e) {
                logger.warning("Failed to send cancellation email: " + e.getMessage());
                // Continue with cancellation even if email fails
            }

            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully!");
            return "redirect:/bookings/my-bookings";
        } catch (Exception e) {
            logger.severe("Error cancelling booking: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }
}