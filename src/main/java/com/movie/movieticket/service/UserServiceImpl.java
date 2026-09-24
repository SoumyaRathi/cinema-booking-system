package com.movie.movieticket.service;

import com.movie.movieticket.dto.UserRegistrationDto;
import com.movie.movieticket.model.PasswordResetToken;
import com.movie.movieticket.model.Role;
import com.movie.movieticket.model.User;
import com.movie.movieticket.model.VerificationToken;
import com.movie.movieticket.repository.PasswordResetTokenRepository;
import com.movie.movieticket.repository.RoleRepository;
import com.movie.movieticket.repository.UserRepository;
import com.movie.movieticket.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    @Override
    public User save(UserRegistrationDto registrationDto) {
        Role role = roleRepository.findByName("ROLE_USER");
        if (role == null) {
            role = new Role("ROLE_USER");
            roleRepository.save(role);
        }

        User user = new User(
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getEmail(),
                passwordEncoder.encode(registrationDto.getPassword()),
                Arrays.asList(role)
        );

        user.setPrivacyPolicyAccepted(registrationDto.isPrivacyPolicyAccepted());
        user.setEnabled(false); // Ensure user is disabled until verified
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void resetNoShowCount(Long userId) {
        User user = getUserById(userId);
        if (user != null) {
            logger.info("Resetting no-show count for user ID: " + userId + 
                       ", Email: " + user.getEmail() + 
                       ", Current count: " + user.getNoShowCount() + 
                       ", Current isBlocked: " + user.getIsBlocked() + 
                       ", Current blockedUntil: " + user.getBlockedUntil());
            
            user.setNoShowCount(0);
            user.setLastNoShow(null);
            
            // Always clear both block types when resetting no-show count
            user.setIsBlocked(false);
            user.setBlockReason(null);
            user.setBlockedUntil(null);
            
            logger.info("After reset - isBlocked: " + user.getIsBlocked() + 
                       ", blockedUntil: " + user.getBlockedUntil());
            
            userRepository.save(user);
        } else {
            logger.warning("Cannot reset no-show count for non-existent user: " + userId);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User account is not verified. Please check your email.");
        }
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                mapRolesToAuthorities(user.getRoles())
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByEmail(username);
    }

    @Override
    public void saveVerificationToken(User user, String token) {
        VerificationToken verificationToken = new VerificationToken(token, user);
        tokenRepository.save(verificationToken);
    }

    @Override
    public String validateVerificationToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null) {
            return "invalidToken";
        }

        User user = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            tokenRepository.delete(verificationToken);
            return "expired";
        }

        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken); // Clean up the token after successful verification
        return "valid";
    }

    @Override
    public VerificationToken getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Override
    public User getUser(String verificationToken) {
        VerificationToken token = tokenRepository.findByToken(verificationToken);
        if (token != null) {
            return token.getUser();
        }
        return null;
    }

    @Override
    public String createPasswordResetTokenForUser(User user) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(myToken);
        return token;
    }

    @Override
    public String validatePasswordResetToken(String token) {
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken(token);
        if (passToken == null) {
            return "invalidToken";
        }

        Calendar cal = Calendar.getInstance();
        if ((passToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            passwordResetTokenRepository.delete(passToken);
            return "expired";
        }

        return "valid";
    }

    @Override
    public User getUserByPasswordResetToken(String token) {
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken(token);
        return passToken != null ? passToken.getUser() : null;
    }

    @Override
    public void changeUserPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public boolean checkIfValidOldPassword(User user, String oldPassword) {
        return passwordEncoder.matches(oldPassword, user.getPassword());
    } 
    
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void updateUser(User user) {
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public boolean verifyUser(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        
        if (verificationToken == null) {
            return false;
        }
        
        // Check if token is expired
        Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            tokenRepository.delete(verificationToken);
            return false;
        }
        
        // Enable the user
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        
        // Delete the token after successful verification
        tokenRepository.delete(verificationToken);
        
        return true;
    }

    @Override
    public List<User> findUsersByKeyword(String keyword) {
        return userRepository.findByKeyword(keyword);
    }

    @Override
    public Long countAllUsers() {
        return userRepository.count();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id).orElse(null);
        if (existingUser != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setEmail(updatedUser.getEmail());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            existingUser.setRoles(updatedUser.getRoles());
            existingUser.setEnabled(updatedUser.isEnabled());
            return userRepository.save(existingUser);
        }
        return null;
    }
    
    @Override
    public PasswordResetToken getPasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token);
    }
    
    @Override
    @Transactional
    public void deletePasswordResetToken(PasswordResetToken token) {
        passwordResetTokenRepository.delete(token);
    }
    
    @Override
    public boolean changePassword(User user, String currentPassword, String newPassword) {
        if (checkIfValidOldPassword(user, currentPassword)) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    // Implement new methods for admin dashboard
    @Override
    public List<User> getRecentUsers(int limit) {
        return userRepository.findAll(
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent(); // Convert Page to List
    }
    
    @Override
    public long getUserCount() {
        return userRepository.count();
    } 
    
    @Override
    public List<User> getUsersByRole(String roleName) {
        // For blocked users, we need to check the isBlocked flag instead of roles
        if ("BLOCKED".equals(roleName)) {
            return userRepository.findByIsBlocked(true);
        }
        
        Role role = roleRepository.findByName(roleName);
        if (role != null) {
            return userRepository.findByRolesContaining(role);
        }
        return List.of();
    }
    
    // New methods for no-show management
    @Override
    public long countBlockedUsers() {
        return userRepository.countByIsBlockedTrue();
    }
    
    @Override
    public long countUsersWithNoShows(Integer minNoShows, Boolean blocked) {
        if (minNoShows != null && blocked != null) {
            return userRepository.countByNoShowCountGreaterThanEqualAndIsBlocked(minNoShows, blocked);
        } else if (minNoShows != null) {
            return userRepository.countByNoShowCountGreaterThanEqual(minNoShows);
        } else if (blocked != null) {
            if (blocked) {
                return userRepository.countByIsBlockedTrue();
            } else {
                // For non-blocked users, we need to count all users minus blocked users
                long totalUsers = userRepository.count();
                long blockedUsers = userRepository.countByIsBlockedTrue();
                return totalUsers - blockedUsers;
            }
        } else {
            // Count users with any no-shows
            return userRepository.findByNoShowCountGreaterThan(0).size();
        }
    }
    
    @Override
    public List<User> findTopUsersWithNoShows(int limit) {
        return userRepository.findByNoShowCountGreaterThanEqualOrderByNoShowCountDesc(1, PageRequest.of(0, limit));
    }
    
    @Override
    public List<User> findUsersWithNoShows(Integer minNoShows, Boolean blocked, Pageable pageable) {
        if (minNoShows != null && blocked != null) {
            return userRepository.findByNoShowCountGreaterThanEqualAndIsBlockedOrderByNoShowCountDesc(minNoShows, blocked, pageable);
        } else if (minNoShows != null) {
            return userRepository.findByNoShowCountGreaterThanEqualOrderByNoShowCountDesc(minNoShows, pageable);
        } else if (blocked != null) {
            return userRepository.findByIsBlocked(blocked);
        } else {
            return userRepository.findByNoShowCountGreaterThan(0, pageable);
        }
    }
    
    @Override
    public List<User> findUsersWithNoShowsByKeyword(Integer minNoShows, Boolean blocked, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findUsersWithNoShows(minNoShows, blocked, pageable);
        }
        // Use the appropriate repository method based on available parameters
        if (minNoShows != null && blocked != null) {
            return userRepository.findByNoShowCountAndBlockedAndKeyword(minNoShows, blocked, keyword, pageable);
        } else if (minNoShows != null) {
            return userRepository.findByNoShowCountAndKeyword(minNoShows, keyword, pageable);
        } else {
            // If we don't have specific repository methods for these combinations,
            // we'll need to filter the results manually
            List<User> users = userRepository.findByKeyword(keyword);
            if (blocked != null) {
                users = users.stream()
                    .filter(user -> blocked.equals(user.getIsBlocked()))
                    .collect(Collectors.toList());
            }
            
            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), users.size());
            
            if (start <= end) {
                return users.subList(start, end);
            }
            return List.of();
        }
    }
    
    @Override
    @Transactional
    public void blockUser(Long userId, String reason) {
        User user = getUserById(userId);
        if (user != null) {
            user.setIsBlocked(true);
            user.setBlockReason(reason);
            userRepository.save(user);
            
            // Try to send notification email
            try {
                emailService.sendNoShowBlockedEmail(user);
            } catch (Exception e) {
                logger.warning("Failed to send block notification email: " + e.getMessage());
            }
        }
    }
    
    @Override
    @Transactional
    public void unblockUser(Long userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setIsBlocked(false);
            user.setBlockReason(null);
            user.setBlockedUntil(null);
            userRepository.save(user);
        }
    }
    
    
    @Override
    public boolean isAdmin(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
    }
}