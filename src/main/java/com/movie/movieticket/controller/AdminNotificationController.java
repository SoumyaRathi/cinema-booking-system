package com.movie.movieticket.controller;

import com.movie.movieticket.model.Notification;
import com.movie.movieticket.repository.NotificationRepository;
import com.movie.movieticket.service.AdminNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {
    
    private static final Logger logger = Logger.getLogger(AdminNotificationController.class.getName());
    
    @Autowired
    private AdminNotificationService adminNotificationService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @GetMapping
    public String viewNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String read,
            @RequestParam(required = false) String search,
            Model model) {
        try {
            logger.info("Loading notifications for admin with filters - type: " + type + ", read: " + read + ", search: " + search);
            
            List<Notification> notifications;
            Boolean readBoolean = null;
            
            // Convert read parameter to Boolean
            if (read != null && !read.isEmpty()) {
                readBoolean = Boolean.parseBoolean(read);
            }
            
            // Apply filters
            if ((type != null && !type.isEmpty()) || readBoolean != null || (search != null && !search.isEmpty())) {
                // If any filter is applied, use the filter query
                notifications = notificationRepository.findByFilters(
                    type != null && !type.isEmpty() ? type : null,
                    readBoolean,
                    search != null && !search.isEmpty() ? search : null
                );
                logger.info("Applied filters, found " + notifications.size() + " notifications");
            } else {
                // No filters, get all notifications
                notifications = adminNotificationService.getAllNotifications();
                logger.info("No filters applied, found " + notifications.size() + " notifications");
            }
            
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", adminNotificationService.getUnreadCount());
            model.addAttribute("recentNotifications", adminNotificationService.getRecentNotifications());
            
            // Add filter parameters to model for form persistence
            model.addAttribute("typeFilter", type);
            model.addAttribute("readFilter", read);
            model.addAttribute("searchFilter", search);
            
            return "notifications-admin";
        } catch (Exception e) {
            logger.severe("Error loading notifications: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading notifications: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/mark-read/{id}")
    @ResponseBody
    public Map<String, Object> markNotificationAsRead(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Marking notification as read: " + id);
            adminNotificationService.markAsRead(id);
            
            response.put("success", true);
            response.put("unreadCount", adminNotificationService.getUnreadCount());
            
            return response;
        } catch (Exception e) {
            logger.severe("Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return response;
        }
    }
    
    @PostMapping("/mark-all-read")
    @ResponseBody
    public Map<String, Object> markAllNotificationsAsRead() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Marking all notifications as read");
            adminNotificationService.markAllAsRead();
            
            response.put("success", true);
            response.put("unreadCount", 0);
            
            return response;
        } catch (Exception e) {
            logger.severe("Error marking all notifications as read: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return response;
        }
    }
}