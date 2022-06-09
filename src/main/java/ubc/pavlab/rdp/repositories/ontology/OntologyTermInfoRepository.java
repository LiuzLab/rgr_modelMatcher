package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * We generally try to be careful here because a lot of terms can be retrieved.
 *
 * @author poirigui
 */
@Repository
public interface OntologyTermInfoRepository extends JpaRepository<OntologyTermInfo, Integer> {

    OntologyTermInfo findByTermIdAndOntology( String ontologyTermInfoId, Ontology ontology );

    /**
     * Retrieve all active terms in all ontologies in a consumable stream.
     */
    Stream<OntologyTermInfo> findAllByActiveTrueAndOntologyActiveTrue();

    List<OntologyTermInfo> findAllByActiveTrueAndIdIn( Collection<Integer> ids );

    /**
     * Retrieve all active terms in a given ontology in a paginated format.
     * <p>
     * Some ontologies (i.e. MONDO, UBERON) have thousands of terms with substantial definition text that can take
     * significant transport time, which is why we favour pagination.
     */
    Page<OntologyTermInfo> findAllByActiveTrueAndOntology( Ontology ontology, Pageable pageable );

    /**
     * Retrieve all terms in a paginated format.
     */
    Page<OntologyTermInfo> findAllByOntology( Ontology ontology, Pageable pageable );

    List<OntologyTermInfo> findAllByActiveTrueAndNameAndOntologyName( String name, String ontologyName );

    /**
     * Retrieve all active terms from active ontologies with the given term ID.
     * <p>
     * In general, this corresponds to only one term, but our system does not prevent multiple ontologies from sharing
     * terms.
     */
    List<OntologyTermInfo> findAllByOntologyInAndTermIdIgnoreCaseAndActive( Set<Ontology> ontologies, String query, boolean active );

    @Query("select t from OntologyTermInfo t join t.altTermIds a where t.active = :active and t.ontology in :ontologies and upper(a) = upper(:query)")
    List<OntologyTermInfo> findAllByOntologyInAndAltTermIdsContainingIgnoreCaseAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    List<OntologyTermInfo> findAllByOntologyInAndNameIgnoreCaseAndActive( Set<Ontology> ontologies, String query, boolean active );

    /**
     * Retrieve all active terms from active ontologies whose term name that match the given pattern.
     */
    List<OntologyTermInfo> findAllByOntologyInAndNameLikeIgnoreCaseAndActive( Set<Ontology> ontologies, String pattern, boolean active );

    /**
     * Retrieve all ontology term info.
     * <p>
     * This will only work on the MySQL vendor.
     *
     * @return a list two elements arrays where the first is the {@link OntologyTermInfo} ID and second its full text
     * score against the query
     */
    @Query(value = "select t.ontology_term_info_id, match(t.name) against(:query in boolean mode) as score from ontology_term_info t join ontology o on t.ontology_id = o.ontology_id where t.active = :active and o.ontology_id in (:ontologyIds) having score > 0", nativeQuery = true)
    List<Object[]> findAllByOntologyInAndNameMatchAndActive( @Param("ontologyIds") Set<Integer> ontologyIds, @Param("query") String query, @Param("active") boolean active );

    @Query("select t from OntologyTermInfo t join t.synonyms s where t.active = :active and t.ontology in :ontologies and upper(s) = upper(:query)")
    List<OntologyTermInfo> findAllByOntologyInAndSynonymsContainingIgnoreCaseAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    @Query("select t from OntologyTermInfo t join t.synonyms s where t.active = :active and t.ontology in :ontologies and upper(s) like upper(:query)")
    List<OntologyTermInfo> findAllByOntologyInAndSynonymsLikeIgnoreCaseAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    @Query(value = "select t.ontology_term_info_id, match(otis.synonym) against (:query in boolean mode) as score from ontology_term_info t join ontology o on o.ontology_id = t.ontology_id join ontology_term_info_synonyms otis on t.ontology_term_info_id = otis.ontology_term_info_id where t.active = :active and o.ontology_id in :ontologyIds having score > 0", nativeQuery = true)
    List<Object[]> findAllByOntologyInAndSynonymsMatchAndActive( @Param("ontologyIds") Set<Ontology> ontologyIds, @Param("query") String query, @Param("active") boolean active );

    /**
     * Retrieve all active terms from active ontologies whose definition match the given pattern.
     */
    List<OntologyTermInfo> findAllByOntologyInAndDefinitionLikeIgnoreCaseAndActive( Set<Ontology> ontologies, String pattern, boolean active );

    @Query(value = "select t.ontology_term_info_id, match(t.definition) against (:query in boolean mode) as score from ontology_term_info t join ontology o on o.ontology_id = t.ontology_id where t.active = :active and o.ontology_id in :ontologyIds having score > 0", nativeQuery = true)
    List<Object[]> findAllByOntologyInAndDefinitionMatchAndActive( @Param("ontologyIds") Set<Integer> ontologyIds, @Param("query") String query, @Param("active") boolean active );

    /**
     * Count the number of active terms.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByActiveTrue();

    /**
     * Count the number of active terms in a given ontology.
     * <p>
     * Note: the result if this query is cached, so it should not be relied upon.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndActiveTrue( Ontology ontology );

    /**
     * Count the number of obsolete terms in a given ontology.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndObsoleteTrue( Ontology ontology );

    /**
     * Count the number of active terms with icons in a given ontology.
     * <p>
     * Note: this method does not check if the passed ontology is active or not.
     *
     * @return the number of terms with icons in the ontology
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndActiveTrueAndHasIconTrue( Ontology ontology );

    /**
     * Activate all the terms in a given ontology.
     * <p>
     * Already active and obsolete terms are ignored.
     *
     * @return the number of activated terms in the ontology
     */
    @Modifying
    @Query("update OntologyTermInfo t set t.active = true where t.ontology = :ontology and t.active = false and t.obsolete = false")
    int activateByOntologyAndActiveFalseAndObsoleteFalse( @Param("ontology") Ontology ontology );

    @Modifying
    @Query("update OntologyTermInfo t set t.active = true where t.id in :termIds and active = false and t.obsolete= false")
    int activateByTermIdsAndActiveFalseAndObsoleteFalse( @Param("termIds") Set<Integer> termIds );

    /**
     * Quickly gather subterm IDs.
     */
    @Query(value = "select ontology_sub_term_info_id from ontology_term_info_sub_terms where ontology_term_info_id in :termIds", nativeQuery = true)
    List<Integer> findSubTermsIdsByTermIdIn( @Param("termIds") Set<Integer> termIds );

    OntologyTermInfo findByTermIdAndOntologyName( String termId, String ontologyName );
}
