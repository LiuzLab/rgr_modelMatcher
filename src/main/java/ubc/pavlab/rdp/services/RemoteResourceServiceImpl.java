package ubc.pavlab.rdp.services;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.VersionUtils;

import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Service("RemoteResourceService")
@CommonsLog
@PreAuthorize("hasPermission(null, 'international-search')")
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_URI = "/api";
    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_USER_GET_URI = "/api/users/{userId}";
    private static final String API_USER_GET_BY_ANONYMOUS_ID_URI = "/api/users/by-anonymous-id/{anonymousId}";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @Autowired
    private TaxonService taxonService;

    @Override
    @Cacheable(value = "ubc.pavlab.rdp.services.RemoteResourceService.apiVersionByRemoteHostAuthority", key = "#remoteHost.authority")
    public String getApiVersion( URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        URI authority = getApiUri( remoteHost );
        URI uri = UriComponentsBuilder.fromUri( authority )
                .path( API_URI )
                .build()
                .toUri();
        OpenAPI openAPI = getFromRequestFuture( remoteHost, asyncRestTemplate.getForEntity( uri, OpenAPI.class ) ).getBody();
        // The OpenAPI specification was introduced in 1.4, so we assume 1.0.0 for previous versions
        if ( openAPI.getInfo() == null ) {
            return "1.0.0";
        } else if ( openAPI.getInfo().getVersion().equals( "v0" ) ) {
            return "1.4.0"; // the version number was missing in early 1.4
        } else {
            return openAPI.getInfo().getVersion();
        }
    }

    @Override
    public List<User> findUsersByLikeName( String nameLike, Boolean prefix, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "nameLike", nameLike );
        params.add( "prefix", prefix.toString() );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .ontologyTermInfos( ontologyTermInfos )
                .build().toMultiValueMap() );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params, "1.0.0" ).stream()
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    public List<User> findUsersByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "descriptionLike", descriptionLike );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .ontologyTermInfos( ontologyTermInfos )
                .build().toMultiValueMap() );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params, "1.0.0" ).stream()
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    public List<User> findUsersByLikeNameAndDescription( String nameLike, boolean prefix, String descriptionLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "nameLike", nameLike );
        params.add( "prefix", String.valueOf( prefix ) );
        params.add( "descriptionLike", descriptionLike );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .ontologyTermInfos( ontologyTermInfos )
                .build().toMultiValueMap() );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params, "1.5.0" ).stream()
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    public List<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tiers, Integer orthologTaxonId, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos ) {
        List<UserGene> intlUsergenes = new LinkedList<>();
        for ( TierType tier : restrictTiers( tiers ) ) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add( "symbol", symbol );
            params.putAll( UserGeneSearchParams.builder()
                    .taxonId( taxon.getId() )
                    .tier( tier )
                    .orthologTaxonId( orthologTaxonId )
                    .researcherPositions( researcherPositions )
                    .researcherCategories( researcherCategories )
                    .organUberonIds( organUberonIds )
                    .ontologyTermInfos( ontologyTermInfos )
                    .build().toMultiValueMap() );
            intlUsergenes.addAll( getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, params, "1.0.0" ) );
        }
        Map<Integer, Integer> taxonOrderingById = taxonService.findByActiveTrue().stream()
                .collect( Collectors.toMap( Taxon::getId, Taxon::getOrdering ) );
        for ( UserGene g : intlUsergenes ) {
            // add back-reference to user
            g.setUser( g.getRemoteUser() );
            // populate taxon ordering
            g.getTaxon().setOrdering( taxonOrderingById.getOrDefault( g.getTaxon().getId(), null ) );
        }
        // sort results from different sources
        return intlUsergenes.stream()
                .sorted( UserGene.getComparator() )
                .collect( Collectors.toList() ); // we need to preserve the search order
    }

    @Override
    public User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        URI authority = getApiUri( remoteHost );

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        addAuthParamIfAdmin( authority, queryParams );
        URI uri = UriComponentsBuilder.fromUri( authority )
                .path( API_USER_GET_URI )
                .replaceQueryParams( queryParams )
                .buildAndExpand( Collections.singletonMap( "userId", userId ) )
                .toUri();

        return getUserByUri( uri );
    }

    @Override
    public User getAnonymizedUser( UUID anonymousId, URI remoteHost ) throws RemoteException {
        URI authority = getApiUri( remoteHost );

        if ( !VersionUtils.satisfiesVersion( getApiVersion( remoteHost ), "1.4.0" ) ) {
            log.info( MessageFormat.format( "{0} does not support retrieving user by anonymous identifier, will return null instead.", remoteHost ) );
            return null;
        }

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        addAuthParamIfAdmin( authority, queryParams );
        URI uri = UriComponentsBuilder.fromUri( authority )
                .path( API_USER_GET_BY_ANONYMOUS_ID_URI )
                .replaceQueryParams( queryParams )
                .buildAndExpand( Collections.singletonMap( "anonymousId", anonymousId ) )
                .toUri();

        return getUserByUri( uri );
    }

    private User getUserByUri( URI uri ) throws RemoteException {
        ResponseEntity<User> responseEntity = getFromRequestFuture( uri, asyncRestTemplate.getForEntity( uri, User.class ) );
        User user = responseEntity.getBody();
        initUser( user );
        return user;
    }

    /**
     * Retrieve entities from all registered partner APIs.
     *
     * @param arrCls         the type of entities to retrieve as a {@link Class}
     * @param path           the API endpoint to query
     * @param params         the parameters for the endpoint
     * @param minimumVersion minimum version requirement as per {@link VersionUtils#satisfiesVersion(String, String)}
     * @param <T>            the type of entities to retrieve
     * @return the entities from all registered partner APIs
     */
    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String path, MultiValueMap<String, String> params, String minimumVersion ) {
        return Arrays.stream( applicationSettings.getIsearch().getApis() )
                .map( URI::create )
                .filter( remoteHost -> {
                    try {
                        return VersionUtils.satisfiesVersion( getApiVersion( remoteHost ), minimumVersion );
                    } catch ( RemoteException e ) {
                        log.warn( String.format( "Failed to retrieve API version from %s.", remoteHost ), e );
                        return false;
                    }
                } )
                .map( api -> {
                    // work on a copy because we'll be selectively adding auth information
                    LinkedMultiValueMap<String, String> apiParams = new LinkedMultiValueMap<>( params );
                    addAuthParamIfAdmin( api, apiParams );
                    return UriComponentsBuilder.fromUri( api )
                            .path( path )
                            .replaceQueryParams( apiParams )
                            .build().toUri();
                } )
                .map( uri -> Pair.of( uri, asyncRestTemplate.getForEntity( uri, arrCls ) ) )
                // it's important to collect, otherwise the future will be created and joined on-by-one, defeating the purpose of using them
                .collect( Collectors.toList() ).stream()
                .map( uriAndFuture -> {
                    try {
                        return getFromRequestFuture( uriAndFuture.getLeft(), uriAndFuture.getRight() );
                    } catch ( RemoteException e ) {
                        return null;
                    }
                } )
                .filter( Objects::nonNull )
                .map( ResponseEntity::getBody )
                .flatMap( Arrays::stream )
                .collect( Collectors.toList() );
    }

    private <T> T getFromRequestFuture( URI uri, Future<T> future ) throws RemoteException {
        Integer requestTimeout = applicationSettings.getIsearch().getRequestTimeout();
        try {
            if ( requestTimeout != null ) {
                return future.get( requestTimeout.longValue(), TimeUnit.SECONDS );
            } else {
                return future.get();
            }
        } catch ( ExecutionException e ) {
            throw new RemoteException( String.format( "Unsuccessful response received for %s.", uri ), e.getCause() );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RemoteException( String.format( "A thread was interrupted while waiting for %s response.", uri ), e );
        } catch ( TimeoutException e ) {
            // no need for the stacktrace in case of timeout
            throw new RemoteException( String.format( "Partner registry %s has timed out after %d s.", uri.getRawAuthority(), requestTimeout ), e );
        }
    }

    private URI getApiUri( URI remoteHost ) throws RemoteException {
        String remoteHostAuthority = remoteHost.getAuthority();
        Map<String, URI> apiUriByAuthority = Arrays.stream( applicationSettings.getIsearch().getApis() )
                .map( URI::create )
                .collect( Collectors.toMap( URI::getAuthority, identity() ) );
        if ( !apiUriByAuthority.containsKey( remoteHost.getAuthority() ) ) {
            throw new RemoteException( MessageFormat.format( "Unknown remote API {0}.", remoteHost.getAuthority() ) );
        }
        return apiUriByAuthority.get( remoteHostAuthority );
    }

    private void initUser( User user ) {
        user.getUserGenes().values().forEach( ug -> ug.setUser( user ) );
        user.getUserTerms().forEach( ug -> ug.setUser( user ) );
        user.getUserOrgans().values().forEach( ug -> ug.setUser( user ) );
    }

    private SortedSet<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toCollection( TreeSet::new ) );
    }

    private void addAuthParamIfAdmin( URI apiUri, MultiValueMap<String, String> query ) {
        User user = userService.findCurrentUser();
        if ( user != null && user.getRoles().contains( roleRepository.findByRole( "ROLE_ADMIN" ) ) ) {
            UriComponents apiUriComponents = UriComponentsBuilder.fromUri( apiUri ).build();
            //noinspection StatementWithEmptyBody
            if ( apiUriComponents.getQueryParams().containsKey( "noauth" ) ) {
                // do nothing, we don't have admin access for this partner
            } else if ( apiUriComponents.getQueryParams().containsKey( "auth" ) ) {
                // use a specific search token
                query.add( "auth", apiUriComponents.getQueryParams().getFirst( "auth" ) );
            } else {
                // use the default search token
                query.add( "auth", applicationSettings.getIsearch().getSearchToken() );
            }
        }
    }

    @Data
    @SuperBuilder
    static class UserSearchParams {

        private Collection<ResearcherPosition> researcherPositions;
        private Collection<ResearcherCategory> researcherCategories;
        private Collection<String> organUberonIds;
        private Collection<OntologyTermInfo> ontologyTermInfos;

        public MultiValueMap<String, String> toMultiValueMap() {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            if ( researcherPositions != null ) {
                for ( ResearcherPosition researcherPosition : researcherPositions ) {
                    params.add( "researcherPosition", researcherPosition.name() );
                }
            }
            if ( researcherCategories != null ) {
                for ( ResearcherCategory researcherCategory : researcherCategories ) {
                    params.add( "researcherCategory", researcherCategory.name() );
                }
            }
            if ( organUberonIds != null ) {
                for ( String organUberonId : organUberonIds ) {
                    params.add( "organUberonIds", organUberonId );
                }
            }
            if ( ontologyTermInfos != null ) {
                for ( OntologyTermInfo ontologyTermInfo : ontologyTermInfos ) {
                    params.add( "ontologyNames", ontologyTermInfo.getOntology().getName() );
                    params.add( "ontologyTermIds", ontologyTermInfo.getTermId() );
                }
            }
            return params;
        }
    }

    @Data
    @SuperBuilder
    static class UserGeneSearchParams extends UserSearchParams {

        private Integer taxonId;
        private TierType tier;
        private Integer orthologTaxonId;

        @Override
        public MultiValueMap<String, String> toMultiValueMap() {
            MultiValueMap<String, String> params = super.toMultiValueMap();
            params.add( "taxonId", taxonId.toString() );
            params.add( "tier", tier.name() );
            if ( orthologTaxonId != null ) {
                params.add( "orthologTaxonId", orthologTaxonId.toString() );
            }
            return params;
        }

    }

}
