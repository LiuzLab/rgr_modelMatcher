package ubc.pavlab.rdp.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ubc.pavlab.rdp.model.UserRegistrationMetrics;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRegistrationMetricsRepository extends JpaRepository<UserRegistrationMetrics, String> {

    @Query("SELECT metrics FROM UserRegistrationMetrics metrics WHERE metrics.dateId = :dateId")
    UserRegistrationMetrics findByDateId(@Param("dateId") String dateId);

}
