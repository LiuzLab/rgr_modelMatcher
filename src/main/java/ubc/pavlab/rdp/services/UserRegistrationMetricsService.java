package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.UserRegistrationMetrics;
import ubc.pavlab.rdp.repositories.UserRegistrationMetricsRepository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class UserRegistrationMetricsService {

    @Autowired
    private UserRegistrationMetricsRepository metricsRepository;

    @Autowired
    private UserService userService; // Assuming UserService has a method to get the total user count

    /**
     * Method to generate a date-based ID in the format yyyyMMdd
     *
     * @return A string representing the date-based ID
     */
    private String getDateBasedId() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC); // Get current date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // Define format
        return today.format(formatter); // Convert to string in yyyyMMdd format
    }

    /**
     * Method to increment the daily registration count and update metrics
     */
    @Transactional
    public void incrementDailyRegistrationCount() {
        String dateId = getDateBasedId(); // Generate the date-based ID
        UserRegistrationMetrics metrics = metricsRepository.findByDateId(dateId);


        if (metrics == null) {
            // Initialize with the total number of users up to now
            metrics = new UserRegistrationMetrics();
            metrics.setDateId(dateId);

            Timestamp currentDate = Timestamp.valueOf(LocalDate.now(ZoneOffset.UTC).atStartOfDay());
            metrics.setDate(currentDate); // Set current date timestamp

            metrics.setTotalRegistrations(userService.getTotalUserCount() + 1); // Set the total number of users up to today
            metrics.setDailyRegistrations(1); // First registration of the day
        } else {
            metrics.incrementRegistrations(); // Increment both total and daily registrations
        }

        metricsRepository.save(metrics);
    }
}