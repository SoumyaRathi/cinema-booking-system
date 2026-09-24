package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Logger;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = Logger.getLogger(EmailServiceImpl.class.getName());
    
    private final JavaMailSender mailSender;
    
    @Autowired
    private BookingPdfGenerator pdfGenerator;
    
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("cinemastarview@gmail.com");
            
            mailSender.send(message);
            logger.info("Email sent successfully to: " + to);
        } catch (Exception e) {
            logger.severe("Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendVerificationEmail(String to, String token, String applicationUrl) {
        try {
            String subject = "Complete Registration - Cinema Movie Ticket Booking";
            String verificationUrl = applicationUrl + "/verify-account?token=" + token;
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Create HTML content with hidden token
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #003366; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .verify-button { display: inline-block; background-color: #003366; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }" +
                "        .verify-button:hover { background-color: #004080; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "        .important-notice { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Cinema Movie Ticket Booking</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear User,</p>" +
                "            <p>Thank you for registering with our Cinema Movie Ticket Booking System.</p>" +
                "            <p>To complete your registration and verify your account, please click the button below:</p>" +
                "            <div style='text-align: center;'>" +
                "                <a href='" + verificationUrl + "' class='verify-button'>Verify Account</a>" +
                "            </div>" +
                "            <div class='important-notice'>" +
                "                <p><strong>Important:</strong></p>" +
                "                <ul>" +
                "                    <li>This verification link will expire in 24 hours</li>" +
                "                    <li>If you did not register for an account, please ignore this email</li>" +
                "                    <li>For security reasons, do not share this email with others</li>" +
                "                </ul>" +
                "            </div>" +
                "            <p>If the button above doesn't work, you can copy and paste this link into your browser:</p>" +
                "            <p style='word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;'>" + verificationUrl + "</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Verification email sent successfully to: " + to);
        } catch (MessagingException e) {
            logger.severe("Failed to send verification email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            // Fallback to plain text email if HTML email fails
            String plainText = "Dear User,\n\n"
                    + "Thank you for registering with our Cinema Movie Ticket Booking System.\n\n"
                    + "To complete your registration and verify your account, please click on the link below:\n"
                    + applicationUrl + "/verify-account?token=" + token + "\n\n"
                    + "This link will expire in 24 hours.\n\n"
                    + "If you did not register for an account, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "Cinema Movie Ticket Booking System Team";
            
            sendEmail(to, "Complete Registration", plainText);
        }
    }
    
    @Override
    public void sendVerificationEmail(User user, String applicationUrl, String token) {
        sendVerificationEmail(user.getEmail(), token, applicationUrl);
    }

    @Override
    public void sendBookingConfirmation(String to, String bookingDetails, String confirmationCode) {
        String subject = "Booking Confirmation - Cinema Movie Ticket";
        String body = "Dear Customer,\n\n"
                + "Thank you for your booking with our Cinema Movie Ticket Booking System.\n\n"
                + "Your booking has been confirmed with the following details:\n\n"
                + bookingDetails + "\n\n"
                + "Your confirmation code is: " + confirmationCode + "\n\n"
                + "Please present this code at the cinema counter to collect your tickets.\n\n"
                + "We hope you enjoy the movie!\n\n"
                + "Best regards,\n"
                + "Cinema Movie Ticket Booking System Team";
        
        sendEmail(to, subject, body);
    }
    
    @Override
    public void sendBookingConfirmationWithReceipt(Booking booking, Payment payment) {
        try {
            User user = booking.getUser();
            String to = user.getEmail();
            String subject = "Booking Confirmation - Cinema Movie Ticket";
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Format dates and currency
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            
            String movieTitle = booking.getScreening().getMovie().getTitle();
            
            // Extract date and time from screeningTime
            LocalDate screeningDate = booking.getScreening().getScreeningTime().toLocalDate();
            LocalTime screeningTime = booking.getScreening().getScreeningTime().toLocalTime();
            
            String formattedScreeningDate = screeningDate.format(dateFormatter);
            String formattedScreeningTime = screeningTime.format(timeFormatter);
            
            String cinemaName = booking.getScreening().getCinema().getName();
            String totalAmount = currencyFormatter.format(booking.getTotalAmount());
            String paymentMethod = payment != null ? payment.getPaymentMethod() : booking.getPaymentMethod();
            String paymentStatus = booking.getPaymentStatus();
            
            // Create HTML content
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #003366; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .booking-details { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin-bottom: 20px; }" +
                "        .receipt { border: 1px solid #ddd; padding: 15px; border-radius: 5px; }" +
                "        .receipt-header { border-bottom: 1px solid #ddd; padding-bottom: 10px; margin-bottom: 10px; }" +
                "        .receipt-row { display: flex; justify-content: space-between; margin-bottom: 5px; }" +
                "        .total { font-weight: bold; border-top: 1px solid #ddd; padding-top: 10px; margin-top: 10px; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "        .confirmation-code { background-color: #003366; color: white; padding: 10px; text-align: center; font-size: 18px; margin: 20px 0; }" +
                "        .important-notice { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "        .warning { color: #dc3545; font-weight: bold; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Cinema Movie Ticket Booking</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear " + user.getFirstName() + ",</p>" +
                "            <p>Thank you for your booking with our Cinema Movie Ticket Booking System. Your booking has been confirmed!</p>" +
                "            <div class='confirmation-code'>" +
                "                Confirmation Code: <strong>" + booking.getConfirmationCode() + "</strong>" +
                "            </div>" +
                "            <div class='booking-details'>" +
                "                <h2>Booking Details</h2>" +
                "                <p><strong>Movie:</strong> " + movieTitle + "</p>" +
                "                <p><strong>Date:</strong> " + formattedScreeningDate + "</p>" +
                "                <p><strong>Time:</strong> " + formattedScreeningTime + "</p>" +
                "                <p><strong>Cinema:</strong> " + cinemaName + "</p>" +
                "                <p><strong>Number of Seats:</strong> " + booking.getNumberOfSeats() + "</p>" +
                "                <p><strong>Booking Status:</strong> " + booking.getStatus() + "</p>" +
                "            </div>" +
                "            <div class='receipt'>" +
                "                <div class='receipt-header'>" +
                "                    <h2>Receipt</h2>" +
                "                </div>" +
                "                <div class='receipt-row'>" +
                "                    <span>Ticket Price:</span>" +
                "                    <span>" + currencyFormatter.format(booking.getScreening().getPrice()) + " x " + booking.getNumberOfSeats() + "</span>" +
                "                </div>" +
                "                <div class='receipt-row total'>" +
                "                    <span>Total Amount:</span>" +
                "                    <span>" + totalAmount + "</span>" +
                "                </div>" +
                "                <div class='receipt-row'>" +
                "                    <span>Payment Method:</span>" +
                "                    <span>Pay at Counter</span>" +
                "                </div>" +
                "                <div class='receipt-row'>" +
                "                    <span>Payment Status:</span>" +
                "                    <span>" + paymentStatus + "</span>" +
                "                </div>" +
                "            </div>" +
                "            <div class='important-notice'>" +
                "                <h3>Important Information</h3>" +
                "                <p><strong>Please arrive at the cinema at least 30 minutes before showtime to complete your payment.</strong></p>" +
                "                <p class='warning'>Your reservation will expire if not paid 15 minutes before showtime.</p>" +
                "                <p>Present this confirmation code at the cinema counter to pay for and collect your tickets.</p>" +
                "                <h4>No-Show Policy:</h4>" +
                "                <ul>" +
                "                    <li>First no-show: Warning notice</li>" +
                "                    <li>Second no-show: 7-day booking ban</li>" +
                "                    <li>Third no-show: Permanent block from online booking</li>" +
                "                </ul>" +
                "            </div>" +
                "            <p>We hope you enjoy the movie!</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            
            // Generate and attach PDF receipt
            try {
                byte[] pdfBytes = pdfGenerator.generateBookingReceipt(booking, payment);
                helper.addAttachment("booking_receipt_" + booking.getConfirmationCode() + ".pdf", 
                        new ByteArrayDataSource(pdfBytes, "application/pdf"));
            } catch (Exception e) {
                logger.warning("Failed to attach PDF receipt: " + e.getMessage());
            }
            
            mailSender.send(mimeMessage);
            logger.info("Booking confirmation email sent successfully to: " + to);
        } catch (MessagingException e) {
            logger.severe("Failed to send booking confirmation email: " + e.getMessage());
            e.printStackTrace();
            // Fallback to plain text email if HTML email fails
            String plainText = "Booking Confirmation - " + booking.getConfirmationCode() + "\n\n" +
                    "Movie: " + booking.getScreening().getMovie().getTitle() + "\n" +
                    "Date: " + booking.getScreening().getScreeningTime().toLocalDate() + "\n" +
                    "Time: " + booking.getScreening().getScreeningTime().toLocalTime() + "\n" +
                    "Total: " + booking.getTotalAmount() + "\n" +
                    "Payment Status: " + booking.getPaymentStatus() + "\n\n" +
                    "IMPORTANT: Please arrive at the cinema at least 30 minutes before showtime to complete your payment.\n" +
                    "Your reservation will expire if not paid 15 minutes before showtime.";
            
            sendEmail(booking.getUser().getEmail(), "Booking Confirmation", plainText);
        }
    }
    
    @Override
    public void sendPaymentReminderEmail(Booking booking) {
        try {
            User user = booking.getUser();
            String to = user.getEmail();
            String subject = "⏰ Payment Reminder - Cinema Movie Ticket";
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Format dates and currency
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            
            String movieTitle = booking.getScreening().getMovie().getTitle();
            LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
            String formattedScreeningDate = screeningTime.format(dateFormatter);
            String formattedScreeningTime = screeningTime.format(timeFormatter);
            String cinemaName = booking.getScreening().getCinema().getName();
            String totalAmount = currencyFormatter.format(booking.getTotalAmount());
            
            // Create HTML content
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #ff9800; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .booking-details { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin-bottom: 20px; }" +
                "        .urgent { background-color: #ffebee; border-left: 4px solid #f44336; padding: 15px; margin: 20px 0; }" +
                "        .confirmation-code { background-color: #ff9800; color: white; padding: 10px; text-align: center; font-size: 18px; margin: 20px 0; }" +
                "        .countdown { font-size: 24px; font-weight: bold; color: #f44336; text-align: center; margin: 20px 0; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>⏰ Payment Reminder</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear " + user.getFirstName() + ",</p>" +
                "            <p>This is a reminder that you have approximately <strong>15 minutes</strong> left to complete your payment at the counter before your booking is automatically voided.</p>" +
                "            <div class='confirmation-code'>" +
                "                Confirmation Code: <strong>" + booking.getConfirmationCode() + "</strong>" +
                "            </div>" +
                "            <div class='booking-details'>" +
                "                <h2>Booking Details</h2>" +
                "                <p><strong>Movie:</strong> " + movieTitle + "</p>" +
                "                <p><strong>Date:</strong> " + formattedScreeningDate + "</p>" +
                "                <p><strong>Time:</strong> " + formattedScreeningTime + "</p>" +
                "                <p><strong>Cinema:</strong> " + cinemaName + "</p>" +
                "                <p><strong>Number of Seats:</strong> " + booking.getNumberOfSeats() + "</p>" +
                "                <p><strong>Total Amount:</strong> " + totalAmount + "</p>" +
                "            </div>" +
                "            <div class='urgent'>" +
                "                <h3>⚠️ Important</h3>" +
                "                <p>Your booking will be automatically voided if payment is not completed 15 minutes before the screening time.</p>" +
                "                <p>Please proceed to the cinema counter immediately with your confirmation code to complete your payment.</p>" +
                "            </div>" +
                "            <p>If you have already made your payment, please disregard this message.</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Payment reminder email sent successfully to: " + to + " for booking: " + booking.getConfirmationCode());
        } catch (Exception e) {
            logger.severe("Failed to send payment reminder email for booking " + booking.getConfirmationCode() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to plain text email
            String plainText = "Payment Reminder - " + booking.getConfirmationCode() + "\n\n" +
                    "Dear " + booking.getUser().getFirstName() + ",\n\n" +
                    "This is a reminder that you have approximately 15 minutes left to complete your payment at the counter before your booking is automatically voided.\n\n" +
                    "Movie: " + booking.getScreening().getMovie().getTitle() + "\n" +
                    "Date: " + booking.getScreening().getScreeningTime().toLocalDate() + "\n" +
                    "Time: " + booking.getScreening().getScreeningTime().toLocalTime() + "\n" +
                    "Total: " + booking.getTotalAmount() + "\n\n" +
                    "Please proceed to the cinema counter immediately with your confirmation code to complete your payment.\n\n" +
                    "Best regards,\n" +
                    "Cinema Movie Ticket Booking System Team";
            
            sendEmail(booking.getUser().getEmail(), "Payment Reminder", plainText);
        }
    }
    
    @Override
    public void sendBookingVoidedEmail(Booking booking) {
        try {
            User user = booking.getUser();
            String to = user.getEmail();
            String subject = "IMPORTANT: Booking Voided - " + booking.getConfirmationCode();
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Format dates and currency
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            
            String movieTitle = booking.getScreening().getMovie().getTitle();
            LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
            String formattedScreeningDate = screeningTime.format(dateFormatter);
            String formattedScreeningTime = screeningTime.format(timeFormatter);
            String cinemaName = booking.getScreening().getCinema().getName();
            
            // Create HTML content
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #f44336; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .booking-details { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin-bottom: 20px; }" +
                "        .void-notice { background-color: #ffebee; border-left: 4px solid #f44336; padding: 15px; margin: 20px 0; }" +
                "        .confirmation-code { background-color: #f44336; color: white; padding: 10px; text-align: center; font-size: 18px; margin: 20px 0; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "        .no-show-warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Booking Voided</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear " + user.getFirstName() + ",</p>" +
                "            <div class='void-notice'>" +
                "                <h3>Your Booking Has Been Voided</h3>" +
                "                <p>We regret to inform you that your booking has been automatically voided as payment was not completed 15 minutes before the screening time.</p>" +
                "                <p>Your seats have been released and are now available for walk-in customers.</p>" +
                "            </div>" +
                "            <div class='confirmation-code'>" +
                "                Confirmation Code: <strong>" + booking.getConfirmationCode() + " (VOID)</strong>" +
                "            </div>" +
                "            <div class='booking-details'>" +
                "                <h2>Booking Details</h2>" +
                "                <p><strong>Movie:</strong> " + movieTitle + "</p>" +
                "                <p><strong>Date:</strong> " + formattedScreeningDate + "</p>" +
                "                <p><strong>Time:</strong> " + formattedScreeningTime + "</p>" +
                "                <p><strong>Cinema:</strong> " + cinemaName + "</p>" +
                "                <p><strong>Number of Seats:</strong> " + booking.getNumberOfSeats() + "</p>" +
                "                <p><strong>Status:</strong> VOID</p>" +
                "            </div>" +
                "            <p>If you still wish to watch this movie, you may purchase tickets at the cinema counter, subject to seat availability.</p>" +
                "            <p>Please note that this booking has been recorded as a no-show in our system.</p>" +
                "            <div class='no-show-warning'>" +
                "                <h3>No-Show Policy:</h3>" +
                "                <ul>" +
                "                    <li>First no-show: Warning notice</li>" +
                "                    <li>Second no-show: 7-day booking ban</li>" +
                "                    <li>Third no-show: Permanent block from online booking (must buy walk-in only)</li>" +
                "                </ul>" +
                "            </div>";
            
            // Add no-show count warning if applicable
            if (user.getNoShowCount() != null && user.getNoShowCount() > 0) {
                htmlContent += 
                "            <div class='no-show-warning'>" +
                "                <h3>No-Show Warning</h3>" +
                "                <p>You have " + user.getNoShowCount() + " no-show" + (user.getNoShowCount() > 1 ? "s" : "") + " on your record. Please be aware that multiple no-shows may result in temporary or permanent booking restrictions.</p>" +
                "                <p><strong>No-Shows: " + user.getNoShowCount() + "</strong></p>" +
                "            </div>";
            }
            
            htmlContent +=
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Booking voided email sent successfully to: " + to + " for booking: " + booking.getConfirmationCode());
        } catch (Exception e) {
            logger.severe("Failed to send booking voided email for booking " + booking.getConfirmationCode() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to plain text email
            String plainText = "IMPORTANT: Booking Voided - " + booking.getConfirmationCode() + "\n\n" +
                    "Dear " + booking.getUser().getFirstName() + ",\n\n" +
                    "We regret to inform you that your booking has been automatically voided as payment was not completed 15 minutes before the screening time.\n\n" +
                    "Movie: " + booking.getScreening().getMovie().getTitle() + "\n" +
                    "Date: " + booking.getScreening().getScreeningTime().toLocalDate() + "\n" +
                    "Time: " + booking.getScreening().getScreeningTime().toLocalTime() + "\n" +
                    "Status: VOID\n\n" +
                    "Your seats have been released and are now available for walk-in customers.\n\n" +
                    "If you still wish to watch this movie, you may purchase tickets at the cinema counter, subject to seat availability.\n\n" +
                    "Please note that this booking has been recorded as a no-show in our system.\n\n" +
                    "Best regards,\n" +
                    "Cinema Movie Ticket Booking System Team";
            
            sendEmail(booking.getUser().getEmail(), "IMPORTANT: Booking Voided", plainText);
        }
    }
    
    @Override
    public void sendNoShowWarning(User user, Booking booking) {
        try {
            String to = user.getEmail();
            String subject = "Warning: Missed Booking - Cinema Movie Ticket";
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Format dates
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
            
            String movieTitle = booking.getScreening().getMovie().getTitle();
            String formattedScreeningDate = booking.getScreening().getScreeningTime().toLocalDate().format(dateFormatter);
            String formattedScreeningTime = booking.getScreening().getScreeningTime().toLocalTime().format(timeFormatter);
            
            // Create HTML content
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Missed Booking Notice</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear " + user.getFirstName() + ",</p>" +
                "            <p>Our records show that you did not show up for your recent booking:</p>" +
                "            <div class='warning-box'>" +
                "                <p><strong>Movie:</strong> " + movieTitle + "</p>" +
                "                <p><strong>Date:</strong> " + formattedScreeningDate + "</p>" +
                "                <p><strong>Time:</strong> " + formattedScreeningTime + "</p>" +
                "                <p><strong>Booking Reference:</strong> " + booking.getConfirmationCode() + "</p>" +
                "            </div>" +
                "            <p>This is your <strong>" + (user.getNoShowCount() != null ? user.getNoShowCount() : 1) + " no-show</strong>.</p>";
            
            if (user.getNoShowCount() == null || user.getNoShowCount() == 1) {
                htmlContent += "<p>This is a warning notice. Please be aware of our no-show policy:</p>";
            } else if (user.getNoShowCount() == 2) {
                htmlContent += "<p>As this is your second no-show, your account has been temporarily blocked from making online bookings for 7 days" + 
                    (user.getBlockedUntil() != null ? " (until " + user.getBlockedUntil().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + ")" : "") + ".</p>";
            } else if (user.getNoShowCount() >= 3) {
                htmlContent += "<p>As this is your third no-show, your account has been permanently blocked from making online bookings. " +
                    "You will need to purchase tickets in person at the cinema.</p>";
            }
            
            htmlContent +=
                "            <div class='warning-box'>" +
                "                <h3>No-Show Policy:</h3>" +
                "                <ul>" +
                "                    <li>First no-show: Warning notice</li>" +
                "                    <li>Second no-show: 7-day booking ban</li>" +
                "                    <li>Third no-show: Permanent block from online booking (must buy walk-in only)</li>" +
                "                </ul>" +
                "            </div>" +
                "            <p>We understand that circumstances can change, but no-shows prevent other customers from enjoying movies and result in lost revenue for the cinema.</p>" +
                "            <p>If you believe this is an error or have any questions, please contact our customer service.</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("No-show warning email sent successfully to: " + to);
        } catch (Exception e) {
            logger.severe("Failed to send no-show warning email: " + e.getMessage());
            e.printStackTrace();
            // Fallback to plain text email if HTML email fails
            String plainText = "Warning: Missed Booking - Cinema Movie Ticket\n\n" +
                    "Dear " + user.getFirstName() + ",\n\n" +
                    "Our records show that you did not show up for your recent booking:\n" +
                    "Movie: " + booking.getScreening().getMovie().getTitle() + "\n" +
                    "Date: " + booking.getScreening().getScreeningTime().toLocalDate() + "\n" +
                    "Time: " + booking.getScreening().getScreeningTime().toLocalTime() + "\n" +
                    "Booking Reference: " + booking.getConfirmationCode() + "\n\n" +
                    "This is your " + (user.getNoShowCount() != null ? user.getNoShowCount() : 1) + " no-show.\n\n" +
                    "No-Show Policy:\n" +
                    "- First no-show: Warning notice\n" +
                    "- Second no-show: 7-day booking ban\n" +
                    "- Third no-show: Permanent block from online booking\n\n" +
                    "If you believe this is an error, please contact our customer service.";
            
            sendEmail(user.getEmail(), "Warning: Missed Booking", plainText);
        }
    }
    
    @Override
    public void sendNoShowWarningEmail(User user, int noShowCount) {
        if (user == null) {
            logger.warning("Cannot send no-show warning email to null user");
            return;
        }
        
        try {
            String to = user.getEmail();
            String subject = "Important: No-Show Warning";
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Format dates if needed
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            
            // Create HTML content
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Important: No-Show Warning</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear " + user.getFirstName() + ",</p>" +
                "            <p>This is an important notice regarding your booking history with our cinema.</p>";
            
            if (noShowCount == 1) {
                htmlContent += 
                    "            <div class='warning-box'>" +
                    "                <p>We have recorded your first no-show for a movie screening. Please be aware that our policy requires customers to attend their booked screenings or cancel them in advance.</p>" +
                    "                <p>Multiple no-shows may result in temporary booking restrictions.</p>" +
                    "            </div>";
            } else if (noShowCount == 2) {
                LocalDateTime blockedUntil = user.getBlockedUntil();
                String blockedUntilStr = blockedUntil != null ? blockedUntil.toLocalDate().format(dateFormatter) : "two weeks from now";
                
                htmlContent += 
                    "            <div class='warning-box'>" +
                    "                <p>We have recorded your second no-show for a movie screening. As per our policy, your account has been temporarily restricted from making new bookings until " + blockedUntilStr + ".</p>" +
                    "                <p>You can still attend any existing bookings you have made.</p>" +
                    "            </div>";
            } else {
                htmlContent += 
                    "            <div class='warning-box'>" +
                    "                <p>We have recorded multiple no-shows for your account. As per our policy, your account has been blocked from making new bookings.</p>" +
                    "                <p>To restore your booking privileges, please contact our customer support team.</p>" +
                    "            </div>";
            }
            
            htmlContent +=
                "            <div class='warning-box'>" +
                "                <h3>No-Show Policy:</h3>" +
                "                <ul>" +
                "                    <li>First no-show: Warning notice</li>" +
                "                    <li>Second no-show: 7-day booking ban</li>" +
                "                    <li>Third no-show: Permanent block from online booking (must buy walk-in only)</li>" +
                "                </ul>" +
                "            </div>" +
                "            <p>If you believe this is an error or have any questions, please contact our customer service.</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("No-show warning email sent successfully to: " + to);
        } catch (Exception e) {
            logger.severe("Failed to send no-show warning email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendNoShowBlockedEmail(User user) {
        if (user == null) {
            logger.warning("Cannot send blocked email to null user");
            return;
        }
        
        try {
            String to = user.getEmail();
            String subject = "Account Blocked - Multiple No-Shows";
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Create HTML content
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "        .blocked-box { background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Account Blocked - Multiple No-Shows</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear " + user.getFirstName() + ",</p>" +
                "            <div class='blocked-box'>" +
                "                <p>We regret to inform you that your account has been blocked from making new bookings due to multiple no-shows.</p>" +
                "                <p>Our records show that you have failed to attend 3 or more movie screenings without cancelling your booking in advance.</p>" +
                "            </div>" +
                "            <p>To discuss this matter and potentially restore your booking privileges, please contact our customer support team.</p>" +
                "            <div class='warning-box'>" +
                "                <h3>No-Show Policy:</h3>" +
                "                <ul>" +
                "                    <li>First no-show: Warning notice</li>" +
                "                    <li>Second no-show: 7-day booking ban</li>" +
                "                    <li>Third no-show: Permanent block from online booking (must buy walk-in only)</li>" +
                "                </ul>" +
                "            </div>" +
                "            <p>Thank you for your understanding.</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("No-show blocked email sent successfully to: " + to);
        } catch (Exception e) {
            logger.severe("Failed to send no-show blocked email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String to, String token, String applicationUrl) {
        try {
            String subject = "Password Reset Request - Cinema Movie Ticket Booking";
            String resetUrl = applicationUrl + "/reset-password?token=" + token;
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Create HTML content with hidden token
            String htmlContent = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { width: 100%; max-width: 600px; margin: 0 auto; }" +
                "        .header { background-color: #003366; color: white; padding: 20px; text-align: center; }" +
                "        .content { padding: 20px; }" +
                "        .reset-button { display: inline-block; background-color: #003366; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }" +
                "        .reset-button:hover { background-color: #004080; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #777; }" +
                "        .important-notice { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>Password Reset Request</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <p>Dear User,</p>" +
                "            <p>You have requested to reset your password for the Cinema Movie Ticket Booking System.</p>" +
                "            <p>To reset your password, please click the button below:</p>" +
                "            <div style='text-align: center;'>" +
                "                <a href='" + resetUrl + "' class='reset-button'>Reset Password</a>" +
                "            </div>" +
                "            <div class='important-notice'>" +
                "                <p><strong>Important:</strong></p>" +
                "                <ul>" +
                "                    <li>This password reset link will expire in 24 hours</li>" +
                "                    <li>If you did not request a password reset, please ignore this email</li>" +
                "                    <li>For security reasons, do not share this email with others</li>" +
                "                </ul>" +
                "            </div>" +
                "            <p>If the button above doesn't work, you can copy and paste this link into your browser:</p>" +
                "            <p style='word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;'>" + resetUrl + "</p>" +
                "            <p>Best regards,<br>Cinema Movie Ticket Booking System Team</p>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>This is an automated email. Please do not reply to this message.</p>" +
                "            <p>&copy; " + java.time.Year.now().getValue() + " Cinema Movie Ticket Booking System. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Password reset email sent successfully to: " + to);
        } catch (MessagingException e) {
            logger.severe("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            // Fallback to plain text email if HTML email fails
            String plainText = "Dear User,\n\n"
                    + "You have requested to reset your password for the Cinema Movie Ticket Booking System.\n\n"
                    + "To reset your password, please click on the link below:\n"
                    + applicationUrl + "/reset-password?token=" + token + "\n\n"
                    + "This link will expire in 24 hours.\n\n"
                    + "If you did not request a password reset, please ignore this email and your password will remain unchanged.\n\n"
                    + "Best regards,\n"
                    + "Cinema Movie Ticket Booking System Team";
            
            sendEmail(to, "Password Reset Request", plainText);
        }
    }
    
    @Override
    public void sendPasswordResetEmail(User user, String applicationUrl, String token) {
        sendPasswordResetEmail(user.getEmail(), token, applicationUrl);
    }
    
    @Override
    public void sendBookingCancellationEmail(Booking booking) {
        try {
            User user = booking.getUser();
            String to = user.getEmail();
            String subject = "Booking Cancellation Confirmation - " + booking.getConfirmationCode();
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cinemastarview@gmail.com");
            
            // Format dates and currency
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            
            String movieTitle = booking.getScreening().getMovie().getTitle();
            LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
            String formattedScreeningDate = screeningTime.format(dateFormatter);
            String formattedScreeningTime = screeningTime.format(timeFormatter);
            String cinemaName = booking.getScreening().getCinema().getName();
            String totalAmount = currencyFormatter.format(booking.getTotalAmount());
            LocalDateTime cancellationTime = LocalDateTime.now();
            String formattedCancellationTime = cancellationTime.format(dateFormatter) + " " + 
                                              cancellationTime.format(timeFormatter);
            
            // Try to use Thymeleaf template if available
            try {
                Context context = new Context();
                context.setVariable("user", user);
                context.setVariable("booking", booking);
                context.setVariable("movie", booking.getScreening().getMovie());
                context.setVariable("screening", booking.getScreening());
                context.setVariable("cancellationTime", cancellationTime);
                
                // Process the template - note the template is directly in templates directory
                String emailContent = templateEngine.process("booking-cancellation", context);
                helper.setText(emailContent, true);
            } catch (Exception e) {
                logger.warning("Failed to process Thymeleaf template: " + e.getMessage());
                
                // Fallback to inline HTML if template processing fails
                String htmlContent = 
                    "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "    <style>" +
                    "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                    "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                    "        .header { background-color: #f8f9fa; padding: 20px; text-align: center; border-bottom: 3px solid #dc3545; }" +
                    "        .content { padding: 20px; }" +
                    "        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #6c757d; }" +
                    "        .booking-details { background-color: #f8f9fa; padding: 15px; margin: 20px 0; border-left: 3px solid #dc3545; }" +
                    "        .highlight { color: #dc3545; font-weight: bold; }" +
                    "    </style>" +
                    "</head>" +
                    "<body>" +
                    "    <div class='container'>" +
                    "        <div class='header'>" +
                    "            <h1>Booking Cancellation Confirmation</h1>" +
                    "        </div>" +
                    "        <div class='content'>" +
                    "            <p>Dear " + user.getFirstName() + ",</p>" +
                    "            " +
                    "            <p>Your booking has been <span class='highlight'>successfully cancelled</span>. Here are the details of your cancelled booking:</p>" +
                    "            " +
                    "            <div class='booking-details'>" +
                    "                <p><strong>Confirmation Code:</strong> " + booking.getConfirmationCode() + "</p>" +
                    "                <p><strong>Movie:</strong> " + movieTitle + "</p>" +
                    "                <p><strong>Date:</strong> " + formattedScreeningDate + "</p>" +
                    "                <p><strong>Time:</strong> " + formattedScreeningTime + "</p>" +
                    "                <p><strong>Theater:</strong> " + cinemaName + "</p>" +
                    "                <p><strong>Number of Seats:</strong> " + booking.getNumberOfSeats() + "</p>" +
                    "                <p><strong>Total Amount:</strong> " + totalAmount + "</p>" +
                    "                <p><strong>Cancellation Time:</strong> " + formattedCancellationTime + "</p>" +
                    "            </div>" +
                    "            " +
                    "            <p>The seats you reserved have been released and are now available for other customers.</p>" +
                    "            " +
                    "            <p>If you have any questions or need further assistance, please don't hesitate to contact us.</p>" +
                    "            " +
                    "            <p>Thank you for using our service!</p>" +
                    "            " +
                    "            <p>Best regards,<br>" +
                    "            Movie Ticket Booking Team</p>" +
                    "        </div>" +
                    "        <div class='footer'>" +
                    "            <p>This is an automated email. Please do not reply to this message.</p>" +
                    "            <p>&copy; " + java.time.Year.now().getValue() + " Movie Ticket Booking System. All rights reserved.</p>" +
                    "        </div>" +
                    "    </div>" +
                    "</body>" +
                    "</html>";
                
                helper.setText(htmlContent, true);
            }
            
            mailSender.send(mimeMessage);
            logger.info("Cancellation email sent to " + to + " for booking " + booking.getConfirmationCode());
        } catch (Exception e) {
            logger.severe("Error sending cancellation email: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to plain text email if HTML email fails
            String plainText = "Booking Cancellation Confirmation - " + booking.getConfirmationCode() + "\n\n" +
                    "Dear " + booking.getUser().getFirstName() + ",\n\n" +
                    "Your booking has been successfully cancelled.\n\n" +
                    "Movie: " + booking.getScreening().getMovie().getTitle() + "\n" +
                    "Date: " + booking.getScreening().getScreeningTime().toLocalDate() + "\n" +
                    "Time: " + booking.getScreening().getScreeningTime().toLocalTime() + "\n" +
                    "Number of Seats: " + booking.getNumberOfSeats() + "\n" +
                    "Total Amount: " + booking.getTotalAmount() + "\n\n" +
                    "The seats you reserved have been released and are now available for other customers.\n\n" +
                    "Thank you for using our service!\n\n" +
                    "Best regards,\n" +
                    "Movie Ticket Booking Team";
            
            sendEmail(booking.getUser().getEmail(), "Booking Cancellation Confirmation", plainText);
        }
    }
}