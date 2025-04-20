package com.example.atheer_ct.controllers;

import com.example.atheer_ct.dto.TowerDto;
import com.example.atheer_ct.services.PathService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TowerController {
    private final PathService pathService;

    public TowerController(PathService pathService) {
        this.pathService = pathService;
    }
    @GetMapping("/findpath")
    public ResponseEntity<?> getShortestPath(@RequestParam  double startLat, @RequestParam double startLon, @RequestParam double endLat, @RequestParam double endLon) {
        Map<String, Object> path = pathService.findShortestPath(startLat, startLon , endLat, endLon);
        if (path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No path found.");
        }
        return ResponseEntity.ok(path);
    }
}
