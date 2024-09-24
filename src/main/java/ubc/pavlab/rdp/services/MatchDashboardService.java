package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.TaxonGenesPayload;

import java.util.Map;
import java.util.Set;

public interface MatchDashboardService {


    User update( User user );

    User updateProfileWithOrganUberonIdsAndModelOrganism(User user,
                                                         Profile profile,
                                                         Set<Publication> publications,
                                                         Set<String> organUberonIds,
                                                         Map<String, TaxonGenesPayload> taxonGenesPayload);
}
