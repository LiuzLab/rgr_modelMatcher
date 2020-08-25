package ubc.pavlab.rdp.services;

import org.springframework.security.authentication.BadCredentialsException;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.validation.ValidationException;
import java.util.*;

/**
 * Created by mjacobson on 16/01/18.
 */
public interface UserService {

    User create( User user );

    User createAdmin( User admin );

    User update( User user );

    void delete( User user );

    User changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException;

    String getCurrentUserName();

    String getCurrentEmail();

    User findCurrentUser();

    User findUserById( int id );

    User findUserByIdNoAuth( int id );

    User findUserByEmailNoAuth( String email );

    User getRemoteAdmin();

    Collection<User> findAll();

    Collection<User> findByLikeName( String nameLike );

    Collection<User> findByStartsName( String startsName );

    Collection<User> findByDescription( String descriptionLike );

    long countResearchers();

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms );

    UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon, int minSize, int maxSize, int minFrequency );

    void updateTermsAndGenesInTaxon( User user,
                                     Taxon taxon,
                                     Map<? extends Gene, TierType> genesToTierMapFrom,
                                     Map<? extends Gene, Optional<PrivacyLevelType>> genesToPrivacyLevelMap,
                                     Collection<? extends GeneOntologyTerm> goTerms );

    User updateUserProfileAndPublications( User user, Set<Publication> publications );

    PasswordResetToken createPasswordResetTokenForUser( User user, String token );

    void verifyPasswordResetToken( int userId, String token ) throws TokenException;

    User changePasswordByResetToken( int userId, String token, String newPassword );

    VerificationToken createVerificationTokenForUser( User user, String token );

    User confirmVerificationToken( String token );

    SortedSet<String> getLastNamesFirstChar();

    UUID getHiddenIdForUser( User user );

    User findUserByHiddenId( UUID hiddenId );

    boolean hasRole( User user, String role );
}
