package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.events.OnContactEmailUpdateEvent;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.persistence.PersistenceContext;
import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;


@Service("matchDashboardService")
@CommonsLog
public class MatchDashboardServiceImpl  implements MatchDashboardService {


    @Autowired
    ApplicationSettings applicationSettings;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRegistrationMetricsService userRegistrationMetricsService;

    @Autowired
    PrivacyService privacyService;

    @PersistenceContext
    private EntityManager entityManager;



    @Transactional
    @Override
    public User createNewUser(User user) {

        User userExists = userService.findUserByEmailNoAuth( user.getEmail() );

        user.setEnabled( false );

        // initialize a basic user profile
        Profile userProfile = user.getProfile();
        userProfile.setPrivacyLevel( privacyService.getDefaultPrivacyLevel() );
        userProfile.setShared( applicationSettings.getPrivacy().isDefaultSharing() );
        userProfile.setHideGenelist( false );
        userProfile.setContactEmailVerified( false );

        OffsetDateTime utcDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        Timestamp timestamp = Timestamp.from(utcDateTime.toInstant());
        user.setRegistrationDate(timestamp);


        if ( userExists != null ) {
            log.warn( "Trying to register an already registered email." );
        }

//        if ( bindingResult.hasErrors() ) {
//
//        } else {
//            user = userService.create( user );
//            VerificationToken token = userService.createVerificationTokenForUser( user );
//            eventPublisher.publishEvent( new OnRegistrationCompleteEvent( user, token ) );
//
//            userRegistrationMetricsService.incrementDailyRegistrationCount(); // Update the registration metrics
//        }

        return user;
    }



    @Transactional
    @Override
    public User createNewUserWithUserInfo(String email,
                                          Profile profile,
                                          Set<Publication> publications,
                                          Set<String> organUberonIds,
                                          Map<String, TaxonGenesPayload> taxonGenesPayload) {

        // Check if user already exists
        // Validate email
        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email provided.");
        }

        // Check if user already exists
        User userExists = userService.findUserByEmailNoAuth(email);
        if (userExists != null) {
            log.warn("User with email " + email + " already exists.");
            return null; // or throw new UserAlreadyExistsException(email);
        }


        // Create and initialize the user object
        User newUser = initializeNewUser(email, profile);

        // Add publications if provided
//        addPublicationsToUser(newUser, publications);

        updateUserPublications(newUser, publications);

        // Add organs if provided
//        addOrgansToUser(newUser, organUberonIds);

        updateUserOrgansAndResearcherCategories(newUser, organUberonIds, newUser.getProfile());


        // Add genes and taxon descriptions if provided
//        addGenesAndTaxonDescriptionsToUser(newUser, taxonGenesPayload);



        buildUserGenesFromTaxonPayload(newUser, taxonGenesPayload);




        // Save and return the newly created user


        log.info( MessageFormat.format( " creating this new scientist user profile for email {0}  :: Saving User :: {1}.", email, newUser ) );
        log.info( MessageFormat.format( " PROFILE : {0} .", newUser.getProfile() ) );
        log.info( MessageFormat.format( " getPublications : {0} .", newUser.getProfile().getPublications() ) );
        log.info( MessageFormat.format( " getResearcherCategories : {0} .", newUser.getProfile().getResearcherCategories() ) );
        log.info( MessageFormat.format( " getUserGenes : {0} .", newUser.getUserGenes() ) );
//        return newUser;

