package ubc.pavlab.rdp.model.MatchDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchByGeneResponse {
    private boolean error = false;
    private String errorMessage;

    private String symbol;
    private Integer geneId;
    private Map<String, List<String>> orthologs;
    private List<ScientistPublicResponse> localScientists;
    private List<ScientistPublicResponse> partnerScientists;
    private String userEmail;

    public static MatchByGeneResponse error(String msg) {
        MatchByGeneResponse e = new MatchByGeneResponse();
        e.setError(true);
        e.setErrorMessage(msg);
        return e;
    }
}
