package com.movie.movieticket.controller;

import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.repository.ScreeningRepository;
import com.movie.movieticket.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/seats")
public class AdminSeatController {

    @Autowired
    private ScreeningRepository screeningRepository;
    
    @Autowired
    private SeatRepository seatRepository;
    
    @GetMapping("/initialize/{screeningId}")
    @ResponseBody
    public String initializeSeats(@PathVariable Long screeningId) {
        try {
            Optional<Screening> screeningOpt = screeningRepository.findById(screeningId);
            if (!screeningOpt.isPresent()) {
                return "Screening not found with ID: " + screeningId;
            }
            
            Screening screening = screeningOpt.get();
            
            // Check if seats already exist
            List<Seat> existingSeats = seatRepository.findByScreeningId(screeningId);
            if (!existingSeats.isEmpty()) {
                return "Seats already exist for this screening. Count: " + existingSeats.size();
            }
            
            // Define rows and columns
            String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H"};
            int columnsPerRow = 10;
            
            List<Seat> seats = new ArrayList<>();
            int seatNumber = 1; // Sequential number for all seats
            
            for (String row : rows) {
                for (int col = 1; col <= columnsPerRow; col++) {
                    Seat seat = new Seat();
                    seat.setScreening(screening);
                    seat.setRow(row);
                    seat.setSeatColumn(col);
                    seat.setSeatNumber(row + col);
                    seat.setBooked(false);
                    seat.setNumber(seatNumber++); // Set the sequential number
                    
                    // Set price and type based on row position
                    int rowIndex = row.charAt(0) - 'A';
                    if (rowIndex < rows.length / 3) {
                        seat.setType("Silver");
                        seat.setPrice(150.00);
                    } else if (rowIndex < 2 * rows.length / 3) {
                        seat.setType("Gold");
                        seat.setPrice(200.00);
                    } else {
                        seat.setType("Platinum");
                        seat.setPrice(250.00);
                    }
                    
                    seats.add(seat);
                }
            }
            
            // Save all seats
            seatRepository.saveAll(seats);
            
            // Update screening with total seats
            screening.setTotalSeats(seats.size());
            screening.setAvailableSeats(seats.size());
            screeningRepository.save(screening);
            
            return "Successfully initialized " + seats.size() + " seats for screening ID: " + screeningId;
        } catch (Exception e) {
            return "Error initializing seats: " + e.getMessage();
        }
    }
    
    @GetMapping("/count/{screeningId}")
    @ResponseBody
    public String countSeats(@PathVariable Long screeningId) {
        try {
            List<Seat> seats = seatRepository.findByScreeningId(screeningId);
            return "Screening ID " + screeningId + " has " + seats.size() + " seats";
        } catch (Exception e) {
            return "Error counting seats: " + e.getMessage();
        }
    }
    
    @GetMapping("/initialize-all")
    @ResponseBody
    public String initializeAllScreenings() {
        try {
            List<Screening> screenings = screeningRepository.findAll();
            int initializedCount = 0;
            
            for (Screening screening : screenings) {
                // Check if seats already exist
                List<Seat> existingSeats = seatRepository.findByScreeningId(screening.getId());
                if (existingSeats.isEmpty()) {
                    // Define rows and columns
                    String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H"};
                    int columnsPerRow = 10;
                    
                    List<Seat> seats = new ArrayList<>();
                    int seatNumber = 1; // Sequential number for all seats
                    
                    for (String row : rows) {
                        for (int col = 1; col <= columnsPerRow; col++) {
                            Seat seat = new Seat();
                            seat.setScreening(screening);
                            seat.setRow(row);
                            seat.setSeatColumn(col);
                            seat.setSeatNumber(row + col);
                            seat.setBooked(false);
                            seat.setNumber(seatNumber++); // Set the sequential number
                            
                            // Set price and type based on row position
                            int rowIndex = row.charAt(0) - 'A';
                            if (rowIndex < rows.length / 3) {
                                seat.setType("Silver");
                                seat.setPrice(150.00);
                            } else if (rowIndex < 2 * rows.length / 3) {
                                seat.setType("Gold");
                                seat.setPrice(200.00);
                            } else {
                                seat.setType("Platinum");
                                seat.setPrice(250.00);
                            }
                            
                            seats.add(seat);
                        }
                    }
                    
                    // Save all seats
                    seatRepository.saveAll(seats);
                    
                    // Update screening with total seats
                    screening.setTotalSeats(seats.size());
                    screening.setAvailableSeats(seats.size());
                    screeningRepository.save(screening);
                    
                    initializedCount++;
                }
            }
            
            return "Successfully initialized seats for " + initializedCount + " screenings";
        } catch (Exception e) {
            return "Error initializing seats: " + e.getMessage();
        }
    }
}