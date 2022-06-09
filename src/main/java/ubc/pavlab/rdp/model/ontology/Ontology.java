package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.cache.annotation.Cacheable;

import javax.persistence.*;
import java.net.URL;
import java.util.*;

/**
 * An ontology of terms.
 *
 * @author poirigui
 */
@Entity
@Table(name = "ontology")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = { "name" })
@ToString(of = { "id", "name" })
public class Ontology implements Comparable<Ontology> {

    public static OntologyBuilder builder( @NonNull String name ) {
        return new OntologyBuilder().name( name );
    }

    /**
     * Obtain a comparator for ordering categories.
     * <p>
     * Use the {@link #getOrdering()} if available, otherwise rely on the {@link #getName()}.
     */
    public static Comparator<Ontology> getComparator() {
        return Comparator.comparing( Ontology::getOrdering, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( Ontology::getName );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ontology_id")
    @JsonIgnore
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Set of terms associated to this category.
     * <p>
     * The {@link LazyCollectionOption#EXTRA} makes it such that calling terms.size() or terms.contains() does not
     * result in a full initialization.
     * <p>
     * Be extremely careful with this collection.
     * <p>
     * Note: I know it's tempting to map this by term ID with {@link MapKey}, but don't. This collection can get pretty
     * huge.
     */
    @OneToMany(mappedBy = "ontology", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordering asc, name asc, termId asc")
    @JsonIgnore
    @LazyCollection(LazyCollectionOption.EXTRA)
    private final SortedSet<OntologyTermInfo> terms = new TreeSet<>();

    /**
     * URL used to resolve the ontology source.
     * <p>
     * The only supported format for now is OBO.
     */
    @JsonIgnore
    private URL ontologyUrl;

    /**
     * Indicate if the ontology is active.
     */
    @JsonIgnore
    private boolean active;

    /**
     * Relative order of categories when sorted as per {@link #getComparator()}.
     */
    @JsonIgnore
    private Integer ordering;

    @Override
    public int compareTo( Ontology ontology ) {
        return getComparator().compare( this, ontology );
    }
}
