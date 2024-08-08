package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneInfoService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.util.SearchResult;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 22/01/18.
 */
@Controller
@CommonsLog
public class GeneController {

    @Autowired
    GeneInfoService geneService;

    @Autowired
    GOService goService;

    @Autowired
    private TaxonService taxonService;

    @ResponseBody
    @GetMapping(
            value = {"/taxon/{taxonId}/gene/search"},
            params = {"symbols"}
    )
    public Map<String, GeneInfo> searchGenesByTaxonAndSymbols( @PathVariable Integer taxonId, @RequestParam List<String> symbols) {
        Taxon taxon = taxonService.findById( taxonId );
//        return geneService.findBySymbolInAndTaxon( symbols, taxon )
//                .stream()
//                .collect( Collectors.toMap( Gene::getSymbol, Function.identity() ) );
        // return symbols.stream().collect( HashMap::new, ( m, s)->m.put(s, geneService.findBySymbolAndTaxon( s, taxon )), HashMap::putAll);
//        return symbols.stream().collect(Collectors.toMap(Function.identity(), s -> geneService.findBySymbolAndTaxon(s, taxon))); // UnComment when later and remote the botton lines


        Map<String, GeneInfo> test = symbols.stream().collect(Collectors.toMap(Function.identity(), s -> geneService.findBySymbolAndTaxon(s, taxon)));
//        log.info( MessageFormat.format( "< AUTOCOMPLETE RESPONSE (searchGenesBy Taxon And Symbols ) For  \ntaxonId : {0} : \nsymbols : {0} \n RESPONSE :: {0} >\n", taxonId, symbols, test ));
        return test;

    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/gene/search/{query}")
    public Collection<SearchResult<GeneInfo>> searchGenesByTaxonAndQuery( @PathVariable Integer taxonId, @PathVariable String query,
                                                                          @RequestParam(value = "max", required = false, defaultValue = "-1") int max ) {
        Taxon taxon = taxonService.findById( taxonId );
//        return geneService.autocomplete( query, taxon, max ); // UnComment when later and remote the botton lines

        Collection<SearchResult<GeneInfo>> test = geneService.autocomplete(query, taxon, max);
//        log.info( MessageFormat.format( "< AUTOCOMPLETE RESPONSE (searchGenesBy Taxon And Query)  For  \ntaxonId : {0} : \nquery : {1} \n RESPONSE :: {2} >\n", taxonId, query, test ));
        return test;


    }

    @ResponseBody
    @GetMapping(value = "/gene/{geneId}")
    public Gene getGene( @PathVariable Integer geneId ) {
        return geneService.load( geneId );
    }

    @ResponseBody
    @GetMapping(value = "/gene/{geneId}/term")
    public Collection<GeneOntologyTerm> getGeneTerms( @PathVariable Integer geneId ) {
        GeneInfo gene = geneService.load( geneId );
        return goService.getTermsForGene( gene, true, true );
    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/genebySymbol/{symbol}")
    public Gene getGeneByTaxonAndSymbol( @PathVariable Integer taxonId, @PathVariable String symbol ) {
        Taxon taxon = taxonService.findById( taxonId );
//        return geneService.findBySymbolAndTaxon( symbol, taxon );  // UnComment when later and remote the botton lines

        GeneInfo Result = geneService.findBySymbolAndTaxon(symbol, taxon);
//        log.info( MessageFormat.format( "< AUTOCOMPLETE RESPONSE (get GeneBy Taxon And Symbol) For  \ntaxonId : {0} : \nsymbol : {1} \nRESPONSE :: {2} >\n", taxonId, symbol, test ));
        return Result;
    }


    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/geneBySymbolList",
            params = {"symbols"}
    )
    public List<GeneInfo> getGeneByTaxonAndSymbolList( @PathVariable Integer taxonId,  @RequestParam List<String> symbols) {
        Taxon taxon = taxonService.findById( taxonId );

        List<GeneInfo> results = (List<GeneInfo>) geneService.findBySymbolListInAndTaxon(symbols, taxon);
        log.info( MessageFormat.format( "< getGeneByTaxonAndSymbolList RESPONSE  \ntaxonId : {0} : \nsymbols : {1} \nRESPONSE :: {2} >\n", taxonId, symbols, results ));
        return results;
    }
}
