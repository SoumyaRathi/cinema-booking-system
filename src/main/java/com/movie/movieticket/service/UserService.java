package com.movie.movieticket.service;

import com.movie.movieticket.dto.UserRegistrationDto;
import com.movie.movieticket.model.PasswordResetToken;
import com.movie.movieticket.model.User;
import com.movie.movieticket.model.VerificationToken;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    User save(UserRegistrationDto registrationDto);
    
    User findByEmail(String email);
    
    User findByUsername(String username);
    
    void saveVerificationToken(User user, String token);
    
    String validateVerificationToken(String token);
    
    VerificationToken getVerificationToken(String token);
    
    User getUser(String verificationToken);
    
    String createPasswordResetTokenForUser(User user);
    
    String validatePasswordResetToken(String token);
    
    User getUserByPasswordResetToken(String token);
    
    void changeUserPassword(User user, String password);
    
    boolean checkIfValidOldPassword(User user, String oldPassword);
    
    List<User> getAllUsers();
    
    List<User> findAllUsers();
    
    User getUserById(Long id);
    
    void updateUser(User user);
    
    void deleteUser(Long id);
    
    boolean verifyUser(String token);
    
    List<User> findUsersByKeyword(String keyword);
    
    Long countAllUsers();
    
    User findById(Long id);
    
    User updateUser(Long id, User user);
    
    PasswordResetToken getPasswordResetToken(String token);
    
    void deletePasswordResetToken(PasswordResetToken token);
    
    boolean changePassword(User user, String currentPassword, String newPassword);
    
    // Methods for admin dashboard
    List<User> getRecentUsers(int limit);
    
    long getUserCount();
    
    List<User> getUsersByRole(String roleName);
    
    // Methods for no-show management
    long countBlockedUsers();
    
    long countUsersWithNoShows(Integer minNoShows, Boolean blocked);
    
    List<User> findTopUsersWithNoShows(int limit);
    
    List<User> findUsersWithNoShows(Integer minNoShows, Boolean blocked, Pageable pageable);
    
    List<User> findUsersWithNoShowsByKeyword(Integer minNoShows, Boolean blocked, String keyword, Pageable pageable);
    
    void blockUser(Long userId, String reason);
    
    void unblockUser(Long userId);
    
    void resetNoShowCount(Long userId);
    
    // New method for checking admin status
    boolean isAdmin(User user);
}