package com.movie.movieticket.controller;

import com.movie.movieticket.model.UserSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.logging.Logger;

@Controller
@RequestMapping("/session")
public class SessionController {

    private static final Logger logger = Logger.getLogger(SessionController.class.getName());
    private static final String USER_SESSION_KEY = "userSession";

    @GetMapping("/info")
    public String sessionInfo(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            
            model.addAttribute("sessionId", session.getId());
            model.addAttribute("creationTime", session.getCreationTime());
            model.addAttribute("lastAccessedTime", session.getLastAccessedTime());
            model.addAttribute("maxInactiveInterval", session.getMaxInactiveInterval());
            
            if (userSession != null) {
                model.addAttribute("userSession", userSession);
                userSession.updateLastAccess();
                logger.info("User session found for user: " + userSession.getEmail());
            } else {
                logger.info("No user session found in the current session");
            }
        } else {
            logger.info("No active session found");
        }
        
        return "session-info";
    }
    
    @GetMapping("/invalidate")
    public String invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            logger.info("Session invalidated");
        }
        return "redirect:/login";
    }
}