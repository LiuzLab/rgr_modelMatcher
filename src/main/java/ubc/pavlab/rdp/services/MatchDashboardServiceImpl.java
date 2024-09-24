package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.events.OnContactEmailUpdateEvent;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.text.MessageFormat;
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

        if ( publications != null ) {
            user.getProfile().getPublications().retainAll( publications );
            user.getProfile().getPublications().addAll( publications );
        }

        if ( applicationSettings.getOrgans().getEnabled() ) {
            User finalUser = user;
            Map<String, UserOrgan> userOrgans = organInfoService.findByUberonIdIn( organUberonIds ).stream()
                    .map( organInfo -> finalUser.getUserOrgans().getOrDefault( organInfo.getUberonId(), UserOrgan.createFromOrganInfo(finalUser, organInfo ) ) )
                    .collect( Collectors.toMap( Organ::getUberonId, identity() ) );
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
            //Updating taxon description.

            user.getTaxonDescriptions().put( taxon, payload.getTaxonDescription() );

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


}

