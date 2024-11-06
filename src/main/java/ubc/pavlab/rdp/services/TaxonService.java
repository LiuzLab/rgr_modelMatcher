package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface TaxonService {

    Taxon findById( final Integer id );

    Map<Integer, Taxon> findByIds(Set<Integer> ids);

    Collection<Taxon> findByActiveTrue();

    Collection<Taxon> loadAll();

}
