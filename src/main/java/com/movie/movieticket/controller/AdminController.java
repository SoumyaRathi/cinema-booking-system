package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    @Autowired
    private UserService userService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private ScreeningService screeningService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private AdminNotificationService adminNotificationService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            logger.info("Loading admin dashboard");
            
            // Add counts for dashboard statistics
            long userCount = userService.getAllUsers().size();
            long movieCount = movieService.getAllMovies().size();
            long bookingCount = bookingService.getAllBookings().size();
            long screeningCount = movieService.getAllUpcomingScreenings().size();
            
            // Get no-show statistics - use getAllMarkedAsNoShow for accurate count
            List<Booking> allNoShows = bookingService.getAllMarkedAsNoShow();
            long noShowCount = allNoShows.size();
            
            // FIXED: Use the same method as counter payments page for consistency
            List<Booking> pendingCounterPayments = bookingService.getValidPendingCounterPayments();
            
            // Use the same filtered list for both stats
            model.addAttribute("pendingPayments", pendingCounterPayments.size());
            model.addAttribute("pendingCounterPayments", pendingCounterPayments.size());
            
            // Get pending no-shows
            List<Booking> pendingNoShows = bookingService.getPotentialNoShows(java.time.LocalDateTime.now().minusMinutes(30));
            
            // Get users with no-shows
            List<User> usersWithNoShows = userService.findUsersWithNoShows(1, null, null);
            
            // Get blocked users - use the updated method that checks isBlocked flag
            List<User> blockedUsersList = userService.getUsersByRole("BLOCKED");
            long blockedUsers = blockedUsersList.size();
            
            // Log the blocked users for debugging
            logger.info("Found " + blockedUsers + " blocked users");
            for (User user : blockedUsersList) {
                logger.info("Blocked user: " + user.getEmail() + ", reason: " + user.getBlockReason());
            }
            
            // Calculate today's revenue
            double todayRevenue = paymentService.getTodayRevenue();
            
            // Add notifications
            model.addAttribute("unreadNotificationsCount", adminNotificationService.getUnreadCount());
            model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
            
            // Add all attributes to model
            model.addAttribute("userCount", userCount);
            model.addAttribute("movieCount", movieCount);
            model.addAttribute("bookingCount", bookingCount);
            model.addAttribute("noShowCount", noShowCount);
            model.addAttribute("screeningCount", screeningCount);
            model.addAttribute("blockedUsers", blockedUsers);
            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("pendingNoShows", pendingNoShows.size());
            
            // Add recent data for dashboard
            model.addAttribute("movies", movieService.getAllMovies());
            model.addAttribute("bookings", bookingService.getAllBookings());
            model.addAttribute("usersWithNoShows", usersWithNoShows);
            
            // Add active page for sidebar
            model.addAttribute("active", "dashboard");
            
            // Log for debugging
            logger.info("Dashboard stats - Pending Counter Payments: " + pendingCounterPayments.size());
            
            return "dashboard-admin";
        } catch (Exception e) {
            logger.severe("Error loading admin dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/profile")
    public String adminProfile(Model model) {
        // Add notifications to the profile page as well
        model.addAttribute("unreadNotificationsCount", adminNotificationService.getUnreadCount());
        model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
        
        // Add active page for sidebar
        model.addAttribute("active", "profile");
        
        return "admin-profile";
    }
    
    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        
        // Get the current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByEmail(auth.getName());
        
        // Check if current password is correct
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/admin/profile";
        }
        
        // Check if new password and confirm password match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New password and confirm password do not match");
            return "redirect:/admin/profile";
        }
        
        // Update the password
        userService.changeUserPassword(user, newPassword);
        
        redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        return "redirect:/admin/profile";
    }
}