        return userService.create(newUser);


    }


    @Transactional
    @Override
    public User createNewUserWithUserInfoTest(String email,
                                          Profile profile,
                                          Set<Publication> publications,
                                          Set<String> organUberonIds,
                                          Map<String, TaxonGenesPayload> taxonGenesPayload) {

        // Check if user already exists
        // Validate email
        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email provided.");
        }

        // Check if user already exists
        User userExists = userService.findUserByEmailNoAuth(email);
        if (userExists != null) {
            log.warn("User with email " + email + " already exists.");
            return null; // or throw new UserAlreadyExistsException(email);
        }


        // Create and initialize the user object
        User newUser = initializeNewUser(email, profile);

        // Add publications if provided
        addPublicationsToUser(newUser, publications);

        // Add organs if provided
        addOrgansToUser(newUser, organUberonIds);

        // Add genes and taxon descriptions if provided
        addGenesAndTaxonDescriptionsToUser(newUser, taxonGenesPayload);


        User user = new User();
        user = updateUserBasicProfile(user, profile);
        user = updateUserPublications(user, publications);
        user = updateUserOrgansAndResearcherCategories(user, organUberonIds, profile);
        user = updateUserGenesAndTaxonDescriptions(user, taxonGenesPayload);




        // Save and return the newly created user


        log.info( MessageFormat.format( " creating this new scientist user profile for email {0}  :: Saving User :: {1}.", email, newUser ) );
        log.info( MessageFormat.format( " PROFILE : {0} .", newUser.getProfile() ) );
        log.info( MessageFormat.format( " getPublications : {0} .", newUser.getProfile().getPublications() ) );
        log.info( MessageFormat.format( " getResearcherCategories : {0} .", newUser.getProfile().getResearcherCategories() ) );
        log.info( MessageFormat.format( " getUserGenes : {0} .", newUser.getUserGenes() ) );
        return newUser;

