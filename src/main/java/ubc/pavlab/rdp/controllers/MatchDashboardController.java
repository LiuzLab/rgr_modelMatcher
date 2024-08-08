package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.UserService;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CommonsLog
public class MatchDashboardController {


    private final UserService userService;

    @Autowired
    public MatchDashboardController(UserService userService) {
        this.userService = userService;
    }

    // For New ModelMatcher Project.
    @GetMapping("/scientistRegistry/user/profile")
    public ResponseEntity<Map<String, Object>> getUserProfileByEmail(@RequestParam("email") String email) {
        try {

//            TODO: Later Move to service MD_service.
            User user = userService.findUserByEmailNoAuth(email);

            Map<Integer, String> userTaxonDescription = new HashMap<>();
            for (Map.Entry<Taxon, String> entry : user.getTaxonDescriptions().entrySet()) {
                Taxon taxon = entry.getKey();
                String description = entry.getValue();
                userTaxonDescription.put(taxon.getId(), description);
            }

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("userTaxonDescription", userTaxonDescription);
            log.info(String.format("MatchDashboardController :: getUserProfileByEmail for user email: %s : response: %s", email, user.getId())); // Avoid logging sensitive info
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping(value = "/scientistRegistry/test")
    public String getTest() {
        return "QA: Check Complete";
    }
}
