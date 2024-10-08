package ubc.pavlab.rdp.model.MatchDashboard;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScientistPublicResponse {
    private Taxon modelOrganism;
    private Integer MatchingGeneId;
    private String MatchingGeneSymbol;
    private String MatchingGeneName;
    private String MatchingGeneAliases;
    private TierType tier;
    private ResearcherPosition PI;
    private String network;
    private String profileLink;

}