//        return userService.create(newUser);


    }



    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"; // Basic email format validation regex
        return email.matches(emailRegex);
    }

    private User initializeNewUser(String email, Profile profile) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setEnabled(false); // User starts as disabled by default

        // Initialize and set the profile with default values
        Profile userProfile = profile;
        userProfile.setPrivacyLevel(privacyService.getDefaultPrivacyLevel());
        userProfile.setShared(applicationSettings.getPrivacy().isDefaultSharing());
        userProfile.setHideGenelist(false); // Default value
        userProfile.setContactEmailVerified(false); // Default value

        newUser.setProfile(userProfile);

        newUser.setPassword("CheckModelMatcherDBForUser");

        // Set registration date to current UTC time
        OffsetDateTime utcDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        Timestamp timestamp = Timestamp.from(utcDateTime.toInstant());
        newUser.setRegistrationDate(timestamp);

        return newUser;
    }

    private void addPublicationsToUser(User user, Set<Publication> publications) {
        if (publications != null && !publications.isEmpty()) {
            user.getProfile().getPublications().addAll(publications);
        }
    }

    private void addOrgansToUser(User user, Set<String> organUberonIds) {
        if (organUberonIds != null && !organUberonIds.isEmpty()) {
            Set<UserOrgan> userOrgans = organUberonIds.stream()
                    .map(uberonId -> {
                        UserOrgan organ = new UserOrgan();
                        organ.setUberonId(uberonId);
                        organ.setUser(user);
                        return organ;
                    })
                    .collect(Collectors.toSet());

            user.getUserOrgans().putAll(
                    userOrgans.stream().collect(Collectors.toMap(UserOrgan::getUberonId, organ -> organ))
            );
        }
    }

    private void addGenesAndTaxonDescriptionsToUser(User user, Map<String, TaxonGenesPayload> taxonGenesPayload) {
        if (taxonGenesPayload == null || taxonGenesPayload.isEmpty()) {
            return;
        }

        // Batch-fetch taxons by IDs to reduce individual DB calls
        Set<Integer> taxonIds = taxonGenesPayload.keySet().stream().map(Integer::valueOf).collect(Collectors.toSet());
        Map<Integer, Taxon> taxonMap = taxonService.findByIds(taxonIds);

        taxonGenesPayload.forEach((taxonIdStr, taxonPayload) -> {
            Integer taxonId = Integer.valueOf(taxonIdStr);
            Taxon taxon = taxonMap.get(taxonId);

            if (taxon == null) {
                log.warn("Taxon not found for id: " + taxonId);
                return;
            }

            // Handle taxon description
            String taxonDescription = taxonPayload.getTaxonDescription();
            if (taxonDescription != null && !taxonDescription.trim().isEmpty()) {
                user.getTaxonDescriptions().put(taxon, taxonDescription);
            } else {
                user.getTaxonDescriptions().remove(taxon);
            }

            // Batch-fetch gene info by IDs
            Set<Integer> geneIds = taxonPayload.getGenesToTierMap().keySet().stream().map(Integer::valueOf).collect(Collectors.toSet());
            Map<Integer, GeneInfo> geneInfoMap = geneService.loadByIds(geneIds);

            // Update user genes based on tier and privacy level
            Map<Integer, UserGene> userGenes = updateTermsAndGenesInTaxon(user, taxon, geneInfoMap, taxonPayload);

            // Remove existing genes for this taxon and add updated ones
            user.getUserGenes().entrySet().removeIf(e -> e.getValue().getTaxon().equals(taxon));
            user.getUserGenes().putAll(userGenes);
        });
    }

    private Map<Integer, UserGene> updateTermsAndGenesInTaxon(User user, Taxon taxon, Map<Integer, GeneInfo> geneInfoMap, TaxonGenesPayload taxonPayload) {
        Map<Integer, UserGene> map = new HashMap<>();
        for (Map.Entry<Integer, GeneInfo> integerGeneInfoEntry : geneInfoMap.entrySet()) {
            Integer geneId = integerGeneInfoEntry.getKey();
            GeneInfo geneInfo = integerGeneInfoEntry.getValue();

            UserGene userGene = user.getUserGenes().getOrDefault(geneId, new UserGene());
            userGene.setUser(user);
            userGene.setTaxon(taxon);
            userGene.setTier(taxonPayload.getGenesToTierMap().get(Integer.valueOf(geneId)));
            userGene.setPrivacyLevel(taxonPayload.getGenesToPrivacyLevelMap().getOrDefault(geneId, null));

            UserGene apply = userGene;
            if (map.put(apply.getGeneId(), apply) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return map;
    }



    @Override
    public User update( User user ) {
        if ( applicationSettings.getPrivacy() == null ) {
            // FIXME: this should not be possible...
            log.warn( MessageFormat.format( "{0} attempted to update, but applicationSettings.privacy is null.", user.getEmail() ) );
        } else {
            PrivacyLevelType defaultPrivacyLevel = PrivacyLevelType.values()[applicationSettings.getPrivacy().getDefaultLevel()];
            boolean defaultSharing = applicationSettings.getPrivacy().isDefaultSharing();
            boolean defaultGenelist = applicationSettings.getPrivacy().isAllowHideGenelist();

            if ( user.getProfile().getPrivacyLevel() == null ) {
                log.warn( "Received a null 'privacyLevel' value in profile." );
                user.getProfile().setPrivacyLevel( defaultPrivacyLevel );
            }

            if ( user.getProfile().getShared() == null ) {
                log.warn( "Received a null 'shared' value in profile." );
                user.getProfile().setShared( defaultSharing );
            }

            if ( user.getProfile().getHideGenelist() == null ) {
                if ( applicationSettings.getPrivacy().isAllowHideGenelist() ) {
                    log.warn( "Received a null 'hideGeneList' value in profile." );
                }
                user.getProfile().setHideGenelist( defaultGenelist );
            }
        }

        PrivacyLevelType userPrivacyLevel = user.getProfile().getPrivacyLevel();

        // We cap the user gene privacy level to its new profile setting
        for ( UserGene gene : user.getUserGenes().values() ) {
            PrivacyLevelType genePrivacyLevel = gene.getPrivacyLevel();
            // in case any of the user or gene privacy level is null, we already have a cascading value
            if ( userPrivacyLevel == null || genePrivacyLevel == null ) {
                continue;
            }
            if ( userPrivacyLevel.ordinal() < genePrivacyLevel.ordinal() ) {
                gene.setPrivacyLevel( userPrivacyLevel );
                log.info( MessageFormat.format( "Privacy level of {0} will be capped to {1} (was {2}",
                        gene, userPrivacyLevel, genePrivacyLevel ) );
            }
        }

        return userRepository.save( user );
    }


    @Transactional
    @Override
//    @PreAuthorize("hasPermission(#user, 'update')")
    public User updateProfileWithOrganUberonIdsAndModelOrganism(User  user,
                                                                Profile profile,
                                                                Set<Publication> publications,
                                                                Set<String> organUberonIds,
                                                                Map<String, TaxonGenesPayload> taxonGenesPayload) {


        user.getProfile().setDepartment( profile.getDepartment() );
        user.getProfile().setDescription( profile.getDescription() );
        user.getProfile().setLastName( profile.getLastName() );
        user.getProfile().setName( profile.getName() );
        if ( profile.getResearcherPosition() == null || applicationSettings.getProfile().getEnabledResearcherPositions().contains( profile.getResearcherPosition().name() ) ) {
            user.getProfile().setResearcherPosition( profile.getResearcherPosition() );
        } else {
            log.warn( MessageFormat.format( "User {0} attempted to set user {1} researcher position to an unknown value {2}.",
                     user, user, profile.getResearcherPosition() ) );
            if ( user.getProfile().getPrivacyLevel() != profile.getPrivacyLevel() ) {
                user.getUserGenes().values().forEach( ug -> ug.setPrivacyLevel( profile.getPrivacyLevel() ) );
            }
        }
        user.getProfile().setResearcherPosition( profile.getResearcherPosition() );
        user.getProfile().setOrganization( profile.getOrganization() );

        if ( profile.getResearcherCategories() != null ) {
            Set<String> researcherCategoryNames = profile.getResearcherCategories().stream()
                    .map( ResearcherCategory::name )
                    .collect( Collectors.toSet() );
            if ( applicationSettings.getProfile().getEnabledResearcherCategories().containsAll( researcherCategoryNames ) ) {
                user.getProfile().getResearcherCategories().retainAll( profile.getResearcherCategories() );
                user.getProfile().getResearcherCategories().addAll( profile.getResearcherCategories() );
            } else {
                log.warn( MessageFormat.format( "User {0} attempted to set user {1} researcher type to an unknown value {2}.",
                        user, user, profile.getResearcherCategories() ) );
            }
        }

        if ( user.getProfile().getContactEmail() == null ||
                !user.getProfile().getContactEmail().equals( profile.getContactEmail() ) ) {
            user.getProfile().setContactEmail( profile.getContactEmail() );
            if ( user.getProfile().getContactEmail() != null && !user.getProfile().getContactEmail().isEmpty() ) {
                if ( user.getProfile().getContactEmail().equals( user.getEmail() ) ) {
                    // if the contact email is set to the user email, it's de facto verified
                    user.getProfile().setContactEmailVerified( true );
                } else {
                    user.getProfile().setContactEmailVerified( false );
                    VerificationToken token = userService.createContactEmailVerificationTokenForUser( user );
                    eventPublisher.publishEvent( new OnContactEmailUpdateEvent( user, token ) );
                }
            } else {
                // contact email is unset, so we don't need to send a confirmation
                user.getProfile().setContactEmailVerified( false );
            }
        }

        user.getProfile().setPhone( profile.getPhone() );
        user.getProfile().setWebsite( profile.getWebsite() );

        // privacy settings
        if ( applicationSettings.getPrivacy().isCustomizableLevel() ) {
            // reset gene privacy levels if the profile value is changed
            if ( applicationSettings.getPrivacy().isCustomizableGeneLevel() &&
                    user.getProfile().getPrivacyLevel() != profile.getPrivacyLevel() ) {
                user.getUserGenes().values().forEach( ug -> ug.setPrivacyLevel( profile.getPrivacyLevel() ) );
            }
            user.getProfile().setPrivacyLevel( profile.getPrivacyLevel() );
        }
        if ( applicationSettings.getPrivacy().isCustomizableSharing() ) {
            user.getProfile().setShared( profile.getShared() );
        }
        if ( applicationSettings.getPrivacy().isAllowHideGenelist() ) {
            user.getProfile().setHideGenelist( profile.getHideGenelist() );
        }

//        if ( publications != null ) {
//            user.getProfile().getPublications().retainAll( publications );
//            user.getProfile().getPublications().addAll( publications );
//        }

        if (publications != null) {
            Set<Publication> attachedPublications = publications.stream()
                    .map(publication -> publication.getId() == null ? publication : entityManager.merge(publication))
                    .collect(Collectors.toSet());

            user.getProfile().getPublications().retainAll(attachedPublications);
            user.getProfile().getPublications().addAll(attachedPublications);
        }

        if ( applicationSettings.getOrgans().getEnabled() ) {
            User finalUser = user;
            Map<String, UserOrgan> userOrgans = organInfoService.findByUberonIdIn( organUberonIds ).stream()
                    .map( organInfo -> {

                        log.info(MessageFormat.format( "Returning this Organ :: {0} .",
                                finalUser.getUserOrgans().getOrDefault( organInfo.getUberonId(), UserOrgan.createFromOrganInfo(finalUser, organInfo ))));

                        return finalUser.getUserOrgans().getOrDefault( organInfo.getUberonId(), UserOrgan.createFromOrganInfo(finalUser, organInfo ) );
                    } )
                    .collect( Collectors.toMap( Organ::getUberonId, identity() ) );

            log.info(MessageFormat.format( "userOrgans :: {0} .", userOrgans));

            user.getUserOrgans().clear();
            user.getUserOrgans().putAll( userOrgans );
        }


        user = buildUserGenesFromTaxonPayload(user, taxonGenesPayload);

        log.info(MessageFormat.format( "User with email {0} attempted to update the user profile to : {1}.",
                user.getEmail(), user ) );

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (applicationSettings.getPrivacy() == null) {
            throw new IllegalStateException("Privacy settings must be configured");
        }

//        return userService.update( user );
        return  update(user);
    }


    public User buildUserGenesFromTaxonPayload(User user, Map<String, TaxonGenesPayload> taxonGenesPayload) {
        taxonGenesPayload.forEach((taxonId, payload) -> {
            Taxon taxon = taxonService.findById(Integer.valueOf(taxonId));
            log.debug("genesToTierMap: " + payload.getGenesToTierMap());
            log.debug("genesToPrivacyLevelMap: " + payload.getGenesToPrivacyLevelMap());

            // Handle taxon description updates or removals
            String taxonDescription = payload.getTaxonDescription();

            if (taxonDescription == null || taxonDescription.trim().isEmpty()) {
                // Remove taxon description if null or empty
                log.debug("Removing taxonDescription for taxonId: " + taxonId);
                user.getTaxonDescriptions().remove(taxon);
            } else {
                // Update taxon description if provided
                log.debug("Updating taxonDescription for taxonId: " + taxonId);
                user.getTaxonDescriptions().put(taxon, taxonDescription);
            }

            // If no genes are present, skip gene processing
            if (payload.getGenesToTierMap().isEmpty() && payload.getGenesToPrivacyLevelMap().isEmpty()) {
                return; // Continue to the next taxon if there are no genes
            }

            // Convert genesToTierMap from Map<String, TierType> to Map<GeneInfo, TierType>
            Map<GeneInfo, TierType> genesToTierMap = payload.getGenesToTierMap().keySet().stream()
                    .filter(geneId -> payload.getGenesToTierMap().get(geneId) != null) // strip null values
                    .map(geneId -> geneService.load(Integer.valueOf(geneId)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(identity(), g -> payload.getGenesToTierMap().get(String.valueOf(g.getGeneId()))));

            // Convert genesToPrivacyLevelMap from Map<String, PrivacyLevelType> to Map<GeneInfo, PrivacyLevelType>
            Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap = payload.getGenesToPrivacyLevelMap().keySet().stream()
                    .filter(geneId -> payload.getGenesToPrivacyLevelMap().get(geneId) != null) // strip null values
                    .map(geneId -> geneService.load(Integer.valueOf(geneId)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(identity(), g -> payload.getGenesToPrivacyLevelMap().get(String.valueOf(g.getGeneId()))));

            Map<Integer, UserGene> userGenes = updateTermsAndGenesInTaxon(
                    user,
                    taxon,
                    genesToTierMap,
                    genesToPrivacyLevelMap);

            // Remove existing genes for this taxon
            user.getUserGenes().entrySet()
                    .removeIf(e -> e.getValue().getTaxon().equals(taxon));

            // Add updated genes
            user.getUserGenes().putAll(userGenes);
        });

        return user;
    }





    public Map<Integer, UserGene> updateTermsAndGenesInTaxon(User user,
                                                             Taxon taxon,
                                                             Map<GeneInfo, TierType> genesToTierMap,
                                                             Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap) {

        return genesToTierMap.entrySet().stream()
                .filter(entry -> entry.getKey().getTaxon().equals(taxon))
                .map(entry -> {
                    GeneInfo geneInfo = entry.getKey();
                    TierType tierType = entry.getValue();

                    UserGene userGene = user.getUserGenes().getOrDefault(geneInfo.getGeneId(), new UserGene());
                    userGene.setUser(user);
                    userGene.setTier(tierType);

                    if (applicationSettings.getPrivacy().isCustomizableGeneLevel()) {
                        // If no privacy level is set, inherit the profile value
                        userGene.setPrivacyLevel(genesToPrivacyLevelMap.getOrDefault(geneInfo, null));
                    }

                    userGene.updateGene(geneInfo);
                    return userGene;
                })
                .collect(Collectors.toMap(UserGene::getGeneId, identity()));
    }





    @Transactional
    @Override
    public User updateUserBasicProfile(User user, Profile profile) {
        user.getProfile().setName(profile.getName());
        user.getProfile().setLastName(profile.getLastName());
        user.getProfile().setDepartment(profile.getDepartment());
        user.getProfile().setPhone(profile.getPhone());
        user.getProfile().setWebsite(profile.getWebsite());
        user.getProfile().setDescription(profile.getDescription());

        updateUserContactEmail(user, profile);

        return user;  // Return the updated user object
    }

    @Transactional
    @Override
    public User updateUserPublications(User user, Set<Publication> publications) {
//        if (publications != null) {
//            user.getProfile().getPublications().retainAll(publications);
//            user.getProfile().getPublications().addAll(publications);
//        }
        if (publications != null) {
            Set<Publication> attachedPublications = publications.stream()
                    .map(publication -> publication.getId() == null ? publication : entityManager.merge(publication))
                    .collect(Collectors.toSet());

            user.getProfile().getPublications().retainAll(attachedPublications);
            user.getProfile().getPublications().addAll(attachedPublications);
        }
        return user;  // Return the updated user object
    }

    @Transactional
    @Override
    public User updateUserOrgansAndResearcherCategories(User user, Set<String> organUberonIds, Profile profile) {
        if (applicationSettings.getOrgans().getEnabled()) {
            Map<String, UserOrgan> userOrgans = organInfoService.findByUberonIdIn(organUberonIds).stream()
                    .map(organInfo -> user.getUserOrgans().getOrDefault(organInfo.getUberonId(), UserOrgan.createFromOrganInfo(user, organInfo)))
                    .collect(Collectors.toMap(Organ::getUberonId, identity()));
            user.getUserOrgans().clear();
            user.getUserOrgans().putAll(userOrgans);
        }

        if (profile.getResearcherCategories() != null) {
            Set<String> researcherCategoryNames = profile.getResearcherCategories().stream()
                    .map(ResearcherCategory::name)
                    .collect(Collectors.toSet());
            if (applicationSettings.getProfile().getEnabledResearcherCategories().containsAll(researcherCategoryNames)) {
                user.getProfile().getResearcherCategories().retainAll(profile.getResearcherCategories());
                user.getProfile().getResearcherCategories().addAll(profile.getResearcherCategories());
            }
        }

        return user;  // Return the updated user object
    }

    @Transactional
    @Override
    public User updateUserGenesAndTaxonDescriptions(User user, Map<String, TaxonGenesPayload> taxonGenesPayload) {
        buildUserGenesFromTaxonPayload(user, taxonGenesPayload);
        return user;  // Return the updated user object
    }

    @Transactional
    @Override
    public User save(User user) {
        return userRepository.save(user);  // Save and return the updated user object
    }

    // Private helper method for contact email updates
    private void updateUserContactEmail(User user, Profile profile) {
        if (!Objects.equals(user.getProfile().getContactEmail(), profile.getContactEmail())) {
            user.getProfile().setContactEmail(profile.getContactEmail());
            if (profile.getContactEmail() != null && !profile.getContactEmail().isEmpty()) {
                if (profile.getContactEmail().equals(user.getEmail())) {
                    user.getProfile().setContactEmailVerified(true);
                } else {
                    user.getProfile().setContactEmailVerified(false);
                    VerificationToken token = userService.createContactEmailVerificationTokenForUser(user);
                    eventPublisher.publishEvent(new OnContactEmailUpdateEvent(user, token));
                }
            } else {
                user.getProfile().setContactEmailVerified(false);
            }
        }
    }

}

