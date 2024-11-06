package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.repositories.TaxonRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("taxonService")
public class TaxonServiceImpl implements TaxonService {

    @Autowired
    private TaxonRepository taxonRepository;

    @Override
    public Taxon findById( Integer id ) {
        return taxonRepository.findOne( id );
    }

    @Override
    public Map<Integer, Taxon> findByIds(Set<Integer> ids) {
        List<Taxon> taxons = taxonRepository.findByIdIn(ids);

        // Convert the list of Taxon objects to a map with the ID as the key
        return taxons.stream()
                .collect(Collectors.toMap(Taxon::getId, taxon -> taxon));
    }

    @Override
    public Collection<Taxon> findByActiveTrue() {
        return taxonRepository.findByActiveTrueOrderByOrdering();
    }

    @Override
    public Collection<Taxon> loadAll() {
        return taxonRepository.findAll();
    }

}
