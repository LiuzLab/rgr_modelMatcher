package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import ubc.pavlab.rdp.events.OnRequestAccessEvent;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.services.OrganInfoService;
//import ubc.pavlab.rdp.util.MetricsUpdater;

import javax.validation.Valid;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static ubc.pavlab.rdp.constants.MetricsConstants.SEARCH_API_REQUEST_COUNT;
import static ubc.pavlab.rdp.constants.MetricsConstants.SEARCH_API_RESPONSE_TIME;

/**
 * Created by mjacobson on 05/02/18.
 */
@Controller
@CommonsLog
public class SearchController {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    private UserOrganService userOrganService;

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @Autowired
    private PrivacyService privacyService;

    @Autowired
    private GeneInfoService geneInfoService;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

//    @Autowired
//    private MetricsUpdater metricsUpdater;

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search")
    public ModelAndView search() {
        ModelAndView modelAndView = new ModelAndView( "search" );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "user", userService.findCurrentUser() );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "nameLike" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike,
                                           @RequestParam Boolean prefix,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "search" );
        Collection<User> users;
        if ( prefix ) {
            users = userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            users = userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }
        modelAndView.addObject( "nameLike", nameLike );
        modelAndView.addObject( "organUberonIds", organUberonIds );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "user", userService.findCurrentUser() );
        modelAndView.addObject( "users", users );
        modelAndView.addObject( "iSearch", iSearch );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds ) );
        }
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "nameLike" })
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike,
                                               @RequestParam Boolean prefix,
                                               @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                               @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                               @RequestParam(required = false) Set<String> organUberonIds ) {
        Collection<User> users;
        if ( prefix ) {
            users = userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            users = userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", users );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike,
                                                  @RequestParam Boolean prefix,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds ) );
        modelAndView.addObject( "remote", true );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
                                                  @RequestParam Boolean iSearch,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "search" );
        modelAndView.addObject( "descriptionLike", descriptionLike );
        modelAndView.addObject( "organUberonIds", organUberonIds );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "iSearch", iSearch );

        modelAndView.addObject( "user", userService.findCurrentUser() );

        modelAndView.addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByDescription( descriptionLike, researcherPositions, researcherCategories, organUberonIds ) );
        }
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike,
                                                      @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                      @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                      @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike,
                                                         @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                         @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                         @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", remoteResourceService.findUsersByDescription( descriptionLike, researcherPositions, researcherCategories, organUberonIds ) );
        modelAndView.addObject( "remote", true );
        return modelAndView;
    }


//    1
    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search", params = { "symbol", "taxonId" })
    public ModelAndView searchUsersByGene( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           Locale locale ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        ModelAndView modelAndView = new ModelAndView( "search" );

        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "symbol", symbol );
        modelAndView.addObject( "taxonId", taxonId );
        modelAndView.addObject( "orthologTaxonId", orthologTaxonId );
        modelAndView.addObject( "tiers", tiers );
        modelAndView.addObject( "organUberonIds", organUberonIds );
        modelAndView.addObject( "iSearch", iSearch );

//        log.info(MessageFormat.format(" USER Permission :: \n char : {0} \n symbol : {0} \n taxonId : {0} \n orthologTaxonId : {0} \n tiers  : {0} \n organUberonIds : {0} \n iSearch : {0} ",
//                userService.getLastNamesFirstChar(), symbol, taxonId, orthologTaxonId, tiers, organUberonIds, iSearch ));

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoTaxonId", new String[]{ taxonId.toString() }, locale ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }

        GeneInfo gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) ) &&
                        // Check if we got some ortholog results
                        ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }
        modelAndView.addObject("gene", gene);
        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
//        log.info(MessageFormat.format(" USER Permission :: \n usergenes : {0} ", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ))));
        if ( iSearch ) {
            modelAndView.addObject( "itlUsergenes", remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds ) );
        }

        return modelAndView;
    }

