package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.Seat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

@Component
public class BookingPdfGenerator {
    
    private static final Logger logger = Logger.getLogger(BookingPdfGenerator.class.getName());
    
    @Autowired
    private SeatService seatService;
    
    public byte[] generateBookingReceipt(Booking booking, Payment payment) {
        try {
            logger.info("Generating PDF receipt for booking ID: " + booking.getId());
            
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph header = new Paragraph("Cinema Movie Ticket Booking", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            // Add booking confirmation
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph confirmationTitle = new Paragraph("Booking Confirmation", titleFont);
            confirmationTitle.setAlignment(Element.ALIGN_CENTER);
            confirmationTitle.setSpacingBefore(20);
            document.add(confirmationTitle);
            
            // Add confirmation code
            Font codeFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph confirmationCode = new Paragraph("Confirmation Code: " + booking.getConfirmationCode(), codeFont);
            confirmationCode.setAlignment(Element.ALIGN_CENTER);
            confirmationCode.setSpacingBefore(10);
            document.add(confirmationCode);
            
            // QR code has been removed as requested
            
            // Add booking details
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph detailsTitle = new Paragraph("Booking Details", sectionFont);
            detailsTitle.setSpacingBefore(20);
            document.add(detailsTitle);
            
            // Format dates
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            
            // Create details table
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingBefore(10);
            
            // Add movie details
            addTableRow(detailsTable, "Movie:", booking.getScreening().getMovie().getTitle());
            addTableRow(detailsTable, "Date:", booking.getScreening().getScreeningTime().toLocalDate().format(dateFormatter));
            addTableRow(detailsTable, "Time:", booking.getScreening().getScreeningTime().toLocalTime().format(timeFormatter));
            addTableRow(detailsTable, "Cinema:", booking.getScreening().getCinema().getName());
            
            // Get seats
            List<Seat> seats = seatService.getSeatsByBookingId(booking.getId());
            StringBuilder seatList = new StringBuilder();
            for (int i = 0; i < seats.size(); i++) {
                Seat seat = seats.get(i);
                seatList.append(seat.getRow()).append(seat.getSeatColumn());
                if (i < seats.size() - 1) {
                    seatList.append(", ");
                }
            }
            addTableRow(detailsTable, "Seats:", seatList.toString());
            
            document.add(detailsTable);
            
            // Add payment details
            Paragraph paymentTitle = new Paragraph("Payment Details", sectionFont);
            paymentTitle.setSpacingBefore(20);
            document.add(paymentTitle);
            
            // Create payment table
            PdfPTable paymentTable = new PdfPTable(2);
            paymentTable.setWidthPercentage(100);
            paymentTable.setSpacingBefore(10);
            
            addTableRow(paymentTable, "Ticket Price:", 
                    currencyFormatter.format(booking.getScreening().getPrice()) + " x " + booking.getNumberOfSeats());
            addTableRow(paymentTable, "Total Amount:", currencyFormatter.format(booking.getTotalAmount()));
            
            String paymentMethod = payment != null ? payment.getPaymentMethod() : booking.getPaymentMethod();
            addTableRow(paymentTable, "Payment Method:", formatPaymentMethod(paymentMethod));
            
            String paymentStatus = booking.getPaymentStatus();
            addTableRow(paymentTable, "Payment Status:", formatPaymentStatus(paymentStatus));
            
            document.add(paymentTable);
            
            // Add important notice for counter payment
            if ("PAY_AT_COUNTER".equals(paymentMethod)) {
                Font noticeFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.RED);
                Paragraph notice = new Paragraph("IMPORTANT: Please arrive at the cinema at least 30 minutes before showtime to complete your payment. " +
                        "Your reservation will expire if not paid 15 minutes before showtime.", noticeFont);
                notice.setSpacingBefore(20);
                document.add(notice);
            }
            
            // Add footer
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
            Paragraph footer = new Paragraph("This is an official receipt from Cinema Movie Ticket Booking System. " +
                    "For any inquiries, please contact our customer service.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            logger.severe("Error generating PDF receipt: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
    
    private void addTableRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private String formatPaymentMethod(String method) {
        if (method == null) {
            return "Not specified";
        }
        
        switch (method) {
            case "CREDIT_CARD":
                return "Credit Card";
            case "GCASH":
                return "GCash";
            case "PAY_AT_COUNTER":
                return "Pay at Counter";
            default:
                return method;
        }
    }
    
    private String formatPaymentStatus(String status) {
        if (status == null) {
            return "Unknown";
        }
        
        switch (status) {
            case "PENDING":
                return "Pending";
            case "COMPLETED":
                return "Completed";
            case "FAILED":
                return "Failed";
            case "REFUNDED":
                return "Refunded";
            default:
                return status;
        }
    }
}