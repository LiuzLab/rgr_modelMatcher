package ubc.pavlab.rdp.controllers;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.*;

@TestPropertySource(properties = {
        "rdp.site.contact-email=contact@localhost",
        "rdp.site.admin-email=admin@localhost",
        "rdp.site.host=http://localhost",
        "rdp.site.mainsite=https://example.com" })
@WebMvcTest(ApiController.class)
@RunWith(SpringRunner.class)
@Import({ WebSecurityConfig.class, SiteSettings.class })
@EnableSpringDataWebSupport
public class ApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean(name = "messageSource")
    private MessageSource messageSource;
    @MockBean
    private UserService userService;
    @MockBean
    private TaxonService taxonService;
    @MockBean
    private GeneInfoService geneService;
    @MockBean
    private UserOrganService userOrganService;
    @MockBean
    private UserGeneService userGeneService;
    @MockBean
    private OrganInfoService organInfoService;
    @MockBean
    private ApplicationSettings applicationSettings;
    @MockBean
    private ApplicationSettings.SearchSettings searchSettings;
    @MockBean
    private ApplicationSettings.InternationalSearchSettings iSearchSettings;
    @MockBean
    private ApplicationSettings.PrivacySettings privacySettings;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private PermissionEvaluator permissionEvaluator;
    @MockBean
    private OntologyService ontologyService;
    @MockBean
    private UserPrivacyService userPrivacyService;
    @MockBean
    private BuildProperties buildProperties;

    @Before
    public void setUp() {
        when( applicationSettings.getSearch() ).thenReturn( searchSettings );
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( searchSettings.getEnabledSearchModes() ).thenReturn( new LinkedHashSet<>( EnumSet.allOf( ApplicationSettings.SearchSettings.SearchMode.class ) ) );
        when( iSearchSettings.isEnabled() ).thenReturn( true );
        when( messageSource.getMessage( eq( "rdp.site.shortname" ), any(), any() ) ).thenReturn( "RDMM" );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.empty() );
        when( buildProperties.getVersion() ).thenReturn( "1.5.0" );
    }

    @Test
    public void accessUnmappedRoute_thenReturn404() throws Exception {
        mvc.perform( get( "/api/notfound" ) )
                .andExpect( status().isNotFound() )
                .andExpect( content().contentType( MediaType.TEXT_PLAIN ) );
    }

    @Test
    public void accessWithoutProperRole_thenReturn401() throws Exception {
        when( userService.countResearchers() )
                .thenThrow( AccessDeniedException.class );
        mvc.perform( get( "/api/stats" ) )
                .andExpect( status().isUnauthorized() )
                .andExpect( content().contentType( MediaType.TEXT_PLAIN ) );
        verify( userService ).countResearchers();
    }

    @Test
    public void getStats() throws Exception {
        mvc.perform( get( "/api/stats" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.version" ).value( "1.5.0" ) );
    }

    @Test
    public void getStats_withOrigin_thenIncludeAccessControlHeadersInResponse() throws Exception {
        mvc.perform( get( "/api/stats" )
                        .header( HttpHeaders.ORIGIN, "https://example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( header().string( HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com" ) );
    }

    @Test
    public void searchUsers() throws Exception {
        User user = createUser( 1 );
        when( userService.findByNameAndDescription( "robert", false, "pancake", null, null, null, null ) )
                .thenReturn( Collections.singletonList( user ) );
        mvc.perform( get( "/api/users/search" )
                        .param( "nameLike", "robert" )
                        .param( "descriptionLike", "pancake" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].id" ).value( 1 ) )
                .andExpect( jsonPath( "$[0].origin" ).value( "RDMM" ) )
                .andExpect( jsonPath( "$[0].originUrl" ).value( "http://localhost" ) );
        verify( userService ).findByNameAndDescription( "robert", false, "pancake", null, null, null, null );
        verify( userPrivacyService ).checkCurrentUserCanSeeGeneList( user );
    }

    @Test
    public void searchGenes_withSearchDisabled_thenReturnServiceUnavailable() throws Exception {
        // configure remote authentication
        when( iSearchSettings.getAuthTokens() ).thenReturn( Collections.singletonList( "1234" ) );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.of( createUser( 1 ) ) );

        when( iSearchSettings.isEnabled() ).thenReturn( false );
        mvc.perform( get( "/api/genes/search" )
                        .header( "Authorization", "Bearer 1234" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" ) )
                .andExpect( status().isServiceUnavailable() );
    }

    @Test
    public void searchGenes_withAuthToken_thenReturnSuccess() throws Exception {
        // configure remote authentication
        when( iSearchSettings.getAuthTokens() ).thenReturn( Collections.singletonList( "1234" ) );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.of( createUser( 1 ) ) );

        // configure one search result
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 2 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .header( "Authorization", "Bearer 1234" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" ) )
                .andExpect( status().is2xxSuccessful() );

        verify( userService ).getRemoteSearchUser();
    }

    @Test
    public void searchGenes_withAuthTokenInQuery_thenReturnSuccess() throws Exception {
        // configure remote authentication
        when( iSearchSettings.getAuthTokens() ).thenReturn( Collections.singletonList( "1234" ) );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.of( createUser( 1 ) ) );

        // configure one search result
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 2 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" )
                        .param( "auth", "1234" ) )
                .andExpect( status().is2xxSuccessful() );

        verify( userService ).getRemoteSearchUser();
    }

    @Test
    public void searchGenes_withInvalidAuthToken_thenReturnUnauthorized() throws Exception {
        mvc.perform( get( "/api/genes/search" )
                        .header( "Authorization", "Bearer unknownToken" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isUnauthorized() )
                .andExpect( header().stringValues( "WWW-Authenticate", "Bearer" ) )
                .andExpect( content().contentType( MediaType.TEXT_PLAIN ) )
                .andExpect( content().string( "No user associated to the provided API token." ) );
    }

    @Test
    public void searchGenes_withInvalidAuthTokenScheme_thenIgnore() throws Exception {
        mvc.perform( get( "/api/genes/search" )
                        .header( "Authorization", "Basic unknownToken" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" ) )
                .andExpect( status().isNotFound() )
                .andExpect( content().contentType( MediaType.TEXT_PLAIN ) );
    }

    @Test
    public void searchGenes_whenMisconfiguredRemoteAdmin_thenReturnUnauthorized() throws Exception {
        when( iSearchSettings.getAuthTokens() ).thenReturn( Collections.singletonList( "1234" ) );
        mvc.perform( get( "/api/genes/search" )
                        .header( "Authorization", "Bearer 1234" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" ) )
                .andExpect( status().isUnauthorized() )
                .andExpect( content().contentType( MediaType.TEXT_PLAIN ) );
    }

    @Test
    public void searchGenes_withSingleTier_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER1" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), null, null, null, null, null );
    }

    @Test
    public void searchGenes_withMultipleTiers_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tiers", "TIER1" )
                        .param( "tiers", "TIER2" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), null, null, null, null, null );
    }

    @Test
    public void searchGenes_withMultipleTiersIncludingTier3_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tiers", "TIER1" )
                        .param( "tiers", "TIER2" )
                        .param( "tiers", "TIER3" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), null, null, null, null, null );
    }

    @Test
    public void searchGenes_withNoTiers_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, TierType.MANUAL, null, null, null, null, null );
    }

    @Test
    public void searchGenes_withTiers12_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIERS1_2" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, TierType.MANUAL, null, null, null, null, null );
    }

    @Test
    public void searchGenes_withTierAny_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null, null, null ) )
                .thenReturn( Lists.newArrayList( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "ANY" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, TierType.MANUAL, null, null, null, null, null );
    }

    @Test
    public void searchGenes_withInvalidTier_thenReturnBadRequest() throws Exception {
        mvc.perform( get( "/api/genes/search" )
                        .param( "symbol", "CDH1" )
                        .param( "taxonId", "9606" )
                        .param( "tier", "TIER4" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.TEXT_PLAIN ) );
    }

    @Test
    public void getUser_thenReturn2xxSuccessful() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserById( 1 ) ).thenReturn( user );
        mvc.perform( get( "/api/users/{userId}", 1 ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( jsonPath( "$.id" ).value( 1 ) )
                .andExpect( jsonPath( "$.origin" ).value( "RDMM" ) )
                .andExpect( jsonPath( "$.originUrl" ).value( "http://localhost" ) );
    }

    @Test
    public void getUser_withAnonymousId_thenReturn2xxSuccessful() throws Exception {
        User user = createUser( 1 );
        UUID anonymousId = UUID.randomUUID();
        when( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ).thenReturn( true );
        when( userService.findUserByAnonymousIdNoAuth( anonymousId ) ).thenReturn( user );
        when( userService.anonymizeUser( user ) ).thenReturn( User.builder().anonymousId( anonymousId ).build() );
        mvc.perform( get( "/api/users/by-anonymous-id/{anonymousId}", anonymousId ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( jsonPath( "$.anonymousId" ).value( anonymousId.toString() ) )
                .andExpect( jsonPath( "$.origin" ).value( "RDMM" ) )
                .andExpect( jsonPath( "$.originUrl" ).value( "http://localhost" ) );

    }

    @Test
    public void getUser_withAnonymousIdAndFeatureIsDisabled_thenReturnServiceUnavailable() throws Exception {
        User user = createUser( 1 );
        UUID anonymousId = UUID.randomUUID();
        when( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ).thenReturn( false );
        when( userService.findUserByAnonymousIdNoAuth( anonymousId ) ).thenReturn( user );
        when( userService.anonymizeUser( user ) ).thenReturn( User.builder().anonymousId( anonymousId ).build() );
        mvc.perform( get( "/api/users/by-anonymous-id/{anonymousId}", anonymousId ) )
                .andExpect( status().isServiceUnavailable() );
    }

    @Test
    public void createOntology_() {
        assertThat( createOntology( "uberon", 1, 3, 1.0 ).getTerms() )
                .hasSize( 3 );
        assertThat( createOntology( "uberon", 3, 3, 1.0 ).getTerms() )
                .hasSize( 3 + ( 3 * 3 ) + ( 3 * 3 * 3 ) );
        assertThat( createOntology( "uberon", 5, 4, .8 ).getTerms() )
                .hasSize( 379 );
    }

    @Test
    public void getOntologies() throws Exception {
        List<Ontology> onts = Lists.newArrayList(
                createOntology( "uberon", 5, 4, 1 ),
                createOntology( "mondo", 5, 4, 1 ) );
        when( ontologyService.findAllOntologies() ).thenReturn( onts );
        long numberOfTerms = 5 + ( 5 * 5 ) + ( 5 * 5 * 5 ) + ( 5 * 5 * 5 * 5 );
        when( ontologyService.countActiveTerms( onts.get( 0 ) ) ).thenReturn( numberOfTerms );
        when( ontologyService.countActiveTerms( onts.get( 1 ) ) ).thenReturn( numberOfTerms );
        when( messageSource.getMessage( "rdp.ontologies.mondo.definition", null, null, Locale.getDefault() ) )
                .thenReturn( "a bunch of disease terms" );
        mvc.perform( get( "/api/ontologies" )
                        .locale( Locale.getDefault() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$" ).isArray() )
                .andExpect( jsonPath( "$[0].id" ).doesNotExist() )
                .andExpect( jsonPath( "$[0].name" ).value( "uberon" ) )
                .andExpect( jsonPath( "$[0].definition" ).value( nullValue() ) )
                .andExpect( jsonPath( "$[0].numberOfTerms" ).value( numberOfTerms ) )
                .andExpect( jsonPath( "$[0].numberOfObsoleteTerms" ).value( 0 ) )
                .andExpect( jsonPath( "$[1].name" ).value( "mondo" ) )
                .andExpect( jsonPath( "$[1].definition" ).value( "a bunch of disease terms" ) )
                .andExpect( jsonPath( "$[1].numberOfTerms" ).value( numberOfTerms ) )
                .andExpect( jsonPath( "$[1].numberOfObsoleteTerms" ).value( 0 ) );

        verify( ontologyService ).findAllOntologies();
    }

    @Test
    public void getOntologyTerms() throws Exception {
        Ontology ont = createOntology( "uberon", 3, 2, 1 );
        when( ontologyService.findByName( "uberon" ) ).thenReturn( ont );
        when( ontologyService.findAllTermsByOntology( eq( ont ), any() ) )
                .thenAnswer( a -> PageableExecutionUtils.getPage( new ArrayList<>( ont.getTerms() ),
                        a.getArgument( 1, Pageable.class ), () -> ont.getTerms().size() ) );
        mvc.perform( get( "/api/ontologies/{ontologyName}/terms", "uberon" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.totalElements" ).value( ont.getTerms().size() ) );
        verify( ontologyService ).findAllTermsByOntology( ont, PageRequest.of( 0, 20 ) );
    }

    @Test
    public void getOntologyTerm() throws Exception {
        Ontology ont = createOntology( "uberon", 3, 2, 1 );
        when( ontologyService.findTermByTermIdAndOntologyName( any(), eq( "uberon" ) ) )
                .thenAnswer( arg -> ont.getTerms().stream()
                        .filter( t -> t.getTermId().equals( arg.getArgument( 0, String.class ) ) )
                        .findFirst()
                        .orElse( null ) );
        when( messageSource.getMessage( "rdp.ontologies.uberon.terms.UBERON:00001.definition", null, null, Locale.getDefault() ) )
                .thenReturn( "this is a nice definition" );
        mvc.perform( get( "/api/ontologies/{ontologyName}/terms/{termId}", "uberon", "UBERON:00001" )
                        .locale( Locale.getDefault() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.id" ).doesNotExist() )
                .andExpect( jsonPath( "$.termId" ).value( "UBERON:00001" ) )
                .andExpect( jsonPath( "$.definition" ).value( "this is a nice definition" ) )
                .andExpect( jsonPath( "$.subTerms" ).isArray() );
        verify( ontologyService ).findTermByTermIdAndOntologyName( "UBERON:00001", "uberon" );
        verify( messageSource ).getMessage( "rdp.ontologies.uberon.terms.UBERON:00001.definition", null, null, Locale.getDefault() );
    }

    @Test
    public void getOntologyTerm_whenTermIsNotActive_thenReturn404() throws Exception {
        Ontology ont = createOntology( "uberon", 3, 2, 1 );
        OntologyTermInfo inactiveTerm = OntologyTermInfo.builder( ont, "UBERON:00001" ).build();
        when( ontologyService.findTermByTermIdAndOntologyName( "UBERON:00001", "uberon" ) ).thenReturn( inactiveTerm );
        mvc.perform( get( "/api/ontologies/{ontologyName}/terms/{termId}", "uberon", "UBERON:00001" ) )
                .andExpect( status().isNotFound() );
        verify( ontologyService ).findTermByTermIdAndOntologyName( "UBERON:00001", "uberon" );
    }
}
