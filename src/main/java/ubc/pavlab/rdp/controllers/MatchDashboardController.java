package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.MatchDashboardService;
import ubc.pavlab.rdp.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CommonsLog
public class MatchDashboardController {


    private final UserService userService;
    private final MatchDashboardService matchDashboardService;

    @Autowired
    public MatchDashboardController(UserService userService,
                                    MatchDashboardService matchDashboardService) {
        this.matchDashboardService = matchDashboardService;
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

    @GetMapping("/scientistRegistry/user/profileJson")
    public ResponseEntity<String> getUserProfileByEmailJsonSerialized(@RequestParam("email") String email) {
        try {
            // TODO: Later Move to service MD_service.
          User user = userService.findUserByEmailNoAuth(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Map<Integer, String> userTaxonDescription = new HashMap<>();
            for (Map.Entry<Taxon, String> entry : user.getTaxonDescriptions().entrySet()) {
                Taxon taxon = entry.getKey();
                String description = entry.getValue();
                userTaxonDescription.put(taxon.getId(), description);
            }

            // Prepare the response map
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("userTaxonDescription", userTaxonDescription);

            // Log user info (excluding sensitive data like email)
            log.info(String.format("MatchDashboardController :: getUserProfileByEmail for user email: %s : response: %s", email, user.getId()));

            // Create a custom ObjectMapper to serialize all fields
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);  // Ensure all fields, including null, are serialized
            mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);  // Include all fields even without views

            // Serialize the response to JSON
            String jsonResponse = mapper.writeValueAsString(response);

            // Return the serialized response
            return ResponseEntity.ok(jsonResponse);
        } catch (JsonProcessingException e) {
            log.error("Error serializing the response", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            log.error("Error retrieving user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }




//    @Data
//    static class UserUpdatedProfileModel {
//        @Valid
//        private String email;
//        private Profile profile;
//        private Set<String> organUberonIds;
//        private Map<Integer, TaxonGenesPayload> taxonGenesPayload;
//    }

//    @RequestParam("email") String email,
//    @PostMapping(value = "/scientistRegistry/user/updateProfile")
//    @PostMapping(value = "/scientistRegistry/user/updateProfile",
//            consumes = {"application/json", "application/json;charset=UTF-8"},
//            produces = "application/json")
//    @ResponseBody

    @PostMapping(
            value = "/scientistRegistry/user/updateProfile2",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> updateUserProfile(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        System.out.println("Request Body: " + requestBody);

        // Simulate processing
        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        return ResponseEntity.ok(response);
    }



    @PostMapping(
            value = "/scientistRegistry/user/updateProfile",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestBody UserUpdatedProfileModel userUpdatedProfileModel,
            HttpServletRequest request) {

        try{

//            Enumeration<String> headerNames = request.getHeaderNames();
//            while (headerNames.hasMoreElements()) {
//                String headerName = headerNames.nextElement();
//                System.out.println(headerName + ": " + request.getHeader(headerName));
//            }


            User user = userService.findUserByEmailNoAuth(userUpdatedProfileModel.getEmail());

            log.info(String.format("MatchDashboardController :: updateUserProfile for user email: %s : response: %s",
                    userUpdatedProfileModel.getEmail(), userUpdatedProfileModel));




            User updatedUser = matchDashboardService.updateProfileWithOrganUberonIdsAndModelOrganism( user,
                    userUpdatedProfileModel.getProfile(),
                    userUpdatedProfileModel.getProfile().getPublications(),
                    userUpdatedProfileModel.getOrganUberonIds(),
                    userUpdatedProfileModel.getTaxonGenesPayload());


            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("updatedUser", updatedUser);

            // Your logic to update the profile
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

    @PostMapping(value = "/test/contentType", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<String> testContentType(@RequestHeader("Content-Type") String contentType, @RequestBody UserUpdatedProfileModel userUpdatedProfileModel) {
        return new ResponseEntity<>("Received Content-Type: " + contentType + "\nBody: " + userUpdatedProfileModel, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/test/contentType2", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> testContentType2(@RequestHeader("Content-Type") String contentType, @RequestBody UserUpdatedProfileModel userUpdatedProfileModel) {
        return new ResponseEntity<>("Received Content-Type: " + contentType + "\nBody: " + userUpdatedProfileModel, HttpStatus.OK);
    }

//    @ResponseBody
//    @RequestMapping(method = RequestMethod.POST, value = "/test/contentType3", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PostMapping(value = "/test/contentType3", consumes = {"*/*"})
    public ResponseEntity<String> testContentType3(@RequestHeader("Content-Type") String contentType, @RequestBody UserUpdatedProfileModel userUpdatedProfileModel) {
        return new ResponseEntity<>("Received Content-Type: " + contentType + "\nBody: " + userUpdatedProfileModel, HttpStatus.OK);
    }


}