//    Search 1
    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view")
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol,
                                               @RequestParam Integer taxonId,
                                               @RequestParam(required = false) Set<TierType> tiers,
                                               @RequestParam(required = false) Integer orthologTaxonId,
                                               @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                               @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                               @RequestParam(required = false) Set<String> organUberonIds,
                                               Locale locale ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::usergenes-table" );

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        GeneInfo gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if ( orthologTaxonId != null && orthologTaxon == null ) {
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoOrthologTaxonId", new String[]{ orthologTaxonId.toString() }, locale ) );
            return modelAndView;
        }

        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            return modelAndView;
        }
        modelAndView.addObject("gene", gene);
        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );

        return modelAndView;
    }
//1
    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view/orthologs")
    public ModelAndView searchOrthologsForGene( @RequestParam String symbol,
                                                @RequestParam Integer taxonId,
                                                @RequestParam(required = false) Set<TierType> tiers,
                                                @RequestParam(required = false) Integer orthologTaxonId,
                                                @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                @RequestParam(required = false) Set<String> organUberonIds,
                                                Locale locale ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/ortholog-table::ortholog-table" );


        long startTime = System.nanoTime();
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );
//        GeneInfo gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Map<Taxon, Set<GeneInfo>> orthologMap = orthologs.stream()
                .collect( Collectors.groupingBy( GeneInfo::getTaxon, Collectors.toSet() ) );
        log.info( MessageFormat.format( "< Search Results orthologMap :: {0} >\n", orthologMap ));
        modelAndView.addObject("gene", gene);
        modelAndView.addObject( "orthologs", orthologMap );


        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;
//        metricsUpdater.updateGauge(SEARCH_API_RESPONSE_TIME, ((double)durationInMillis));
        log.info( MessageFormat.format( "< Search Response time :: {0} ms >\n", durationInMillis ));
//        metricsUpdater.incrementCounter(SEARCH_API_REQUEST_COUNT);
        return modelAndView;
    }
