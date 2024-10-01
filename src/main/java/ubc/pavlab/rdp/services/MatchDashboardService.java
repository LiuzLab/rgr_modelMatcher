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

    // Update Step 1: Basic Profile Information
    User updateUserBasicProfile(User user, Profile profile);

    // Update Step 2: Publications
    User updateUserPublications(User user, Set<Publication> publications);

    // Update Step 3: Organ Uberon IDs and Researcher Categories
    User updateUserOrgansAndResearcherCategories(User user, Set<String> organUberonIds, Profile profile);

    // Update Step 4: Taxon Genes and Descriptions
    User updateUserGenesAndTaxonDescriptions(User user, Map<String, TaxonGenesPayload> taxonGenesPayload);

    // Save user to the database
    User save(User user);
}
