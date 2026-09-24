package com.movie.movieticket.repository;

import com.movie.movieticket.model.Role;
import com.movie.movieticket.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    
    User findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    User findUserByEmail(@Param("email") String email);
    
    // Add the missing findByKeyword method
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> findByKeyword(@Param("keyword") String keyword);
    
    // Add the missing findByRolesContaining method
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRolesContaining(@Param("role") Role role);
    
    // No-show tracking methods
    List<User> findByNoShowCountGreaterThan(int count);
    
    List<User> findByNoShowCountGreaterThan(int count, Pageable pageable);
    
    List<User> findByIsBlocked(boolean isBlocked);
    
    @Query("SELECT u FROM User u WHERE u.blockedUntil IS NOT NULL AND u.blockedUntil > CURRENT_TIMESTAMP")
    List<User> findTemporarilyBlockedUsers();
    
    long countByIsBlockedTrue();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.noShowCount >= :minNoShows")
    long countByNoShowCountGreaterThanEqual(@Param("minNoShows") int minNoShows);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.noShowCount >= :minNoShows AND u.isBlocked = :blocked")
    long countByNoShowCountGreaterThanEqualAndIsBlocked(@Param("minNoShows") int minNoShows, @Param("blocked") boolean blocked);
    
    List<User> findByNoShowCountGreaterThanEqualOrderByNoShowCountDesc(int minNoShows, Pageable pageable);
    
    List<User> findByNoShowCountGreaterThanEqualAndIsBlockedOrderByNoShowCountDesc(int minNoShows, boolean blocked, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.noShowCount >= :minNoShows AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> findByNoShowCountAndKeyword(@Param("minNoShows") int minNoShows, @Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.noShowCount >= :minNoShows AND u.isBlocked = :blocked AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> findByNoShowCountAndBlockedAndKeyword(@Param("minNoShows") int minNoShows, @Param("blocked") boolean blocked, 
                                                    @Param("keyword") String keyword, Pageable pageable);
}