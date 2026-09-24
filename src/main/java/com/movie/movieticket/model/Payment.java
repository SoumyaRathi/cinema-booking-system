package com.movie.movieticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false)
    private String paymentMethod; // GCASH, CREDIT_CARD, PAY_AT_COUNTER

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING, REFUNDED

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "gcash_number")
    private String gcashNumber;

    @Column(name = "gcash_name")
    private String gcashName;

    @Column(name = "payment_notes")
    private String paymentNotes;

    @Column(name = "processed_by")
    private String processedBy;

    // Constructors
    public Payment() {
        this.paymentDate = LocalDateTime.now();
    }

    public Payment(Booking booking, String paymentMethod, Double amount, String status) {
        this.booking = booking;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = status;
        this.paymentDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getGcashNumber() {
        return gcashNumber;
    }

    public void setGcashNumber(String gcashNumber) {
        this.gcashNumber = gcashNumber;
    }

    public String getGcashName() {
        return gcashName;
    }

    public void setGcashName(String gcashName) {
        this.gcashName = gcashName;
    }

    public String getPaymentNotes() {
        return paymentNotes;
    }

    public void setPaymentNotes(String paymentNotes) {
        this.paymentNotes = paymentNotes;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", booking=" + (booking != null ? booking.getId() : "null") +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", paymentDate=" + paymentDate +
                '}';
    }
}