package ubc.pavlab.rdp.controllers;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.services.OrganInfoService;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds common implementation details of {@link SearchController} and {@link SearchViewController}.
 *
 * @author poirigui
 * @see SearchController
 * @see SearchViewController
 */
public abstract class AbstractSearchController {

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private MessageSource messageSource;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    protected static class SearchParams {
        private boolean iSearch;
        private Set<ResearcherPosition> researcherPositions;
        private Set<ResearcherCategory> researcherCategories;
        private Set<String> organUberonIds;
        /**
         * Order matters here because we want to preserve the UI rendering.
         */
        private List<Integer> ontologyTermIds;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    protected static class UserSearchParams extends SearchParams {
        @NotNull
        private String nameLike;
        private boolean prefix;
        @NotNull
        private String descriptionLike;

        public UserSearchParams( String nameLike, boolean prefix, String descriptionLike, boolean iSearch, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, List<Integer> ontologyTermIds ) {
            super( iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds );
            this.nameLike = nameLike;
            this.prefix = prefix;
            this.descriptionLike = descriptionLike;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    protected static class GeneSearchParams extends SearchParams {
        @NotNull
        private GeneInfo gene;
        private Set<TierType> tiers;
        private Taxon orthologTaxon;

        public GeneSearchParams( GeneInfo gene, Set<TierType> tiers, Taxon orthologTaxon, boolean iSearch, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, List<Integer> ontologyTermIds ) {
            super( iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds );
            this.gene = gene;
            this.tiers = tiers;
            this.orthologTaxon = orthologTaxon;
        }
    }

    private static void addClause( StringBuilder builder, List<String> clauses ) {
        if ( builder.length() > 0 ) {
            builder.append( " AND " );
        }
        if ( clauses.size() > 1 ) {
            builder.append( '(' );
        }
        builder.append( String.join( " OR ", clauses ) );
        if ( clauses.size() > 1 ) {
            builder.append( ')' );
        }
    }

    private static void addClause( StringBuilder builder, String clause ) {
        addClause( builder, Collections.singletonList( clause ) );
    }

    private void addSearchParams( StringBuilder builder, SearchParams userSearchParams, Locale locale ) {
        if ( userSearchParams.researcherPositions != null ) {
            addClause( builder, userSearchParams.researcherPositions.stream()
                    .map( p -> String.format( locale, "researcher position equals '%s'",
                            messageSource.getMessage( "ResearcherPosition." + p.name(), null, locale ) ) )
                    .collect( Collectors.toList() ) );
        }
        if ( userSearchParams.researcherCategories != null ) {
            addClause( builder, userSearchParams.researcherCategories.stream()
                    .map( p -> String.format( locale, "researcher category equals '%s'",
                            messageSource.getMessage( "ResearcherCategory." + p.name(), null, locale ) ) )
                    .collect( Collectors.toList() ) );
        }
        if ( userSearchParams.organUberonIds != null ) {
            addClause( builder, organInfoService.findByUberonIdIn( userSearchParams.organUberonIds ).stream()
                    .map( p -> String.format( locale, "has term '%s' from UBERON", p.getName() ) ).collect( Collectors.toList() ) );
        }
        if ( userSearchParams.ontologyTermIds != null ) {
            List<OntologyTermInfo> infos = ontologyService.findAllTermsByIdIn( userSearchParams.ontologyTermIds );
            Map<Ontology, List<OntologyTermInfo>> termByOntology = infos.stream()
                    .collect( Collectors.groupingBy( OntologyTermInfo::getOntology, Collectors.toList() ) );
            for ( Map.Entry<Ontology, List<OntologyTermInfo>> entry : termByOntology.entrySet() ) {
                addClause( builder, entry.getValue().stream()
                        .map( p -> String.format( locale, "has term '%s' from '%s'",
                                ontologyService.resolveOntologyTermInfoName( p, locale ),
                                ontologyService.resolveOntologyName( entry.getKey(), locale ) ) )
                        .collect( Collectors.toList() ) );
            }
        }
    }

    protected String summarizeUserSearchParams( UserSearchParams userSearchParams, Locale locale ) {
        StringBuilder builder = new StringBuilder();
        if ( !userSearchParams.nameLike.isEmpty() ) {
            addClause( builder, String.format( "name %s '%s'", userSearchParams.prefix ? "starts with" : "contains", userSearchParams.nameLike ) );
        }
        if ( !userSearchParams.descriptionLike.isEmpty() ) {
            addClause( builder, String.format( "research description contains '%s'", userSearchParams.descriptionLike ) );
        }
        addSearchParams( builder, userSearchParams, locale );
        return "Search query: " + ( builder.length() == 0 ? "everything" : builder ) + ".";
    }

    protected String summarizeGeneSearchParams( GeneSearchParams geneSearchParams, Locale locale ) {
        StringBuilder builder = new StringBuilder();
        addClause( builder, String.format( "gene symbol equals '%s'", geneSearchParams.gene.getSymbol() ) );
        if ( geneSearchParams.getTiers() != null ) {
            addClause( builder, geneSearchParams.tiers.stream().map( t -> String.format( "gene with tier '%s'", t.getLabel() ) ).collect( Collectors.toList() ) );
        } else {
            addClause( builder, "gene in all tiers" );
        }
        if ( geneSearchParams.orthologTaxon != null ) {
            addClause( builder, String.format( "gene ortholog from '%s'", geneSearchParams.orthologTaxon.getCommonName() ) );
        } else {
            addClause( builder, "gene from all orthologs" );
        }
        addSearchParams( builder, geneSearchParams, locale );
        return "Search query: " + ( builder.length() == 0 ? "everything" : builder ) + ".";
    }

    /**
     * Rewrite the search parameters to be more specific when either the name or description patterns are missing.
     */
    protected String redirectToSpecificSearch( String nameLike, String descriptionLike ) {
        if ( nameLike.isEmpty() == descriptionLike.isEmpty() ) {
            throw new IllegalArgumentException( "Either of 'nameLike' or 'descriptionLike' has to be empty, but not both." );
        }
        if ( nameLike.isEmpty() ) {
            return "redirect:" + ServletUriComponentsBuilder.fromCurrentRequest()
                    .scheme( null ).host( null )
                    .replaceQueryParam( "nameLike" )
                    .replaceQueryParam( "prefix" )
                    .build( true )
                    .toUriString();
        } else {
            return "redirect:" + ServletUriComponentsBuilder.fromCurrentRequest()
                    .scheme( null ).host( null )
                    .replaceQueryParam( "descriptionLike" )
                    .build( true )
                    .toUriString();
        }
    }

    protected Collection<OrganInfo> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn( organUberonIds );
    }

    protected List<OntologyTermInfo> ontologyTermsFromIds( List<Integer> ontologyTermIds ) {
        return ontologyTermIds == null ? null : ontologyService.findAllTermsByIdIn( ontologyTermIds );
    }
}
