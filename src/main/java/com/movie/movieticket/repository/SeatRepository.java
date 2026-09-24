package com.movie.movieticket.repository;

import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByScreening(Screening screening);
    
    List<Seat> findByScreeningId(Long screeningId);
    
    List<Seat> findByBookingId(Long bookingId);
    
    List<Seat> findByScreeningIdAndBookedFalse(Long screeningId);
    
    List<Seat> findByScreeningIdAndBookedTrue(Long screeningId);
    
    @Query("SELECT s FROM Seat s WHERE s.screening.id = :screeningId AND s.booked = false")
    List<Seat> findAvailableSeatsByScreeningId(@Param("screeningId") Long screeningId);
    
    @Query("SELECT s FROM Seat s WHERE s.screening.id = :screeningId AND s.booked = true")
    List<Seat> findBookedSeatsByScreeningId(@Param("screeningId") Long screeningId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.screening.id = :screeningId AND s.booked = false")
    long countAvailableSeatsByScreeningId(@Param("screeningId") Long screeningId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.screening.id = :screeningId AND s.booked = true")
    long countBookedSeatsByScreeningId(@Param("screeningId") Long screeningId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.screening.id = :screeningId")
    long countTotalSeatsByScreeningId(@Param("screeningId") Long screeningId);
    
    List<Seat> findByRowAndScreeningId(String row, Long screeningId);
    
    List<Seat> findByTypeAndScreeningId(String type, Long screeningId);
    
    boolean existsByScreeningId(Long screeningId);
}