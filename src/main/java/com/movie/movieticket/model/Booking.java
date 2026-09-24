package com.movie.movieticket.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Column(nullable = false)
    private LocalDateTime bookingTime;
    
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = true)
    private LocalDateTime paymentTime;

    @Column(nullable = false)
    private Double totalPrice;
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount = 0.0;

    @Column(nullable = false)
    private String status; // RESERVED, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW

    @Column(nullable = false)
    private boolean paid = false;
    
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING"; // PENDING, COMPLETED, FAILED, REFUNDED
    
    @Column(name = "payment_method")
    private String paymentMethod; // PAY_AT_COUNTER
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
    @Column(name = "number_of_seats")
    private Integer numberOfSeats = 0;
    
    @Column(name = "confirmation_code")
    private String confirmationCode;
    
    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;
    
    @Column(name = "marked_as_no_show")
    private Boolean markedAsNoShow = false;
    
    @Column(name = "no_show_processed")
    private Boolean noShowProcessed = false;
    
    @Column(name = "admin_notes")
    private String adminNotes;
    
    @Column(name = "processed_by")
    private String processedBy;
    
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;
    
    @Transient
    private List<Seat> seats = new ArrayList<>();

    // Constructors
    public Booking() {
        this.bookingTime = LocalDateTime.now();
        this.bookingDate = LocalDate.now(); // Initialize booking date
        this.status = "RESERVED";
        this.paymentStatus = "PENDING"; // Initialize payment status
        this.totalAmount = 0.0; // Initialize total amount
        this.confirmationCode = generateConfirmationCode();
        this.reminderSent = false;
    }

    public Booking(User user, Screening screening, Double totalPrice) {
        this.user = user;
        this.screening = screening;
        this.totalPrice = totalPrice;
        this.totalAmount = totalPrice; // Set total amount same as total price
        this.bookingTime = LocalDateTime.now();
        this.bookingDate = LocalDate.now(); // Initialize booking date
        this.status = "RESERVED";
        this.paymentStatus = "PENDING"; // Initialize payment status
        this.paid = false;
        this.confirmationCode = generateConfirmationCode();
        this.reminderSent = false;
        
        // Set expiration time to 30 minutes before screening
        if (screening != null && screening.getScreeningTime() != null) {
            this.expirationTime = screening.getScreeningTime().minusMinutes(30);
        }
    }

    // Existing getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Screening getScreening() {
        return screening;
    }

    public void setScreening(Screening screening) {
        this.screening = screening;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }
    
    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
        this.totalAmount = totalPrice; // Update total amount when total price is set
    }
    
    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
    
    public Integer getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(Integer numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }
    
    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
    
    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
        this.numberOfSeats = seats.size();
    }
    
    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }
    
    public Boolean getMarkedAsNoShow() {
        return markedAsNoShow;
    }
    
    public void setMarkedAsNoShow(Boolean markedAsNoShow) {
        this.markedAsNoShow = markedAsNoShow;
    }
    
    public Boolean getNoShowProcessed() {
        return noShowProcessed;
    }
    
    public void setNoShowProcessed(Boolean noShowProcessed) {
        this.noShowProcessed = noShowProcessed;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getProcessedBy() {
        return processedBy;
    }
    
    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
    
    public Boolean getReminderSent() {
        return reminderSent;
    }
    
    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
    
    // Helper method to calculate number of seats from a list of seats
    public void calculateNumberOfSeats() {
        if (this.seats != null) {
            this.numberOfSeats = this.seats.size();
        }
    }
    
    // Helper method to generate a confirmation code
    private String generateConfirmationCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    // Helper methods for booking status
    public boolean isExpired() {
        return this.expirationTime != null && LocalDateTime.now().isAfter(this.expirationTime);
    }
    
    public boolean isPendingPayment() {
        return "PENDING".equals(this.paymentStatus) && !isExpired();
    }
    
    public boolean isNoShow() {
        return this.markedAsNoShow || 
               ("PENDING".equals(this.paymentStatus) && 
                this.screening != null && 
                this.screening.getScreeningTime() != null && 
                LocalDateTime.now().isAfter(this.screening.getScreeningTime()));
    }
    
    // Helper method to check if booking is within 15 minutes of screening
    public boolean isWithinFifteenMinutesOfScreening() {
        if (this.screening == null || this.screening.getScreeningTime() == null) {
            return false;
        }
        LocalDateTime fifteenMinBeforeScreening = this.screening.getScreeningTime().minusMinutes(15);
        return LocalDateTime.now().isAfter(fifteenMinBeforeScreening);
    }
    
    // Helper method to check if booking is within 30 minutes of screening
    public boolean isWithinThirtyMinutesOfScreening() {
        if (this.screening == null || this.screening.getScreeningTime() == null) {
            return false;
        }
        LocalDateTime thirtyMinBeforeScreening = this.screening.getScreeningTime().minusMinutes(30);
        return LocalDateTime.now().isAfter(thirtyMinBeforeScreening);
    }
    
    // Helper method to get time remaining until 15-minute cutoff
    public LocalDateTime getFifteenMinuteCutoff() {
        if (this.screening == null || this.screening.getScreeningTime() == null) {
            return null;
        }
        return this.screening.getScreeningTime().minusMinutes(15);
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", user=" + (user != null ? user.getEmail() : "null") +
                ", screening=" + (screening != null ? screening.getId() : "null") +
                ", bookingTime=" + bookingTime +
                ", bookingDate=" + bookingDate +
                ", paymentTime=" + paymentTime +
                ", totalPrice=" + totalPrice +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", paid=" + paid +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentReference='" + paymentReference + '\'' +
                ", numberOfSeats=" + numberOfSeats +
                ", confirmationCode='" + confirmationCode + '\'' +
                ", expirationTime=" + expirationTime +
                ", markedAsNoShow=" + markedAsNoShow +
                ", reminderSent=" + reminderSent +
                '}';
    }
}