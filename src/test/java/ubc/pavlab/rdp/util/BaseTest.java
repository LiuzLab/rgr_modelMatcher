package ubc.pavlab.rdp.util;

import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mjacobson on 14/02/18.
 */
@SuppressWarnings("WeakerAccess")
public class BaseTest {

    protected static final String EMAIL = "bruce@wayne.com";
    protected static final String ENCODED_PASSWORD = "$2a$10$ny8JDrJGVcf27xs7RqsHh.ytcFQYhXqr4vI9Kq57HE1tQgePfQXyC";
    protected static final String PASSWORD = "imbatman";
    protected static final String NAME = "Bruce";
    protected static final String LAST_NAME = "Wayne";


    protected static final String TAXON_COMMON_NAME = "Honey Bee";
    protected static final String TAXON_SCIENTIFIC_NAME = "Apis mellifera";

    protected void becomeUser( User user ) {
        UserPrinciple up = new UserPrinciple( user );
        Authentication authentication = mock( Authentication.class );
        SecurityContext securityContext = mock( SecurityContext.class );
        when( securityContext.getAuthentication() ).thenReturn( authentication );
        SecurityContextHolder.setContext( securityContext );
        when( SecurityContextHolder.getContext().getAuthentication().getPrincipal() ).thenReturn( up );
        when( SecurityContextHolder.getContext().getAuthentication().getName() ).thenReturn( user.getEmail() );
    }

    protected Role createRole( int id, String role ) {
        Role r = new Role();
        r.setId( id );
        r.setRole( role );
        return r;
    }

    protected User createUnpersistedUser() {
        User user = new User();
        user.setEmail( EMAIL );
        user.setPassword( ENCODED_PASSWORD ); // imbatman

        Profile profile = new Profile();
        profile.setName( NAME );
        profile.setLastName( LAST_NAME );
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setShared( false );
        user.setProfile( profile );

        return user;
    }

    protected User createUser( int id ) {
        User user = createUnpersistedUser();
        user.setId( id );

        return user;
    }

    protected User createUserWithRole( int id, String... role ) {
        User user = createUnpersistedUser();
        user.setId( id );

        user.setRoles( Arrays.stream(role).map( r -> createRole( r.length(), r ) ).collect( Collectors.toSet() ) );

        return user;
    }

    @SneakyThrows
    protected Taxon createTaxon( int id ) {
        Taxon taxon = new Taxon();
        taxon.setActive( true );
        taxon.setCommonName( TAXON_COMMON_NAME );
        taxon.setScientificName( TAXON_SCIENTIFIC_NAME );
        taxon.setGeneUrl( new URL("ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Caenorhabditis_elegans.gene_info.gz") );
        taxon.setId( id );

        return taxon;
    }

    protected GeneOntologyTerm createTerm( String id ) {
        GeneOntologyTerm term = new GeneOntologyTerm();
        term.setGoId( id );
        term.setObsolete( false );

        return term;
    }

    protected GeneOntologyTerm createTermWithGene( String id, Gene... genes ) {
        GeneOntologyTerm term = new GeneOntologyTerm();
        term.setGoId( id );
        term.setObsolete( false );

        Arrays.stream(genes).forEach( g -> {
            term.getDirectGenes().add( g );
            g.getTerms().add( term );
        } );

        return term;
    }

    protected GeneInfo createGene( int id, Taxon taxon ) {
        GeneInfo gene = new GeneInfo();
        gene.setGeneId( id );
        gene.setTaxon( taxon );

        return gene;
    }

    protected UserTerm createUserTerm( int id, GeneOntologyTerm term, Taxon taxon ) {
        UserTerm ut = UserTerm.createUserTerm(term, taxon, null );
        ut.setId( id );
        return ut;
    }

    protected UserTerm createUserTerm( int id, GeneOntologyTerm term, Taxon taxon, Set<Gene> genes ) {
        UserTerm ut = UserTerm.createUserTerm(term, taxon, genes );
        ut.setId( id );
        return ut;
    }

    protected UserGene createUserGene( int id, Gene gene, User user, TierType tier ) {
        UserGene ug = UserGene.createUserGeneFromGene( gene, user, tier, PrivacyLevelType.PRIVATE );
        ug.setId( id );
        return ug;
    }

    protected String toGOId( int id ) {
        return String.format( "GO:%07d", id );
    }

}
