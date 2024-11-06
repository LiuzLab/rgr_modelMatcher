package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.MatchDashboard.ScientistPublicResponse;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@CommonsLog
public class MatchDashboardSearchController {

    private final GeneInfoService geneService;
    private final TaxonService taxonService;
    private final UserGeneService userGeneService;
    private final RemoteResourceService remoteResourceService;
    private final OrganInfoService organInfoService;

    @Autowired
    public MatchDashboardSearchController(GeneInfoService geneService,
                                          TaxonService taxonService,
                                          UserGeneService userGeneService,
                                          RemoteResourceService remoteResourceService,
                                          OrganInfoService organInfoService) {
        this.geneService = geneService;
        this.taxonService = taxonService;
        this.userGeneService = userGeneService;
        this.remoteResourceService = remoteResourceService;
        this.organInfoService = organInfoService;
    }

    @Value("${modelMatcher.url}")
    private String HOSTING_BASE_URL;

    @GetMapping(value = "/SearchScientistByGene", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchScientistUsersByGene(
            @RequestParam String symbol,
            @RequestParam(required = false, defaultValue = "9606") Integer taxonId,
            @RequestParam(required = false, defaultValue = "false") Boolean iSearch) {

        try {
            if (symbol == null || symbol.isEmpty()) {
                log.warn("Symbol is required for gene search.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Symbol parameter is required.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Set<TierType> tiers = TierType.ANY;
            Taxon taxon = taxonService.findById(taxonId);
            if (taxon == null) {
                throw new IllegalArgumentException("Invalid taxon ID provided.");
            }
            GeneInfo gene = geneService.findBySymbolAndTaxon(symbol, taxon);

            List<UserGene> userGenes = userGeneService.handleGeneSearch(gene, tiers, null,
                    null, null, organsFromUberonIds(null));

            List<ScientistPublicResponse> scientistUserResponses = mapResponseToScientistPublicResponse(userGenes);
            List<ScientistPublicResponse> combinedUserList = new ArrayList<>(scientistUserResponses);

            if (Boolean.TRUE.equals(iSearch)) {
                Collection<UserGene> itlUserGenes = remoteResourceService.findGenesBySymbol(symbol, taxon, tiers, null, null, null, null);
                List<ScientistPublicResponse> itlResponses = mapResponseToScientistPublicResponse(itlUserGenes);
                combinedUserList = Stream.concat(scientistUserResponses.stream(), itlResponses.stream())
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("gene", gene);
            response.put("ScientistMatches", combinedUserList);

            log.info("Response generated for searchScientistUsersByGene with symbol: " + symbol);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Error with provided arguments: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error retrieving searchScientistUsersByGene", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private List<ScientistPublicResponse> mapResponseToScientistPublicResponse(Collection<UserGene> userGenes) {
        return userGenes.stream().map(userGene -> {
            ScientistPublicResponse response = new ScientistPublicResponse();

            response.setModelOrganism(userGene.getTaxon());
            response.setMatchingGeneId(userGene.getGeneId());
            response.setMatchingGeneSymbol(userGene.getSymbol());
            response.setMatchingGeneName(userGene.getName());
            response.setMatchingGeneAliases(userGene.getAliases());
            response.setTier(userGene.getTier());

            ResearcherPosition pi = (userGene.getRemoteUser() != null && userGene.getRemoteUser().getProfile() != null)
                    ? userGene.getRemoteUser().getProfile().getResearcherPosition()
                    : (userGene.getUser() != null && userGene.getUser().getProfile() != null)
                    ? userGene.getUser().getProfile().getResearcherPosition()
                    : null;
            response.setPI(pi);

            String origin = (userGene.getRemoteUser() != null && userGene.getRemoteUser().getOrigin() != null)
                    ? userGene.getRemoteUser().getOrigin()
                    : "ModelMatcher";
            response.setNetwork(origin);

            String profileLink = (userGene.getRemoteUser() != null)
                    ? userGene.getRemoteUser().getOriginUrl() + "/userView/" + userGene.getRemoteUser().getId()
                    : HOSTING_BASE_URL + "/userView/" + userGene.getUser().getId();
            response.setProfileLink(profileLink);

            return response;
        }).collect(Collectors.toList());
    }

    private Collection<OrganInfo> organsFromUberonIds(Set<String> organUberonIds) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn(organUberonIds);
    }
}
