package ubc.pavlab.rdp.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileUpdateRequest {
    private Profile profile; // Step 1
    private Set<Publication> publications; // Step 2
    private Set<String> organUberonIds; // Step 3
    private Map<String, TaxonGenesPayload> taxonGenesPayload; // Step 4
}