//2
    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "symbol", "taxonId", "orthologTaxonId" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol,
                                                  @RequestParam Integer taxonId,
                                                  @RequestParam(required = false) Set<TierType> tiers,
                                                  @RequestParam(required = false) Integer orthologTaxonId,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Collection<UserGene> userGenes = remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds );

        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::usergenes-table" );
        modelAndView.addObject( "usergenes", userGenes );
        modelAndView.addObject( "remote", true );

        return modelAndView;
    }

    @GetMapping(value = "/search/view/user-preview/{userId}")
    public ModelAndView previewUser( @PathVariable Integer userId,
                                     @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/profile::user-preview" );
        modelAndView.addObject( "user", userService.findUserById( userId ) );
        return modelAndView;
    }

    @GetMapping(value = "/search/view/user-preview/by-anonymous-id/{anonymousId}")
    public ModelAndView previewUser( @PathVariable UUID anonymousId,
                                     @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/profile::user-preview" );
        User user = userService.findUserByAnonymousId( anonymousId );
        if ( user == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
        }
        modelAndView.addObject( "user", userService.anonymizeUser( user ) );
        return modelAndView;
    }

    @GetMapping(value = "/userView/{userId}")
    public ModelAndView viewUser( @PathVariable Integer userId,
                                  @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "userView" );
        User user = userService.findCurrentUser();

        log.info( MessageFormat.format( "Searching for USER :: {0}", user ));
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() && privacyService.checkCurrentUserCanSearch( true ) ) {
            try {
                viewUser = remoteResourceService.getRemoteUser( userId, URI.create( remoteHost ) );
            } catch ( RemoteException e ) {
                log.error( MessageFormat.format( "Could not fetch the remote user id {0} from {1}.", userId, remoteHost ), e );
                modelAndView.setStatus( HttpStatus.SERVICE_UNAVAILABLE );
                modelAndView.setViewName( "error/503" );
                return modelAndView;
            }
        } else {
            viewUser = userService.findUserById( userId );

        }

        if ( viewUser == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
        } else {

//            log.info( MessageFormat.format( "Passing this information ::" +
//                    " \n< USER :: {0} > \n< viewUser :: {1} > \n< EffectivePrivacyLevel :: {2} HIDE LISr :{3} >", user, viewUser, viewUser.getEffectivePrivacyLevel(), viewUser.getProfile().getHideGenelist() ));
//            Set<Taxon> userFirstTaxons = viewUser.getTaxons();
//            int count = 0;
//            for (Taxon userFirstTaxon : userFirstTaxons) {
//                log.info( MessageFormat.format( " user {0}  Taxon {1}  :: \n ", viewUser.getEmail(), count));
//                Set<UserGene> genesList = viewUser.getGenesByTaxon(userFirstTaxon);
//                int x = 0  ;
//                int finalCount = count;
//                genesList.forEach((gene) -> {
//                    log.info( MessageFormat.format( " USER GENE {0} - {1} :: {2} \n ", finalCount, x, gene));
//                });
//                count++;
//            }


            modelAndView.addObject( "user", user );
            modelAndView.addObject( "viewUser", viewUser );
            modelAndView.addObject( "viewOnly", true );
        }
        return modelAndView;
    }

    @Data
    private static class RequestAccessForm {
        @NotBlank(message = "Reason cannot be blank.")
        private String reason;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @GetMapping("/search/gene/by-anonymous-id/{anonymousId}/request-access")
    public Object requestGeneAccessView( @PathVariable UUID anonymousId,
                                         RedirectAttributes redirectAttributes ) {
        ModelAndView modelAndView = new ModelAndView( "search/request-access" );
        UserGene userGene = userService.findUserGeneByAnonymousId( anonymousId );
        if ( userGene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
            return modelAndView;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( permissionEvaluator.hasPermission( auth, userGene, "read" ) ) {
            String redirectUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path( "userView/{userId}" )
                    .buildAndExpand( Collections.singletonMap( "userId", userGene.getUser().getId() ) )
                    .toUriString();
            redirectAttributes.addFlashAttribute( "message", "There is no need to request access as you have sufficient permission to see this gene." );
            return new RedirectView( redirectUri );
        }
        modelAndView.addObject( "requestAccessForm", new RequestAccessForm() );
        modelAndView.addObject( "userGene", userService.anonymizeUserGene( userGene ) );
        return modelAndView;
    }

    @Transactional
    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @PostMapping("/search/gene/by-anonymous-id/{anonymousId}/request-access")
    public ModelAndView requestGeneAccess( @PathVariable UUID anonymousId,
                                           @Valid RequestAccessForm requestAccessForm,
                                           BindingResult bindingResult,
                                           RedirectAttributes redirectAttributes ) {
        ModelAndView modelAndView = new ModelAndView( "search/request-access" );
        UserGene userGene = userService.findUserGeneByAnonymousId( anonymousId );
        if ( userGene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
            return modelAndView;
        }

        modelAndView.addObject( "userGene", userService.anonymizeUserGene( userGene ) );
//        log.info(MessageFormat.format(" USER Access userGene :: {0}", userService.anonymizeUserGene( userGene )));

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
        } else {
            eventPublisher.publishEvent( new OnRequestAccessEvent( userService.findCurrentUser(), userGene, requestAccessForm.reason ) );
            redirectAttributes.addFlashAttribute( "message", "An access request has been sent and will be reviewed." );
            return new ModelAndView( "redirect:/search" );
        }
        return modelAndView;
    }

//    private Collection<UserOrgan> organsFromUberonIds( Set<String> organUberonIds ) {
//        return organUberonIds == null ? null : userOrganService.findByUberonIdIn( organUberonIds );
//    }

        private Collection<OrganInfo> organsFromUberonIds(Set<String> organUberonIds) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn(organUberonIds);
    }

}






