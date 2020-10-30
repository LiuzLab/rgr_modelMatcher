package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;

/**
 * Created by mjacobson on 18/01/18.
 */
@Controller
@CommonsLog
public class TermController {

    @Autowired
    private GOService goService;

    @Autowired
    private TaxonService taxonService;

    @PreAuthorize("hasPermission(null, 'search')")
    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/term/search/{query}")
    public List<SearchResult<GeneOntologyTerm>> searchTermsByQueryAndTaxon( @PathVariable Integer taxonId, @PathVariable String query,
                                                                            @RequestParam(value = "max", required = false, defaultValue = "-1") int max ) {
        Taxon taxon = taxonService.findById( taxonId );

        return goService.search( query, taxon, max );

    }

    @PreAuthorize("hasPermission(null, 'search')")
    @ResponseBody
    @GetMapping(value = "/term/{goId}")
    public GeneOntologyTerm getTerm( @PathVariable String goId ) {
        return goService.getTerm( goId );
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/term/{goId}/gene")
    public Collection<GeneInfo> termGenes( @PathVariable Integer taxonId, @PathVariable String goId ) {
        Taxon taxon = taxonService.findById( taxonId );
        GeneOntologyTerm term = goService.getTerm( goId );

        return goService.getGenesInTaxon( term, taxon );
    }

}
