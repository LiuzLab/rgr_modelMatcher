package ubc.pavlab.rdp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "user_registration_metrics")
@Data
@NoArgsConstructor
public class UserRegistrationMetrics {

    @Id
    private String dateId; // Use date-based string as ID

    @Column(nullable = false)
    private Timestamp date;  // Use Timestamp to store only the date component

    @Column(nullable = false)
    private long totalRegistrations; // Total number of registrations up to this day

    @Column(nullable = false)
    private long dailyRegistrations; // Number of registrations for the specific day

    @Version
    private int version; // Optimistic locking field

    // Combined method to increment both total and daily registrations
    public void incrementRegistrations() {
        this.totalRegistrations++;
        this.dailyRegistrations++;
    }
}