//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
//
//
////TODO: uabckjdab
////package ubc.pavlab.rdp.controllers;
//
//import java.net.URI;
//import java.text.MessageFormat;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Locale;
//import java.util.Set;
//import java.util.UUID;
//import java.util.stream.Collectors;
//import javax.validation.Valid;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.hibernate.validator.constraints.NotBlank;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.MessageSource;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.access.PermissionEvaluator;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.servlet.ModelAndView;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//import org.springframework.web.servlet.view.RedirectView;
//import ubc.pavlab.rdp.exception.RemoteException;
//import ubc.pavlab.rdp.model.*;
//import ubc.pavlab.rdp.model.enums.ResearcherCategory;
//import ubc.pavlab.rdp.model.enums.ResearcherPosition;
//import ubc.pavlab.rdp.model.enums.TierType;
//import ubc.pavlab.rdp.services.GeneInfoService;
//import ubc.pavlab.rdp.services.OrganInfoService;
//import ubc.pavlab.rdp.services.RemoteResourceService;
//import ubc.pavlab.rdp.services.TaxonService;
//import ubc.pavlab.rdp.services.UserGeneService;
//import ubc.pavlab.rdp.services.UserService;
//
///**
// * This is a recovered class after decompressing, Please change the format as you work on this file.
// * */
//
//@Controller
//public class SearchController {
//    private static final Log log = LogFactory.getLog(SearchController.class);
//    @Autowired
//    private MessageSource messageSource;
//    @Autowired
//    private UserService userService;
//    @Autowired
//    private TaxonService taxonService;
//    @Autowired
//    private UserGeneService userGeneService;
//    @Autowired
//    private OrganInfoService organInfoService;
//    @Autowired
//    private RemoteResourceService remoteResourceService;
//    @Autowired
//    private GeneInfoService geneInfoService;
//    @Autowired
//    private PermissionEvaluator permissionEvaluator;
//
//    public SearchController() {
//    }
//
//    @PreAuthorize("hasPermission(null, 'search')")
//    @GetMapping({"/search"})
//    public ModelAndView search() {
//        ModelAndView modelAndView = new ModelAndView("search");
//        modelAndView.addObject("chars", this.userService.getLastNamesFirstChar());
//        modelAndView.addObject("user", this.userService.findCurrentUser());
//        return modelAndView;
//    }
//
//    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
//    @GetMapping(
//            value = {"/search"},
//            params = {"nameLike"}
//    )
//    public ModelAndView searchUsersByName(@RequestParam String nameLike, @RequestParam Boolean prefix, @RequestParam Boolean iSearch,
//                                          @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
//                                          @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
//                                          @RequestParam(required = false) Set<String> organUberonIds) {
//        ModelAndView modelAndView = new ModelAndView("search");
//        Collection<User> users;
//        if (prefix) {
//            users = userService.findByStartsName(nameLike, researcherPositions, researcherCategories, organsFromUberonIds(organUberonIds));
//        } else {
//            users = this.userService.findByLikeName(nameLike, researcherPositions, researcherCategories, organsFromUberonIds(organUberonIds));
//        }
//
//        modelAndView.addObject("nameLike", nameLike);
//        modelAndView.addObject("organUberonIds", organUberonIds);
//        modelAndView.addObject("chars", this.userService.getLastNamesFirstChar());
//        modelAndView.addObject("user", this.userService.findCurrentUser());
//        modelAndView.addObject("users", users);
//        modelAndView.addObject("iSearch", iSearch);
//        if (iSearch) {
//            modelAndView.addObject("itlUsers", this.remoteResourceService.findUsersByLikeName(nameLike, prefix, researcherPositions, researcherCategories, organUberonIds));
//        }
//
//        return modelAndView;
//    }
//
//    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
//    @GetMapping(
//            value = {"/search"},
//            params = {"descriptionLike"}
//    )
//    public ModelAndView searchUsersByDescription(@RequestParam String descriptionLike, @RequestParam Boolean iSearch,
//                                                 @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
//                                                 @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
//                                                 @RequestParam(required = false) Set<String> organUberonIds) {
//        ModelAndView modelAndView = new ModelAndView("search");
//        modelAndView.addObject("descriptionLike", descriptionLike);
//        modelAndView.addObject("organUberonIds", organUberonIds);
//        modelAndView.addObject("chars", this.userService.getLastNamesFirstChar());
//        modelAndView.addObject("iSearch", iSearch);
//        modelAndView.addObject("user", this.userService.findCurrentUser());
//        modelAndView.addObject("users", this.userService.findByDescription(descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds(organUberonIds)));
//        if (iSearch) {
//            modelAndView.addObject("itlUsers", this.remoteResourceService.findUsersByDescription(descriptionLike, researcherPositions, researcherCategories, organUberonIds));
//        }
//
//        return modelAndView;
//    }
//
//    @PreAuthorize("hasPermission(null, 'search')")
//    @GetMapping(
//            value = {"/search"},
//            params = {"symbol", "taxonId"}
//    )
//    public ModelAndView searchUsersByGene(@RequestParam String symbol, @RequestParam Integer taxonId, @RequestParam Boolean iSearch, @RequestParam(required = false) Set<TierType> tiers, @RequestParam(required = false) Integer orthologTaxonId, @RequestParam(required = false) Set<ResearcherPosition> researcherPositions, @RequestParam(required = false) Set<ResearcherCategory> researcherCategories, @RequestParam(required = false) Set<String> organUberonIds, Locale locale) {
//        if (taxonId != 9606) {
//            orthologTaxonId = null;
//        }
//
//        if (tiers == null) {
//            tiers = TierType.ANY;
//        }
//
//        ModelAndView modelAndView = new ModelAndView("search");
//        modelAndView.addObject("chars", this.userService.getLastNamesFirstChar());
//        modelAndView.addObject("symbol", symbol);
//        modelAndView.addObject("taxonId", taxonId);
//        modelAndView.addObject("orthologTaxonId", orthologTaxonId);
//        modelAndView.addObject("tiers", tiers);
//        modelAndView.addObject("organUberonIds", organUberonIds);
//        modelAndView.addObject("iSearch", iSearch);
//
//        log.info(MessageFormat.format(" USER Permission :: \n char : {0} \n symbol : {0} \n taxonId : {0} \n orthologTaxonId : {0} \n tiers  : {0} \n organUberonIds : {0} \n iSearch : {0} ",
//                userService.getLastNamesFirstChar(), symbol, taxonId, orthologTaxonId, tiers, organUberonIds, iSearch ));
//        Taxon taxon = this.taxonService.findById(taxonId);
//        if (taxon == null) {
//            modelAndView.setStatus(HttpStatus.NOT_FOUND);
//            modelAndView.addObject("message", this.messageSource.getMessage("SearchController.errorNoTaxonId", new String[]{taxonId.toString()}, locale));
//            modelAndView.addObject("error", Boolean.TRUE);
//            return modelAndView;
//        } else {
//            GeneInfo gene = this.geneInfoService.findBySymbolAndTaxon(symbol, taxon);
//            if (gene == null) {
//                modelAndView.setStatus(HttpStatus.NOT_FOUND);
//                modelAndView.addObject("message", this.messageSource.getMessage("SearchController.errorNoGene", new String[]{symbol, taxon.getScientificName()}, locale));
//                modelAndView.addObject("error", Boolean.TRUE);
//                return modelAndView;
//            } else {
//                Taxon orthologTaxon = orthologTaxonId == null ? null : this.taxonService.findById(orthologTaxonId);
//                Collection<GeneInfo> orthologs = (Collection)gene.getOrthologs().stream().filter((g) -> {
//                    return orthologTaxon == null || g.getTaxon().equals(orthologTaxon);
//                }).collect(Collectors.toSet());
//                if (orthologTaxon != null && !orthologTaxon.equals(gene.getTaxon()) && orthologs.isEmpty()) {
//                    modelAndView.setStatus(HttpStatus.NOT_FOUND);
//                    modelAndView.addObject("message", this.messageSource.getMessage("SearchController.errorNoOrthologs", new String[]{symbol, orthologTaxon.getScientificName()}, locale));
//                    modelAndView.addObject("error", Boolean.TRUE);
//                    return modelAndView;
//                } else {
//                    modelAndView.addObject("gene", gene);
//                    modelAndView.addObject("usergenes", this.userGeneService.handleGeneSearch(gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds(organUberonIds)));
//                    log.info(MessageFormat.format(" USER Permission :: \n usergenes : {0} ", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ))));
//
//                    if (iSearch) {
//                        modelAndView.addObject("itlUsergenes", this.remoteResourceService.findGenesBySymbol(symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds));
//                    }
//
//                    return modelAndView;
//                }
//            }
//        }
//    }
//
//    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
//    @GetMapping({"/userView/{userId}"})
//    public ModelAndView viewUser(@PathVariable Integer userId, @RequestParam(required = false) String remoteHost) {
//        ModelAndView modelAndView = new ModelAndView("userView");
//        User user = this.userService.findCurrentUser();
//        User viewUser;
//        if (remoteHost != null && !remoteHost.isEmpty()) {
//            URI remoteHostUri = URI.create(remoteHost);
//
//            try {
//                viewUser = this.remoteResourceService.getRemoteUser(userId, remoteHostUri);
//            } catch (RemoteException var8) {
//                RemoteException e = var8;
//                log.error(MessageFormat.format("Could not fetch the remote user id {0} from {1}.", userId, remoteHostUri.getAuthority()), e);
//                modelAndView.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
//                modelAndView.setViewName("error/503");
//                return modelAndView;
//            }
//        } else {
//            viewUser = this.userService.findUserById(userId);
//        }
//
//        if (viewUser == null) {
//            modelAndView.setStatus(HttpStatus.NOT_FOUND);
//            modelAndView.setViewName("error/404");
//        } else {
//
//                        log.info( MessageFormat.format( "Passing this information ::" +
//                    " \n< USER :: {0} > \n< viewUser :: {1} > \n< EffectivePrivacyLevel :: {2} >", user, viewUser, viewUser.getEffectivePrivacyLevel()));
//            Set<Taxon> userFirstTaxons = viewUser.getTaxons();
//            int count = 0;
//            for (Taxon userFirstTaxon : userFirstTaxons) {
//                log.info( MessageFormat.format( " user {0}  Taxon {1} :: \n ", viewUser.getEmail(), count));
//                Set<UserGene> genesList = viewUser.getGenesByTaxon(userFirstTaxon);
//                int x = 0  ;
//                int finalCount = count;
//                genesList.forEach((gene) -> {
//                    log.info( MessageFormat.format( " USER GENE {0} - {1} :: {2} \n ", finalCount, x, gene));
//                });
//                count++;
//            }
////            log.info("Passing this information :: \nUSER ::" + user + "\n viewUser ::" + viewUser);
//
//            modelAndView.addObject("user", user);
//            modelAndView.addObject("viewUser", viewUser);
//            modelAndView.addObject("viewOnly", Boolean.TRUE);
//
//
//        }
//
//        return modelAndView;
//    }
//
//    @PreAuthorize("hasPermission(null, 'search') and hasAnyRole('USER', 'ADMIN')")
//    @GetMapping({"/search/gene/by-anonymous-id/{anonymousId}/request-access"})
//    public Object requestGeneAccessView(@PathVariable UUID anonymousId, RedirectAttributes redirectAttributes) {
//        ModelAndView modelAndView = new ModelAndView("search/request-access");
//        UserGene userGene = this.userService.findUserGeneByAnonymousIdNoAuth(anonymousId);
//        if (userGene == null) {
//            modelAndView.setStatus(HttpStatus.NOT_FOUND);
//            modelAndView.setViewName("error/404");
//            return modelAndView;
//        } else {
//            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//            if (this.permissionEvaluator.hasPermission(auth, userGene, "read")) {
//                String redirectUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("userView/{userId}").buildAndExpand(Collections.singletonMap("userId", userGene.getUser().getId())).toUriString();
//                redirectAttributes.addFlashAttribute("message", "There is no need to request access as you have sufficient permission to see this gene.");
//                return new RedirectView(redirectUri);
//            } else {
//                modelAndView.addObject("requestAccessForm", new RequestAccessForm());
//                modelAndView.addObject("userGene", this.userService.anonymizeUserGene(userGene));
//                return modelAndView;
//            }
//        }
//    }
//
//    @PreAuthorize("hasPermission(null, 'search') and hasAnyRole('USER', 'ADMIN')")
//    @PostMapping({"/search/gene/by-anonymous-id/{anonymousId}/request-access"})
//    public ModelAndView requestGeneAccess(@PathVariable UUID anonymousId, @Valid RequestAccessForm requestAccessForm, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
//        ModelAndView modelAndView = new ModelAndView("search/request-access");
//        UserGene userGene = this.userService.findUserGeneByAnonymousIdNoAuth(anonymousId);
//        if (userGene == null) {
//            modelAndView.setStatus(HttpStatus.NOT_FOUND);
//            modelAndView.setViewName("error/404");
//            return modelAndView;
//        } else {
//            modelAndView.addObject("userGene", this.userService.anonymizeUserGene(userGene));
//            if (bindingResult.hasErrors()) {
//                modelAndView.setStatus(HttpStatus.BAD_REQUEST);
//                return modelAndView;
//            } else {
//                this.userService.sendGeneAccessRequest(this.userService.findCurrentUser(), userGene, requestAccessForm.getReason());
//                redirectAttributes.addFlashAttribute("message", "An access request has been sent and will be reviewed.");
//                return new ModelAndView("redirect:/search");
//            }
//        }
//    }
//
//    private Collection<OrganInfo> organsFromUberonIds(Set<String> organUberonIds) {
//        return organUberonIds == null ? null : organInfoService.findByUberonIdIn(organUberonIds);
//    }
//
//    private static class RequestAccessForm {
//        @NotBlank(
//                message = "Reason cannot be blank."
//        )
//        private String reason;
//
//        public RequestAccessForm() {
//        }
//
//        public String getReason() {
//            return this.reason;
//        }
//
//        public void setReason(String reason) {
//            this.reason = reason;
//        }
//
//        public boolean equals(Object o) {
//            if (o == this) {
//                return true;
//            } else if (!(o instanceof RequestAccessForm)) {
//                return false;
//            } else {
//                RequestAccessForm other = (RequestAccessForm)o;
//                if (!other.canEqual(this)) {
//                    return false;
//                } else {
//                    Object this$reason = this.getReason();
//                    Object other$reason = other.getReason();
//                    if (this$reason == null) {
//                        if (other$reason != null) {
//                            return false;
//                        }
//                    } else if (!this$reason.equals(other$reason)) {
//                        return false;
//                    }
//
//                    return true;
//                }
//            }
//        }
//
//        protected boolean canEqual(Object other) {
//            return other instanceof RequestAccessForm;
//        }
//
//        public int hashCode() {
//            boolean PRIME = true;
//            int result = 1;
//            Object $reason = this.getReason();
//            result = result * 59 + ($reason == null ? 43 : $reason.hashCode());
//            return result;
//        }
//
//        public String toString() {
//            return "SearchController.RequestAccessForm(reason=" + this.getReason() + ")";
//        }
//    }
//}
