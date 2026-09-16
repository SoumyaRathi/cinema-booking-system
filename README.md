<<<<<<< HEAD
# Cinema Booking System

A Spring Boot-based cinema ticket booking system with an admin panel for managing movies, showtimes, and bookings.

## Features

### User Features
- **Browse Movies**: View currently showing and upcoming movies
- **Book Tickets**: Select seats and book tickets for available showtimes
- **Seat Selection**: Interactive seat map with real-time availability
- **Booking History**: View past and upcoming bookings
- **User Authentication**: Secure login and registration system

### Admin Features
- **Movie Management**: Add, edit, and remove movies
- **Showtime Management**: Create and manage movie showtimes
- **Booking Management**: View and manage all bookings
- **User Management**: View and manage registered users
- **Dashboard**: Overview of bookings, revenue, and system statistics

## Tech Stack

### Backend
- **Java** - Programming language
- **Spring Boot** - Application framework
- **Spring MVC** - Web framework
- **Spring Data JPA** - Data persistence
- **Spring Security** - Authentication and authorization
- **Hibernate** - ORM framework
- **MySQL/PostgreSQL** - Database (choose based on your setup)
- **Maven** - Dependency management

### Frontend
- **HTML5** - Markup
- **CSS3** - Styling
- **JavaScript** - Client-side scripting
- **Thymeleaf** - Server-side template engine (if used)
- **Bootstrap** - CSS framework (if used)

## Prerequisites

Before running this application, make sure you have the following installed:
- Java JDK 11 or higher
- Maven 3.6+
- MySQL 8.0+ or PostgreSQL 12+
- Git

## Installation & Setup

### 1. Clone the repository
```bash
git clone https://github.com/iiammae/cinema-booking-system.git
cd cinema-booking-system
```

### 2. Configure Database
Create a database for the application:
```sql
CREATE DATABASE cinema_booking;
```

Update the database configuration in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cinema_booking
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### 3. Build the project
```bash
mvn clean install
```

### 4. Run the application
```bash
mvn spring-boot:run
```

Or run the generated JAR file:
```bash
java -jar target/cinema-booking-system-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## Default Admin Credentials

After first run, you can create an admin account or use these default credentials (if configured):
- **Username**: admin
- **Password**: admin123

⚠️ **Important**: Change the default password after first login!

## Project Structure

```
cinema-booking-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cinema/
│   │   │       ├── controller/     # Controllers
│   │   │       ├── model/          # Entity classes
│   │   │       ├── repository/     # Data repositories
│   │   │       ├── service/        # Business logic
│   │   │       └── config/         # Configuration classes
│   │   └── resources/
│   │       ├── static/             # CSS, JS, images
│   │       ├── templates/          # HTML templates
│   │       └── application.properties
│   └── test/                       # Test classes
├── .gitattributes
├── .gitignore
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## API Endpoints

### User Endpoints
- `GET /` - Home page
- `GET /movies` - List all movies
- `GET /movies/{id}` - Movie details
- `POST /booking` - Create booking
- `GET /booking/history` - View booking history

### Admin Endpoints
- `GET /admin` - Admin dashboard
- `GET /admin/movies` - Manage movies
- `POST /admin/movies` - Add new movie
- `PUT /admin/movies/{id}` - Update movie
- `DELETE /admin/movies/{id}` - Delete movie
- `GET /admin/bookings` - View all bookings

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Future Enhancements

- [ ] Payment gateway integration
- [ ] Email notifications for bookings
- [ ] Mobile responsive design
- [ ] QR code for tickets
- [ ] Movie reviews and ratings
- [ ] Multi-language support
- [ ] Social media integration

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

Your Name - [@iiammae](https://github.com/iiammae)

Project Link: [https://github.com/iiammae/cinema-booking-system](https://github.com/iiammae/cinema-booking-system)

## Acknowledgments

- Spring Boot Documentation
- Bootstrap
- Font Awesome Icons
- [Any other resources or inspirations]

---

**Note**: This is a learning project created for educational purposes.
=======
git add README.md
   git commit -m "Add README with features and tech stack"
   git push origin main
>>>>>>> 7bb073c217b6abfce712fef93fb3b3bf1e7ccb04
