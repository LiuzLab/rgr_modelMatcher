package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxonGenesPayload {

    @JsonProperty("geneTierMap")
    private Map<String, TierType> genesToTierMap;

    @JsonProperty("genePrivacyLevelMap")
    private Map<String, PrivacyLevelType> genesToPrivacyLevelMap;

    private String taxonDescription;
}