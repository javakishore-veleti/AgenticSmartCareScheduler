package com.agenticcare.web.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/smart-care/api/admin/v1/aws-profiles")
@Tag(name = "Admin - AWS Profiles", description = "Scan and list local AWS CLI profiles")
public class AdminAwsProfilesController {

    private static final Logger log = LoggerFactory.getLogger(AdminAwsProfilesController.class);

    @GetMapping("/scan")
    @Operation(summary = "Scan local ~/.aws/config for configured profiles")
    public ResponseEntity<List<Map<String, String>>> scanProfiles() {
        log.info(">>> GET /aws-profiles/scan called");
        List<Map<String, String>> profiles = new ArrayList<>();

        File configFile = new File(System.getProperty("user.home") + "/.aws/config");
        if (!configFile.exists()) {
            log.warn("~/.aws/config not found");
            return ResponseEntity.ok(profiles);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            String currentProfile = null;
            String currentRegion = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[profile ")) {
                    if (currentProfile != null) {
                        profiles.add(Map.of("profileName", currentProfile, "region", currentRegion != null ? currentRegion : ""));
                    }
                    currentProfile = line.replace("[profile ", "").replace("]", "").trim();
                    currentRegion = null;
                } else if (line.startsWith("[default]")) {
                    if (currentProfile != null) {
                        profiles.add(Map.of("profileName", currentProfile, "region", currentRegion != null ? currentRegion : ""));
                    }
                    currentProfile = "default";
                    currentRegion = null;
                } else if (line.startsWith("region") && currentProfile != null) {
                    currentRegion = line.split("=")[1].trim();
                }
            }
            if (currentProfile != null) {
                profiles.add(Map.of("profileName", currentProfile, "region", currentRegion != null ? currentRegion : ""));
            }
        } catch (IOException e) {
            log.error("Error reading ~/.aws/config", e);
        }

        log.info("Found {} AWS profiles", profiles.size());
        return ResponseEntity.ok(profiles);
    }
}
