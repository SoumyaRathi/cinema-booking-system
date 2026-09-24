package com.movie.movieticket.repository;

import com.movie.movieticket.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();
    
    List<Notification> findByIsReadOrderByCreatedAtDesc(boolean isRead);
    
    List<Notification> findTop10ByOrderByCreatedAtDesc();
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false")
    long countUnreadNotifications();
    
    // Add filter methods
    List<Notification> findByTypeOrderByCreatedAtDesc(String type);
    
    List<Notification> findByIsReadAndTypeOrderByCreatedAtDesc(boolean isRead, String type);
    
    @Query("SELECT n FROM Notification n WHERE " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:read IS NULL OR n.isRead = :read) AND " +
           "(:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByFilters(
        @Param("type") String type, 
        @Param("read") Boolean read, 
        @Param("search") String search
    );
}