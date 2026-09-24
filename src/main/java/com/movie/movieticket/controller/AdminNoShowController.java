package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.BookingService;
import com.movie.movieticket.service.NoShowService;
import com.movie.movieticket.service.SeatService;
import com.movie.movieticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin")
public class AdminNoShowController {
    
    private static final Logger logger = Logger.getLogger(AdminNoShowController.class.getName());
    
    @Autowired
    private NoShowService noShowService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SeatService seatService;
    
    @GetMapping("/no-shows")
    public String listNoShows(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model) {
        try {
            logger.info("Loading no-show bookings for admin page");
            
            // Set default dates if not provided
            if (fromDate == null) {
                fromDate = LocalDate.now().minusMonths(1);
            }
            
            if (toDate == null) {
                toDate = LocalDate.now();
            }
            
            // Convert LocalDate to LocalDateTime for proper filtering
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);
            
            logger.info("Searching for no-shows between " + fromDateTime + " and " + toDateTime);
            
            // Get all bookings marked as no-show (regardless of date filter for debugging)
            List<Booking> allNoShows = bookingService.getAllMarkedAsNoShow();
            logger.info("Total bookings marked as no-show: " + allNoShows.size());
            
            // Get all bookings with status = NO_SHOW
            List<Booking> statusNoShows = bookingService.getBookingsByStatus("NO_SHOW");
            logger.info("Total bookings with status NO_SHOW: " + statusNoShows.size());
            
            // Combine both lists for display
            List<Booking> noShowBookings = allNoShows;
            
            model.addAttribute("noShowBookings", noShowBookings);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("selectedStatus", status);
            
            // Get pending bookings that might become no-shows
            List<Booking> pendingNoShows = noShowService.findPendingNoShows();
            model.addAttribute("pendingNoShows", pendingNoShows);
            
            return "no-shows";
        } catch (Exception e) {
            logger.severe("Error loading no-show bookings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading no-show bookings: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/no-shows/user/{email}")
    public String listUserNoShows(
            @PathVariable String email,
            Model model) {
        try {
            logger.info("Loading no-show bookings for user: " + email);
            
            List<Booking> userNoShows = bookingService.getNoShowBookingsByUserEmail(email);
            logger.info("Found " + userNoShows.size() + " no-show bookings for user: " + email);
            
            model.addAttribute("noShowBookings", userNoShows);
            model.addAttribute("userEmail", email);
            
            return "no-shows";
        } catch (Exception e) {
            logger.severe("Error loading user no-show bookings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading user no-show bookings: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/no-show-users")
    public String listUsersWithNoShows(
            @RequestParam(required = false, defaultValue = "1") Integer minNoShows,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        try {
            logger.info("Loading users with no-shows for admin page with filters - minNoShows: " + 
                       minNoShows + ", blocked: " + blocked + ", keyword: " + keyword);
            
            // Create a Pageable object with sorting by lastNoShow in descending order
            // This ensures the latest no-show users appear at the top
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastNoShow"));
            
            // Get users with no-shows based on filters
            List<User> usersWithNoShows = userService.findUsersWithNoShows(minNoShows, blocked, pageable);
            
            // If keyword is provided, filter the results manually
            // This is a workaround since the service method doesn't accept a keyword parameter
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchTerm = keyword.toLowerCase();
                usersWithNoShows = usersWithNoShows.stream()
                    .filter(user -> 
                        (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(searchTerm)) ||
                        (user.getLastName() != null && user.getLastName().toLowerCase().contains(searchTerm)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTerm)))
                    .toList();
            }
            
            logger.info("Found " + usersWithNoShows.size() + " users with no-shows matching filters");
            
            // Add filter parameters to model for form values
            model.addAttribute("minNoShows", minNoShows);
            model.addAttribute("blocked", blocked);
            model.addAttribute("keyword", keyword);
            model.addAttribute("usersWithNoShows", usersWithNoShows);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            return "no-show-users";
        } catch (Exception e) {
            logger.severe("Error loading users with no-shows: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading users with no-shows: " + e.getMessage());
            return "error";
        }
    }
    
    // API endpoint for fetching user booking details via AJAX
    @GetMapping("/api/no-show-users/{userId}/booking-details")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUserBookingDetails(@PathVariable Long userId) {
        try {
            logger.info("Fetching booking details for user ID: " + userId);
            
            List<Booking> userBookings = noShowService.getUserNoShowBookings(userId);
            
            List<Map<String, Object>> bookingDetails = userBookings.stream()
                .map(this::convertBookingToMap)
                .collect(Collectors.toList());
            
            logger.info("Found " + bookingDetails.size() + " no-show bookings for user ID: " + userId);
            
            return ResponseEntity.ok(bookingDetails);
        } catch (Exception e) {
            logger.severe("Error fetching booking details for user ID: " + userId + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private Map<String, Object> convertBookingToMap(Booking booking) {
        Map<String, Object> bookingMap = new HashMap<>();
        
        try {
            bookingMap.put("id", booking.getId());
            
            // Use actual confirmation code field from booking
            bookingMap.put("confirmationCode", booking.getConfirmationCode() != null ? 
                booking.getConfirmationCode() : "N/A");
            
            bookingMap.put("movieTitle", booking.getScreening() != null && booking.getScreening().getMovie() != null 
                ? booking.getScreening().getMovie().getTitle() : "N/A");
            
            if (booking.getScreening() != null && booking.getScreening().getScreeningTime() != null) {
                LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
                bookingMap.put("screeningDate", screeningTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                bookingMap.put("screeningTime", screeningTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            } else {
                bookingMap.put("screeningDate", "N/A");
                bookingMap.put("screeningTime", "N/A");
            }
            
            // Get seat count and numbers
            try {
                List<Seat> seats = seatService.getSeatsByBookingId(booking.getId());
                bookingMap.put("seatCount", seats.size());
                
                String seatNumbers = seats.stream()
                    .map(seat -> seat.getSeatNumber())
                    .collect(Collectors.joining(", "));
                bookingMap.put("seatNumbers", seatNumbers.isEmpty() ? "N/A" : seatNumbers);
            } catch (Exception e) {
                logger.warning("Error getting seats for booking ID: " + booking.getId() + " - " + e.getMessage());
                bookingMap.put("seatCount", 0);
                bookingMap.put("seatNumbers", "N/A");
            }
            
            bookingMap.put("totalAmount", booking.getTotalAmount() != null ? booking.getTotalAmount().toString() : "0.00");
            bookingMap.put("status", booking.getStatus());
            
            if (booking.getBookingTime() != null) {
                bookingMap.put("bookingDate", booking.getBookingTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else {
                bookingMap.put("bookingDate", "N/A");
            }
            
        } catch (Exception e) {
            logger.warning("Error converting booking to map for booking ID: " + booking.getId() + " - " + e.getMessage());
            // Set default values in case of error
            bookingMap.put("confirmationCode", "N/A");
            bookingMap.put("movieTitle", "Error loading data");
            bookingMap.put("screeningDate", "N/A");
            bookingMap.put("screeningTime", "N/A");
            bookingMap.put("seatCount", 0);
            bookingMap.put("seatNumbers", "N/A");
            bookingMap.put("totalAmount", "0.00");
        }
        
        return bookingMap;
    }
    
    @PostMapping("/no-shows/mark/{bookingId}")
    public String markAsNoShow(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try { 
            logger.info("Marking booking ID: " + bookingId + " as no-show");
            
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/admin/no-shows";
            }
            // Get current admin username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = authentication.getName();
            
            // Get seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(bookingId);
            logger.info("Found " + seats.size() + " seats to release for booking ID: " + bookingId);
            
            // Mark as no-show and process user's no-show count
            booking = noShowService.markAsNoShow(booking);
            booking.setProcessedBy(adminUsername);
            booking.setAdminNotes("Manually marked as no-show by admin");
            bookingService.saveBooking(booking);
            
            redirectAttributes.addFlashAttribute("success", "Booking successfully marked as no-show and seats released");
            return "redirect:/admin/no-shows";
        } catch (Exception e) {
            logger.severe("Error marking booking as no-show: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error marking booking as no-show: " + e.getMessage());
            return "redirect:/admin/no-shows";
        }
    }
    
    @PostMapping("/no-shows/process-pending")
    public String processPendingNoShows(RedirectAttributes redirectAttributes) {
        try {
            logger.info("Processing all pending no-shows");
            
            int processed = noShowService.processPendingNoShows();
            
            redirectAttributes.addFlashAttribute("success", "Successfully processed " + processed + " pending no-shows and released their seats");
            return "redirect:/admin/no-shows";
        } catch (Exception e) {
            logger.severe("Error processing pending no-shows: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error processing pending no-shows: " + e.getMessage());
            return "redirect:/admin/no-shows";
        }
    } 
    
    @PostMapping("/no-shows/reset-user/{userId}")
    public String resetUserNoShowStatus(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Resetting no-show status for user ID: " + userId);
            
            User user = noShowService.resetUserNoShowStatus(userId);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/no-show-users";
            }
            
            redirectAttributes.addFlashAttribute("success", "Successfully reset no-show status for user: " + user.getEmail());
            return "redirect:/admin/no-show-users";
        } catch (Exception e) {
            logger.severe("Error resetting user no-show status: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error resetting user no-show status: " + e.getMessage());
            return "redirect:/admin/no-show-users";
        }
    }
    
    @GetMapping("/no-show-users/view-profile/{id}")
    public String viewUserProfile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Viewing profile for user ID: " + id);
            
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/no-show-users";
            }
            
            redirectAttributes.addFlashAttribute("info", "Viewing user: " + user.getFirstName() + " " + user.getLastName());
            return "redirect:/admin/users";
        } catch (Exception e) {
            logger.severe("Error viewing user profile: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error viewing user profile: " + e.getMessage());
            return "redirect:/admin/no-show-users";
        }
    }
    
    @GetMapping("/users/unblock/{id}")
    public String unblockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Unblocking user ID: " + id);
            
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/no-show-users";
            }
            
            userService.unblockUser(id);
            
            redirectAttributes.addFlashAttribute("success", "Successfully unblocked user: " + user.getEmail());
            return "redirect:/admin/no-show-users";
        } catch (Exception e) {
            logger.severe("Error unblocking user: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error unblocking user: " + e.getMessage());
            return "redirect:/admin/no-show-users";
        }
    }
    
    @GetMapping("/users/block/{id}")
    public String blockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Blocking user ID: " + id);
            
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/no-show-users";
            }
            
            userService.blockUser(id, "Manually blocked by admin due to no-shows");
            
            redirectAttributes.addFlashAttribute("success", "Successfully blocked user: " + user.getEmail());
            return "redirect:/admin/no-show-users";
        } catch (Exception e) {
            logger.severe("Error blocking user: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error blocking user: " + e.getMessage());
            return "redirect:/admin/no-show-users";
        }
    }
    
    @GetMapping("/users/reset-no-shows/{id}")
    public String resetNoShowCount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Resetting no-show count for user ID: " + id);
            
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/no-show-users";
            }
            
            // Log user status before reset
            logger.info("Before reset - User ID: " + id + 
                       ", Email: " + user.getEmail() + 
                       ", NoShowCount: " + user.getNoShowCount() + 
                       ", IsBlocked: " + user.getIsBlocked() + 
                       ", BlockedUntil: " + user.getBlockedUntil());
            
            userService.resetNoShowCount(id);
            
            // Fetch updated user to verify changes
            User updatedUser = userService.getUserById(id);
            logger.info("After reset - User ID: " + id + 
                       ", Email: " + updatedUser.getEmail() + 
                       ", NoShowCount: " + updatedUser.getNoShowCount() + 
                       ", IsBlocked: " + updatedUser.getIsBlocked() + 
                       ", BlockedUntil: " + updatedUser.getBlockedUntil());
            
            redirectAttributes.addFlashAttribute("success", "Successfully reset no-show count for user: " + user.getEmail());
            return "redirect:/admin/no-show-users";
        } catch (Exception e) {
            logger.severe("Error resetting no-show count: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error resetting no-show count: " + e.getMessage());
            return "redirect:/admin/no-show-users";
        }
    }
    
    @GetMapping("/users/send-warning/{id}")
    public String sendWarningEmail(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logger.info("Sending warning email to user ID: " + id);
            
            User user = userService.getUserById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/no-show-users";
            }
            
            // Get the most recent no-show booking for this user
            List<Booking> userNoShows = bookingService.getNoShowBookingsByUserEmail(user.getEmail());
            if (userNoShows.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No no-show bookings found for this user");
                return "redirect:/admin/no-show-users";
            }
            
            // Sort by screening time descending and get the most recent one
            Booking mostRecentNoShow = userNoShows.stream()
                .sorted((b1, b2) -> b2.getScreening().getScreeningTime().compareTo(b1.getScreening().getScreeningTime()))
                .findFirst()
                .orElse(null);
            
            if (mostRecentNoShow != null) {
                noShowService.sendNoShowWarning(user, mostRecentNoShow);
                redirectAttributes.addFlashAttribute("success", "Successfully sent warning email to user: " + user.getEmail());
            } else {
                redirectAttributes.addFlashAttribute("error", "Could not find a valid no-show booking to reference in the warning email");
            }
            
            return "redirect:/admin/no-show-users";
        } catch (Exception e) {
            logger.severe("Error sending warning email: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error sending warning email: " + e.getMessage());
            return "redirect:/admin/no-show-users";
        }
    }
}