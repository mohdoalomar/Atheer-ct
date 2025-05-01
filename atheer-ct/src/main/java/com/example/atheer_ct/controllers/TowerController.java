package com.example.atheer_ct.controllers;

import com.example.atheer_ct.dto.TowerDto;
import com.example.atheer_ct.services.CombinedPathService;
import com.example.atheer_ct.services.OldPathService;
import com.example.atheer_ct.services.POPService;
import com.example.atheer_ct.services.PathService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TowerController {
    private final PathService pathService;
    private final POPService popService;
    private final OldPathService oldPathService;
    private final CombinedPathService combinedPathService;

    public TowerController(PathService pathService, POPService popService, OldPathService oldPathService, CombinedPathService combinedPathService) {
        this.oldPathService = oldPathService;
        this.pathService = pathService;
        this.combinedPathService = combinedPathService;
        this.popService = popService;

    }
    @GetMapping("/findpath")
    public ResponseEntity<?> getShortestPath(@RequestParam  double startLat, @RequestParam double startLon, @RequestParam double endLat, @RequestParam double endLon) {
        Map<String, Object> path = combinedPathService.findShortestPath(startLat, startLon , endLat, endLon);
        if (path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No path found.");
        }
        return ResponseEntity.ok(path);
    }
    @GetMapping("/oldfindpath")
    public ResponseEntity<?> getShortestPathOld(@RequestParam  double startLat, @RequestParam double startLon, @RequestParam double endLat, @RequestParam double endLon) {
        Map<String, Object> path = oldPathService.findShortestPath(startLat, startLon , endLat, endLon);
        if (path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No path found.");
        }
        return ResponseEntity.ok(path);
    }
    @PostMapping("/pop")
    public ResponseEntity<?> getPOPMapping(
            @RequestParam double popLat,
            @RequestParam double popLon,
            @RequestParam(required = false) boolean optimizeTowers,
            @RequestBody List<Map<String, Double>> destinations) {

        try {
            Map<String, Object> result;

                result = popService.findMinimumTowerPOPPaths(popLat, popLon, destinations);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate POP mapping: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/example/alhofuf")
    public ResponseEntity<?> getAlHofufExample(@RequestParam(required = false) boolean optimizeTowers) {
        try {
            // Al Hofuf city center as the POP location
            double popLat = 25.3790;
            double popLon = 49.5883;

            // Create a list of destinations around Al Hofuf
            List<Map<String, Double>> destinations = new ArrayList<>();

            // King Fahd Hospital
            destinations.add(Map.of(
                    "latitude", 25.3713,
                    "longitude", 49.5810
            ));

            // King Faisal University
            destinations.add(Map.of(
                    "latitude", 25.3499,
                    "longitude", 49.5971
            ));

            // Al Ahsa Mall
            destinations.add(Map.of(
                    "latitude", 25.3783,
                    "longitude", 49.5549
            ));

            // Hofuf Central Market
            destinations.add(Map.of(
                    "latitude", 25.3823,
                    "longitude", 49.5922
            ));

            // Al Othaim Mall
            destinations.add(Map.of(
                    "latitude", 25.3638,
                    "longitude", 49.6012
            ));

            // Salmaniya Garden
            destinations.add(Map.of(
                    "latitude", 25.3866,
                    "longitude", 49.5994
            ));

            // Al Hofuf Airport
            destinations.add(Map.of(
                    "latitude", 25.2856,
                    "longitude", 49.4850
            ));

            // Ibrahim Palace
            destinations.add(Map.of(
                    "latitude", 25.3782,
                    "longitude", 49.5785
            ));

            Map<String, Object> result;


                // Use the advanced minimum tower optimization
                result = popService.findMinimumTowerPOPPaths(popLat, popLon, destinations);

            // Add the POP location to the result for reference
            result.put("pop", Map.of(
                    "latitude", popLat,
                    "longitude", popLon,
                    "name", "Al Hofuf City Center POP"
            ));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate Al Hofuf example: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
