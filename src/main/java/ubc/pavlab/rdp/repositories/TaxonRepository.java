package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;

import javax.persistence.QueryHint;
import java.util.List;
import java.util.Set;

@Repository
public interface TaxonRepository extends JpaRepository<Taxon, Integer> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Taxon> findByActiveTrueOrderByOrdering();

    // New method to fetch a list of Taxon by their IDs
    List<Taxon> findByIdIn(Set<Integer> ids);
}
