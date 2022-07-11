package ubc.pavlab.rdp.model;

import lombok.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

/**
 * Created by mjacobson on 19/01/18.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = { "user" }, callSuper = true)
public class PasswordResetToken extends Token implements UserContent, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Override
    public Optional<User> getOwner() {
        return Optional.of( getUser() );
    }

    @Override
    protected TemporalAmount getDuration() {
        return Duration.of( 2, ChronoUnit.HOURS );
    }

    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return PrivacyLevelType.PRIVATE;
    }
}
