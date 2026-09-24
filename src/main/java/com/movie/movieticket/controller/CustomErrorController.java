package com.movie.movieticket.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Controller
public class CustomErrorController implements ErrorController {
    
    private static final Logger logger = Logger.getLogger(CustomErrorController.class.getName());

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        // Enhanced logging with request URI
        logger.severe("Error occurred on path: " + (requestUri != null ? requestUri.toString() : "Unknown path") + 
                     ", Error: " + (exception != null ? exception.toString() : "Unknown error"));
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            model.addAttribute("statusCode", statusCode);
            
            if (statusCode == 403) {
                model.addAttribute("error", "Access Denied");
                model.addAttribute("message", "You don't have permission to access this resource");
                return "error";
            } else if (statusCode == 404) {
                model.addAttribute("error", "Page not found");
                model.addAttribute("message", "The requested page does not exist");
                return "error";
            } else if (statusCode == 500) {
                model.addAttribute("error", "Internal Server Error");
                if (exception != null) {
                    model.addAttribute("exception", exception.toString());
                    // Log the stack trace for debugging
                    if (exception instanceof Throwable) {
                        ((Throwable) exception).printStackTrace();
                    }
                }
                if (message != null && !message.toString().isEmpty()) {
                    model.addAttribute("message", message.toString());
                } else {
                    model.addAttribute("message", "An unexpected server error occurred");
                }
                return "error";
            }
        }
        
        // For any other error
        model.addAttribute("error", "An unexpected error occurred");
        model.addAttribute("message", "Please try again later or contact support");
        
        // Add request path to model for debugging
        if (requestUri != null) {
            model.addAttribute("path", requestUri.toString());
        }
        
        return "error";
    }
    
    // This method is optional in newer Spring Boot versions but adding it for compatibility
    public String getErrorPath() {
        return "/error";
    }
}