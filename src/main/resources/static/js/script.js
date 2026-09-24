// Common JavaScript functions for the Cinema Booking System

// Initialize tooltips
document.addEventListener('DOMContentLoaded', function() {
    // Initialize any Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize datetime pickers if any
    const datetimeInputs = document.querySelectorAll('input[type="datetime-local"]');
    if (datetimeInputs.length > 0) {
        // Set min date to today
        const today = new Date();
        const formattedDate = today.toISOString().slice(0, 16);
        datetimeInputs.forEach(input => {
            input.min = formattedDate;
        });
    }
    
    // Add event listeners for movie search if search box exists
    const searchBox = document.getElementById('movieSearch');
    if (searchBox) {
        searchBox.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            const movieCards = document.querySelectorAll('.movie-card');
            
            movieCards.forEach(card => {
                const title = card.querySelector('.card-title').textContent.toLowerCase();
                const genre = card.querySelector('.badge').textContent.toLowerCase();
                
                if (title.includes(searchTerm) || genre.includes(searchTerm)) {
                    card.style.display = 'block';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    }
    
    // Add confirmation for delete actions
    const deleteButtons = document.querySelectorAll('.btn-danger');
    deleteButtons.forEach(button => {
        if (!button.hasAttribute('data-confirm-set')) {
            button.setAttribute('data-confirm-set', 'true');
            button.addEventListener('click', function(e) {
                if (!confirm('Are you sure you want to proceed with this action?')) {
                    e.preventDefault();
                }
            });
        }
    });
    
    // Auto-dismiss alerts after 5 seconds
    setTimeout(function() {
        $('.alert').alert('close');
    }, 5000);
    
    // Display file name when selected for custom file inputs
    $('.custom-file-input').on('change', function() {
        var fileName = $(this).val().split('\\').pop();
        $(this).next('.custom-file-label').html(fileName);
        
        // Image preview
        if (this.files && this.files[0]) {
            var reader = new FileReader();
            reader.onload = function(e) {
                $('#imagePreview').show();
                $('#imagePreview img').attr('src', e.target.result);
            }
            reader.readAsDataURL(this.files[0]);
        }
    });
    
    // Initialize flatpickr for date/time pickers if available
    if (typeof flatpickr !== 'undefined') {
        flatpickr("#screeningDateTime", {
            enableTime: true,
            dateFormat: "Y-m-d H:i",
            minDate: "today",
            time_24hr: true
        });
    }
    
    // Set min date to today for date inputs
    var dateInputs = document.querySelectorAll('input[type="date"]');
    if (dateInputs.length > 0) {
        var today = new Date().toISOString().split('T')[0];
        dateInputs.forEach(function(input) {
            if (!input.min) {
                input.setAttribute('min', today);
            }
            if (!input.value && input.id === 'screeningDate') {
                input.value = today;
            }
        });
    }
    
    // Screening time selection functionality
    initScreeningTimeSelection();
    
    // Initialize DataTables if available
    initDataTables();
    
    // Initialize seat selection if on booking page
    initSeatSelection();
    
    // Initialize booking page functionality
    initBookingPage();
    
    // Initialize admin bookings page functionality
    initAdminBookingsPage();
    
    // Initialize countdown timer for booking confirmation
    initCountdownTimer();
    
    // Initialize counter payments admin functionality
    initCounterPaymentsAdmin();
    
    // Initialize sales report charts if available
    initSalesReportCharts();
    
    // Initialize notification functionality
    initNotifications();
    
    // Initialize time option selection
    initTimeOptionSelection();
    
    // Initialize admin dashboard functionality
    initAdminDashboard();
    
    // Initialize carousel if it exists
    if ($('.carousel').length > 0) {
        $('.carousel').carousel({
            interval: 5000
        });
    }
    
    // Initialize movie details page functionality
    initMovieDetailsPage();
    
    // Initialize movies page functionality
    initMoviesPage();
    
    // Initialize movie screening page functionality
    initMovieScreeningPage();
});

// Function to validate registration form
function validateRegistrationForm() {
    const password = document.getElementById('password').value;
    const passwordConfirm = document.getElementById('passwordConfirm').value;
    
    if (password !== passwordConfirm) {
        alert('Passwords do not match!');
        return false;
    }
    
    if (password.length < 6) {
        alert('Password must be at least 6 characters long!');
        return false;
    }
    
    return true;
}

// Function to filter movies by genre
function filterMoviesByGenre(genre) {
    const movieCards = document.querySelectorAll('.movie-card');
    
    if (genre === 'all') {
        movieCards.forEach(card => {
            card.style.display = 'block';
        });
        return;
    }
    
    movieCards.forEach(card => {
        const movieGenre = card.querySelector('.badge').textContent.toLowerCase();
        
        if (movieGenre === genre.toLowerCase()) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}

// Function to initialize screening time selection
function initScreeningTimeSelection() {
    // Update selected times display when checkboxes change
    $(document).on('change', 'input[name="screeningTimes"]', function() {
        updateSelectedTimes();
    });
    
    // Add custom time button functionality
    $('#addCustomTime').click(function() {
        var customTime = $('#customTime').val().trim();
        if (customTime) {
            // Validate time format (24-hour)
            var timeRegex = /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/;
            if (!timeRegex.test(customTime)) {
                alert('Please enter a valid time in 24-hour format (HH:MM)');
                return;
            }
            
            // Check if this time is available
            var cinemaId = $('#cinemaId').val();
            var screeningDate = $('#screeningDate').val();
            
            if (!cinemaId) {   alert('Please select a cinema first');
                return;
            }
            
            if (!screeningDate) {
                alert('Please select a date first');
                return;
            }
            
            // Check availability via AJAX
            $.ajax({
                url: '/admin/screenings/available-times',
                type: 'GET',
                data: {
                    cinemaId: cinemaId,
                    date: screeningDate
                },
                success: function(availableTimes) {
                    if (availableTimes.includes(customTime)) {
                        // Time is available, add it
                        addCustomTimeToForm(customTime);
                    } else {
                        alert('This time is not available. Another movie is already scheduled at this time in this cinema.');
                    }
                },
                error: function() {
                    alert('Error checking time availability. Please try again.');
                }
            });
        }
    });
    
    // When cinema or date changes, update available times
    $('#cinemaId, #screeningDate').change(function() {
        var cinemaId = $('#cinemaId').val();
        var screeningDate = $('#screeningDate').val();
        
        if (cinemaId && screeningDate) {
            updateAvailableTimes(cinemaId, screeningDate);
        }
    });
}

// Function to add custom time to form
function addCustomTimeToForm(customTime) {
    // Create a unique ID for the checkbox
    var timeId = 'custom-' + Date.now();
    
    // Add a new checkbox for the custom time
    var customTimeHtml = `
        <div class="form-check time-checkbox">
            <input class="form-check-input" type="checkbox" name="screeningTimes" id="${timeId}" value="${customTime}" checked>
            <label class="form-check-label" for="${timeId}">${customTime} (custom)</label>
        </div>
    `;
    
    // Append to the first row
    $('.row.mb-3:first').append(customTimeHtml);
    
    // Clear the input
    $('#customTime').val('');
    
    // Update selected times display
    updateSelectedTimes();
}

// Function to update selected times display
function updateSelectedTimes() {
    var selectedTimes = [];
    $('input[name="screeningTimes"]:checked').each(function() {
        selectedTimes.push($(this).val());
    });
    
    if (selectedTimes.length > 0) {
        $('#selectedTimes').removeClass('d-none');
        
        var html = '';
        selectedTimes.forEach(function(time) {
            html += '<span class="time-badge">' + time + '</span>';
        });
        
        $('#timesList').html(html);
    } else {
        $('#selectedTimes').addClass('d-none');
    }
}

// Function to update available times
function updateAvailableTimes(cinemaId, date) {
    $.ajax({
        url: '/admin/screenings/available-times',
        type: 'GET',
        data: {
            cinemaId: cinemaId,
            date: date
        },
        success: function(availableTimes) {
            // Reset all checkboxes
            $('input[name="screeningTimes"]').prop('checked', false);
            $('.time-checkbox').removeClass('disabled');
            
            // Standard times to check
            var standardTimes = ['10:00', '13:00', '16:00', '19:00', '22:00'];
            
            // Disable times that are not available
            standardTimes.forEach(function(time) {
                var timeCheckbox = $('input[value="' + time + '"]');
                if (timeCheckbox.length && !availableTimes.includes(time)) {
                    timeCheckbox.prop('disabled', true);
                    timeCheckbox.closest('.time-checkbox').addClass('disabled');
                } else if (timeCheckbox.length) {
                    timeCheckbox.prop('disabled', false);
                }
            });
            
            // Remove any custom times that are not available
            $('input[name="screeningTimes"]').each(function() {
                var value = $(this).val();
                if (!standardTimes.includes(value) && !availableTimes.includes(value)) {
                    $(this).closest('.time-checkbox').remove();
                }
            });
            
            updateSelectedTimes();
        },
        error: function() {
            console.error('Error fetching available times');
        }
    });
}

// Function to initialize DataTables
function initDataTables() {
    if ($.fn.DataTable && $('#paymentsTable').length) {
        $('#paymentsTable').DataTable({
            "order": [[ 7, "desc" ]],
            "pageLength": 25,
            "language": {
                "search": "Search payments:",
                "lengthMenu": "Show _MENU_ payments per page",
                "info": "Showing _START_ to _END_ of _TOTAL_ payments",
                "infoEmpty": "Showing 0 to 0 of 0 payments",
                "infoFiltered": "(filtered from _MAX_ total payments)"
            }
        });
    }
}

// Function to initialize seat selection
function initSeatSelection() {
    if ($('.seat-map').length) {
        const ticketPrice = parseFloat($('#total-amount').data('price') || 0);
        const selectedSeats = new Set();
        
        // Handle seat selection
        $('.seat-available').click(function() {
            const seatId = $(this).data('seat');
            
            if ($(this).hasClass('seat-selected')) {
                $(this).removeClass('seat-selected').addClass('seat-available');
                selectedSeats.delete(seatId);
            } else {
                $(this).removeClass('seat-available').addClass('seat-selected');
                selectedSeats.add(seatId);
            }
            
            updateBookingSummary();
        });
        
        function updateBookingSummary() {
            const seatCount = selectedSeats.size;
            const totalAmount = (seatCount * ticketPrice).toFixed(2);
            
            $('#seat-count').text(seatCount);
            $('#total-amount').text('₱' + totalAmount);
            
            if (seatCount > 0) {
                let seatsList = '';
                selectedSeats.forEach(seat => {
                    seatsList += `<div class="badge badge-primary mr-1 mb-1 p-2">${seat}</div>`;
                });
                $('#selected-seats-list').html(seatsList);
                $('#proceed-btn').prop('disabled', false);
            } else {
                $('#selected-seats-list').html('<p class="text-muted">No seats selected</p>');
                $('#proceed-btn').prop('disabled', true);
            }
            
            // Update hidden input for form submission
            $('#selected-seats-input').val(Array.from(selectedSeats).join(','));
        } 
    }
}

// Function to initialize booking page
function initBookingPage() {
    // Calculate total price when number of tickets changes
    $('#numberOfSeats').change(function() {
        if ($(this).length) {
            // Get the ticket price from the page
            var priceElement = document.querySelector('input[readonly]');
            if (priceElement) {
                var ticketPrice = parseFloat(priceElement.value);
                var numberOfSeats = $(this).val();
                var total = numberOfSeats * ticketPrice;
                $('#totalPrice').val(total.toFixed(2));
            }
        }
    });
}

// Function to initialize admin bookings page
function initAdminBookingsPage() {
    if ($('#bookingsTable').length) {
        // Sort bookings by booking time (latest first)
        sortTableByDate();
        
        // Search functionality
        $("#searchInput").on("keyup", function() {
            filterTable();
        });
        
        // Clear search
        $("#clearSearch").click(function() {
            $("#searchInput").val('');
            filterTable();
        });
        
        // Status filter
        $("#statusFilter").change(function() {
            filterTable();
        });
        
        // Date filter badges
        $(".filter-badge").click(function() {
            $(".filter-badge").removeClass("badge-primary").addClass("badge-secondary");
            $(this).removeClass("badge-secondary").addClass("badge-primary");
            filterTable();
        });
        
        // Initial filter
        filterTable();
    }
}

// Function to sort table by booking date (latest first)
function sortTableByDate() {
    var tbody = $("#bookingsTable tbody");
    if (tbody.length) {
        var rows = tbody.find("tr").toArray();
        
        rows.sort(function(a, b) {
            var dateA = $(a).find("td:eq(8)").text(); // Booking Time column (now index 8 with confirmation code added)
            var dateB = $(b).find("td:eq(8)").text();
            return new Date(dateB) - new Date(dateA); // Sort in descending order (latest first)
        });
        
        $.each(rows, function(index, row) {
            tbody.append(row);
        });
    }
}

// Function to filter the table based on search input and filters
function filterTable() {
    if ($("#searchInput").length) {
        var searchValue = $("#searchInput").val().toLowerCase();
        var statusFilter = $("#statusFilter").val();
        var dateFilter = $(".filter-badge.badge-primary").data("filter") || "all";
        var visibleCount = 0;
        
        // Get current date for date filtering
        var today = new Date();
        today.setHours(0, 0, 0, 0);
        
        var weekStart = new Date(today);
        weekStart.setDate(today.getDate() - today.getDay());
        
        var monthStart = new Date(today);
        monthStart.setDate(1);
        
        $(".booking-row").each(function() {
            var row = $(this);
            var rowText = row.text().toLowerCase();
            var rowCode = row.data("code").toLowerCase();
            var rowStatus = row.data("status");
            var rowPaid = row.data("paid");
            var rowDate = new Date(row.data("booking-time"));
            
            // Search text filter - check both visible text and confirmation code
            var matchesSearch = searchValue === "" || 
                               rowText.indexOf(searchValue) > -1 || 
                               rowCode.indexOf(searchValue) > -1;
            
            // Status filter
            var matchesStatus = statusFilter === "all" || 
                               (statusFilter === "paid" && rowPaid === true) ||
                               (statusFilter === "unpaid" && rowPaid === false) ||
                               statusFilter === rowStatus;
            
            // Date filter
            var matchesDate = dateFilter === "all" ||
                             (dateFilter === "today" && rowDate >= today) ||
                             (dateFilter === "week" && rowDate >= weekStart) ||
                             (dateFilter === "month" && rowDate >= monthStart);
            
            // Show/hide row based on all filters
            if (matchesSearch && matchesStatus && matchesDate) {
                row.show();
                visibleCount++;
                
                // Highlight search terms if there's a search value
                if (searchValue !== "") {
                    row.find("td").each(function() {
                        var cell = $(this);
                        var cellText = cell.text().toLowerCase();
                        
                        if (cellText.indexOf(searchValue) > -1 && !cell.hasClass("actions")) {
                            cell.addClass("highlight");
                        } else {
                            cell.removeClass("highlight");
                        }
                    });
                } else {
                    row.find("td").removeClass("highlight");
                }
            } else {
                row.hide();
            }
        });
        
        // Show/hide no results message
        if (visibleCount === 0) {
            $("#noResults").show();
        } else {
            $("#noResults").hide();
        }
        
        // Update results count
        $("#resultsCount").text("Showing " + visibleCount + " bookings");
    }
}

// Function to initialize countdown timer for booking confirmation
function initCountdownTimer() {
    const countdownEl = document.getElementById('countdown');
    if (countdownEl) {
        // Initialize countdown timer
        let timeLeft = 10 * 60; // 10 minutes in seconds
        
        function updateTimer() {
            const minutes = Math.floor(timeLeft / 60);
            let seconds = timeLeft % 60;
            seconds = seconds < 10 ? '0' + seconds : seconds;
            countdownEl.textContent = minutes + ':' + seconds;
            
            if (timeLeft <= 0) {
                clearInterval(timerInterval);
                alert('Your session has expired. Please start over.');
                window.location.href = '/movies';
            }
            timeLeft--;
        }
        
        updateTimer();
        const timerInterval = setInterval(updateTimer, 1000);
    }
}

// Function to initialize counter payments admin functionality
function initCounterPaymentsAdmin() {
    // Search functionality for payment cards
    $("#searchInput").on("keyup", function() {
        var value = $(this).val().toLowerCase();
        $(".payment-item").filter(function() {
            $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1)
        });
    });
    
    // Update countdown timers
    function updateCountdowns() {
        $('.countdown').each(function() {
            var expirationTime = new Date($(this).data('expiration'));
            var now = new Date();
            var diff = expirationTime - now;
            
            if (diff <= 0) {
                $(this).text('EXPIRED');
                $(this).addClass('text-danger');
                
                // Hide the process payment button for expired bookings
                $(this).closest('.payment-card').find('.btn-success').hide();
                
                // Add an expired badge to the card header
                var header = $(this).closest('.payment-card').find('.payment-card-header');
                if (header.find('.badge-danger').length === 0) {
                    header.append('<span class="badge badge-danger ml-2">Expired</span>');
                }
            } else {
                var hours = Math.floor(diff / (1000 * 60 * 60));
                var minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
                
                if (hours > 0) {
                    $(this).text(hours + 'h ' + minutes + 'm remaining');
                } else {
                    $(this).text(minutes + ' minutes remaining');
                }
                
                if (diff < 30 * 60 * 1000) { // Less than 30 minutes
                    $(this).addClass('text-danger');
                }
            }
        });
    }
    
    // If countdown elements exist, update them
    if ($('.countdown').length > 0) {
        // Update countdowns immediately and then every minute
        updateCountdowns();
        setInterval(updateCountdowns, 60000);
        
        // Refresh the page every 5 minutes to get updated data
        setTimeout(function() {
            location.reload();
        }, 5 * 60 * 1000);
    }
}

// Function to initialize sales report charts
function initSalesReportCharts() {
    // Check if we're on the sales report page with Chart.js available
    if (typeof Chart !== 'undefined' && document.getElementById('salesTrendChart')) {
        // Chart initialization will be handled by the inline script in the page
        console.log('Sales report chart ready for initialization');
    }
}

// Function to initialize notifications
function initNotifications() {
    // Mark notification as read when clicked
    $('.notification-item').click(function() {
        var notificationId = $(this).data('id');
        var link = $(this).data('link');
        
        // Send AJAX request to mark notification as read
        $.ajax({
            url: '/admin/notifications/mark-read/' + notificationId,
            type: 'POST',
            success: function() {
                // Redirect to the notification link if provided
                if (link) {
                    window.location.href = link;
                }
            }
        });
    });
    
    // Mark all notifications as read
    $('#markAllRead').click(function(e) {
        e.preventDefault();
        
        // Send AJAX request to mark all notifications as read
        $.ajax({
            url: '/admin/notifications/mark-all-read',
            type: 'POST',
            success: function() {
                // Update UI to reflect all notifications are read
                $('.notification-item').removeClass('unread');
                $('.notification-badge').text('0').hide();
            }
        });
    });
}

// Function to initialize time option selection
function initTimeOptionSelection() {
    // Handle time selection
    $('.time-option').click(function() {
        if (!$(this).hasClass('disabled')) {
            $('.time-option').removeClass('selected');
            $(this).addClass('selected');
            $('#screeningTime').val($(this).data('value'));
        }
    });
    
    // When cinema or date changes, update available times
    $('#cinemaId, #screeningDate').change(function() {
        var cinemaId = $('#cinemaId').val();
        var screeningDate = $('#screeningDate').val();
        var screeningId = $('#screeningId').val();
        
        if (cinemaId && screeningDate) {
            updateAvailableTimeOptions(cinemaId, screeningDate, screeningId);
        }
    });
}

// Function to update available time options
function updateAvailableTimeOptions(cinemaId, date, screeningId) {
    $.ajax({
        url: '/admin/screenings/available-times',
        type: 'GET',
        data: {
            cinemaId: cinemaId,
            date: date,
            screeningId: screeningId
        },
        success: function(availableTimes) {
            // Clear existing time options
            $('#timeOptions').empty();
            
            // Get current selected time
            var currentTime = $('#screeningTime').val();
            var foundCurrentTime = false;
            
            // Add new time options
            availableTimes.forEach(function(time) {
                var isSelected = time === currentTime;
                if (isSelected) {
                    foundCurrentTime = true;
                }
                
                var timeOption = $('<div class="time-option" data-value="' + time + '">' + time + '</div>');
                if (isSelected) {
                    timeOption.addClass('selected');
                }
                
                $('#timeOptions').append(timeOption);
            });
            
            // If current time is not in available times, add it
            if (!foundCurrentTime && currentTime) {
                var currentTimeOption = $('<div class="time-option selected" data-value="' + currentTime + '">' + currentTime + ' (current)</div>');
                $('#timeOptions').append(currentTimeOption);
            }
            
            // Reattach click handlers
            $('.time-option').click(function() {
                if (!$(this).hasClass('disabled')) {
                    $('.time-option').removeClass('selected');
                    $(this).addClass('selected');
                    $('#screeningTime').val($(this).data('value'));
                }
            });
        },
        error: function() {
            console.error('Error fetching available times');
        }
    });
}

// Function to initialize admin dashboard
function initAdminDashboard() {
    // Check if we're on the admin dashboard page
    if ($('.stat-card').length > 0) {
        // Auto-refresh dashboard data every 5 minutes
        setTimeout(function() {
            location.reload();
        }, 5 * 60 * 1000);
    }
}

// Function to initialize movie details page
function initMovieDetailsPage() {
    // Auto-dismiss alerts after 5 seconds (already included in DOMContentLoaded)
}

// Function to initialize movies page
function initMoviesPage() {
    // Filter functionality
    $("#searchMovie").on("keyup", function() {
        var value = $(this).val().toLowerCase();
        $(".movie-item").filter(function() {
            $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1)
        });
    });
    
    $("#genreFilter").on("change", function() {
        var value = $(this).val().toLowerCase();
        if (value === "") {
            $(".movie-item").show();
        } else {
            $(".movie-item").filter(function() {
                $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1);
            });
        }
    });
}

// Function to initialize movie screening page
function initMovieScreeningPage() {
    // Handle date navigation
    $('.date-item').click(function() {
        // Remove active class from all date items
        $('.date-item').removeClass('active');
        // Add active class to clicked date item
        $(this).addClass('active');
        
        // Get the date from data attribute
        var dateStr = $(this).data('date');
        
        // Hide all date sections
        $('.date-section').removeClass('active');
        
        // Show only the selected date section
        $('#date-' + dateStr).addClass('active');
    });
    
    // Initialize with the first date
    var initialDate = $('.date-item.active').data('date');
    if (initialDate) {
        $('.date-section').removeClass('active');
        $('#date-' + initialDate).addClass('active');
    }
}

// Additional functions from index.html
$(document).ready(function() {
    // Auto-dismiss alerts after 5 seconds (already included in DOMContentLoaded)
    
    // Initialize carousel (already included in DOMContentLoaded)
});