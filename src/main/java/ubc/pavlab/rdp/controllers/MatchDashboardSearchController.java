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
import ubc.pavlab.rdp.model.MatchDashboard.MatchByGeneResponse;
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

    private final String ANONOYMOUS = "Anonymous Scientist";
    private final String LINK_TO_RESTRICTED_GENE_INFO = "https://www.modelmatcher.net/Tutorials.html#collapse24_41";

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


    /**
     * A single JSON endpoint that returns:
     *  • gene metadata
     *  • ortholog mappings
     *  • local & partner registry scientists (with tiers, model organisms, etc.)
     *  • optional userEmail for audit/tracking
     */
    @GetMapping(path = "/scientistUserSearchByWebApp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MatchByGeneResponse> searchByGeneJson(
            @RequestParam String symbol,
            @RequestParam(required = false, defaultValue = "9606") Integer taxonId,
            @RequestParam(required = false, defaultValue = "false") Boolean iSearch,
            @RequestParam(required = false) String userEmail
    ) {
        // 1) validate & load gene
        Taxon taxon = taxonService.findById(taxonId);
        if (taxon == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(MatchByGeneResponse.error("Invalid taxonId"));
        }
        GeneInfo gene = geneService.findBySymbolAndTaxon(symbol, taxon);
        if (gene == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(MatchByGeneResponse.error("Gene not found: " + symbol));
        }

        // 2) build ortholog map: taxon → [symbols]
        Map<String,List<String>> orthologs = gene.getOrthologs().stream()
                .collect(Collectors.groupingBy(
                        gi -> gi.getTaxon().getScientificName(),
                        Collectors.mapping(GeneInfo::getSymbol, Collectors.toList())
                ));

        // 3) local matches
        Set<TierType> tiers = TierType.ANY;
        Collection<UserGene> localMatches = userGeneService
                .handleGeneSearch(gene, tiers, null, null, null, organsFromUberonIds(null));

        // 4) partner registry matches
        Collection<UserGene> partnerMatches = Collections.emptyList();
        if (Boolean.TRUE.equals(iSearch)) {
            partnerMatches = remoteResourceService
                    .findGenesBySymbol(symbol, taxon, tiers, null, null, null, null);
        }

        // 5) convert both sets to your DTO
        List<ScientistPublicResponse> local = mapResponseToScientistPublicResponse(localMatches);
        List<ScientistPublicResponse> partner = mapResponseToScientistPublicResponse(partnerMatches);

        // 6) package into the response DTO
        MatchByGeneResponse resp = new MatchByGeneResponse();
        resp.setSymbol(gene.getSymbol());
        resp.setGeneId(gene.getGeneId());
        resp.setOrthologs(orthologs);
        resp.setLocalScientists(local);
        resp.setPartnerScientists(partner);
        resp.setUserEmail(userEmail);

        return ResponseEntity.ok(resp);
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

            String lastName = (userGene.getUser().getProfile().getLastName() != null && userGene.getUser().getId() != 0)
                    ? userGene.getUser().getProfile().getLastName()
                    : ANONOYMOUS;
            response.setLastName(lastName);

            String organization = (userGene.getUser().getProfile().getOrganization() != null && userGene.getUser().getId() != 0)
                    ? userGene.getUser().getProfile().getOrganization()
                    : null;
            response.setOrganization(organization);

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
                    : ( userGene.getUser().getId() != 0 ? HOSTING_BASE_URL + "/userView/" + userGene.getUser().getId() : LINK_TO_RESTRICTED_GENE_INFO);
            response.setProfileLink(profileLink);

            return response;
        }).collect(Collectors.toList());
    }

    private Collection<OrganInfo> organsFromUberonIds(Set<String> organUberonIds) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn(organUberonIds);
    }



}
