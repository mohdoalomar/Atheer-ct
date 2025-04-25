package com.example.atheer_ct.services;

import com.example.atheer_ct.dto.TowerDto;
import com.example.atheer_ct.entities.Tower;
import com.example.atheer_ct.repo.TowerRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CombinedPathService {

    private final TowerRepository towerRepository;
    private final double MAX_TOWER_DISTANCE = 10.1; // Strict 10km constraint
    private final double BEARING_DEVIATION_PENALTY = 1.5; // Penalty for zigzag paths
    private final double DIRECT_PATH_WEIGHT = 0.8; // Lower than in PathService to prioritize tower count

    public CombinedPathService(TowerRepository towerRepository) {
        this.towerRepository = towerRepository;
    }

    public Map<String, Object> findShortestPath(double startLat, double startLon, double endLat, double endLon) {
        Map<String, Object> result = new HashMap<>();

        // Create virtual towers for start and end points
        TowerDto startTower = TowerDto.builder()
                .tawalId("START_VIRTUAL")
                .siteName("Virtual Start Tower")
                .latitude(startLat)
                .longitude(startLon)
                .build();

        TowerDto endTower = TowerDto.builder()
                .tawalId("END_VIRTUAL")
                .siteName("Virtual End Tower")
                .latitude(endLat)
                .longitude(endLon)
                .build();

        // Calculate direct distance between start and end points
        double directDistance = calculateDistance(startLat, startLon, endLat, endLon);
        System.out.println("Direct distance: " + directDistance);

        // If direct distance is less than MAX_TOWER_DISTANCE, return only virtual towers
        if (directDistance <= MAX_TOWER_DISTANCE) {
            result.put("path", Arrays.asList(startTower, endTower));
            return result;
        }

        // Get all towers from the database
        List<Tower> dbTowers = towerRepository.findAll();

        // If no towers in database, return only virtual towers or error
        if (dbTowers.isEmpty()) {
            // Check if we can make a direct connection
            if (directDistance <= MAX_TOWER_DISTANCE) {
                result.put("path", Arrays.asList(startTower, endTower));
                return result;
            } else {
                result.put("error", "Cannot create path. The direct distance between start and end points ("
                        + String.format("%.2f", directDistance) + " km) exceeds the maximum allowed distance of "
                        + MAX_TOWER_DISTANCE + " km, and no towers are available.");
                return result;
            }
        }

        // Convert database towers to DTOs
        List<TowerDto> allTowers = dbTowers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // COMBINED APPROACH:

        // 1. First find path with minimum tower count (using approach from OldPathService)
        List<TowerDto> minTowerPath = findMinimumTowerCountPath(startTower, endTower, allTowers);

        // 2. Then find path with most direct route (using approach from PathService)
        List<TowerDto> directPath = findDirectPath(startTower, endTower, allTowers);

        // 3. Compare tower counts - if equal, use the direct path; otherwise use min tower path
        List<TowerDto> selectedPath;

        if (directPath.size() <= minTowerPath.size()) {
            System.out.println("Selected direct path with " + directPath.size() + " towers");
            selectedPath = directPath;
        } else {
            System.out.println("Selected minimum tower path with " + minTowerPath.size() +
                    " towers instead of direct path with " + directPath.size() + " towers");
            selectedPath = minTowerPath;
        }

        // Validate the selected path
        Map<String, Object> validationResult = validateAllPathSegments(selectedPath);
        if (validationResult.containsKey("error")) {
            return validationResult;
        }

        result.put("path", selectedPath);
        return result;
    }

    /**
     * Find path with minimum number of towers (from OldPathService approach)
     */
    private List<TowerDto> findMinimumTowerCountPath(TowerDto start, TowerDto end, List<TowerDto> allTowers) {
        // Create a map of all nodes for quick lookup
        Map<String, TowerDto> towerMap = new HashMap<>();
        towerMap.put(getTowerId(start), start);
        towerMap.put(getTowerId(end), end);
        allTowers.forEach(t -> towerMap.put(getTowerId(t), t));

        // BFS for shortest path (fewest hops)
        Queue<String> queue = new LinkedList<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();

        String startId = getTowerId(start);
        String endId = getTowerId(end);

        queue.add(startId);
        visited.add(startId);

        boolean pathFound = false;

        while (!queue.isEmpty() && !pathFound) {
            String currentId = queue.poll();
            TowerDto currentTower = towerMap.get(currentId);

            if (currentId.equals(endId)) {
                pathFound = true;
                break;
            }

            // Find all possible next hops that are within range
            List<String> neighbors = new ArrayList<>();

            for (String towerId : towerMap.keySet()) {
                if (towerId.equals(currentId)) continue;

                TowerDto potentialNeighbor = towerMap.get(towerId);
                double distance = calculateDistance(
                        currentTower.getLatitude(), currentTower.getLongitude(),
                        potentialNeighbor.getLatitude(), potentialNeighbor.getLongitude()
                );

                if (distance <= MAX_TOWER_DISTANCE) {
                    neighbors.add(towerId);
                }
            }

            // Sort neighbors by their proximity to the end point (greedy approach)
            neighbors.sort((a, b) -> {
                TowerDto towerA = towerMap.get(a);
                TowerDto towerB = towerMap.get(b);

                double distA = calculateDistance(
                        towerA.getLatitude(), towerA.getLongitude(),
                        end.getLatitude(), end.getLongitude()
                );

                double distB = calculateDistance(
                        towerB.getLatitude(), towerB.getLongitude(),
                        end.getLatitude(), end.getLongitude()
                );

                return Double.compare(distA, distB);
            });

            // Process all neighbors
            for (String neighborId : neighbors) {
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    previous.put(neighborId, currentId);
                    queue.add(neighborId);
                }
            }
        }

        // Reconstruct path
        List<TowerDto> path = new ArrayList<>();
        String currentId = endId;

        // If no path to end found
        if (!visited.contains(endId)) {
            // Fall back to interpolation
            return findPathByInterpolation(start, end, allTowers);
        }

        // Build path from end to start
        while (currentId != null) {
            path.add(0, towerMap.get(currentId));
            currentId = previous.get(currentId);
        }

        return path;
    }

    /**
     * Find direct path with A* algorithm (from newer PathService approach)
     */
    private List<TowerDto> findDirectPath(TowerDto start, TowerDto end, List<TowerDto> allTowers) {
        // Create a map of all nodes for quick lookup
        Map<String, TowerDto> towerMap = new HashMap<>();
        towerMap.put(getTowerId(start), start);
        towerMap.put(getTowerId(end), end);
        allTowers.forEach(t -> towerMap.put(getTowerId(t), t));

        String startId = getTowerId(start);
        String endId = getTowerId(end);

        // Track visited nodes and path construction
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Map<String, Double> distanceSoFar = new HashMap<>(); // Track distance traveled to each node

        // A* algorithm implementation
        PriorityQueue<String> queue = new PriorityQueue<>((a, b) -> {
            TowerDto towerA = towerMap.get(a);
            TowerDto towerB = towerMap.get(b);

            // Distance to end (heuristic component)
            double distA = calculateDistance(
                    towerA.getLatitude(), towerA.getLongitude(),
                    end.getLatitude(), end.getLongitude()
            );

            double distB = calculateDistance(
                    towerB.getLatitude(), towerB.getLongitude(),
                    end.getLatitude(), end.getLongitude()
            );

            // Get path distances so far
            double pathDistA = distanceSoFar.getOrDefault(a, Double.MAX_VALUE);
            double pathDistB = distanceSoFar.getOrDefault(b, Double.MAX_VALUE);

            // Calculate bearing deviations to prefer straighter paths
            double bearingDeviationA = 0;
            double bearingDeviationB = 0;

            // Calculate ideal bearing from start to end
            double idealBearing = calculateBearing(
                    start.getLatitude(), start.getLongitude(),
                    end.getLatitude(), end.getLongitude()
            );

            if (previous.containsKey(a)) {
                String prevId = previous.get(a);
                TowerDto prevTower = towerMap.get(prevId);

                double segmentBearing = calculateBearing(
                        prevTower.getLatitude(), prevTower.getLongitude(),
                        towerA.getLatitude(), towerA.getLongitude()
                );

                bearingDeviationA = Math.abs(segmentBearing - idealBearing);
                if (bearingDeviationA > 180) bearingDeviationA = 360 - bearingDeviationA;
            }

            if (previous.containsKey(b)) {
                String prevId = previous.get(b);
                TowerDto prevTower = towerMap.get(prevId);

                double segmentBearing = calculateBearing(
                        prevTower.getLatitude(), prevTower.getLongitude(),
                        towerB.getLatitude(), towerB.getLongitude()
                );

                bearingDeviationB = Math.abs(segmentBearing - idealBearing);
                if (bearingDeviationB > 180) bearingDeviationB = 360 - bearingDeviationB;
            }

            // Combine the metrics with appropriate weights
            // Higher weight on distance to end (heuristic) to prefer more direct paths
            double scoreA = pathDistA + (distA * 1.5) + (bearingDeviationA * 0.1);
            double scoreB = pathDistB + (distB * 1.5) + (bearingDeviationB * 0.1);

            return Double.compare(scoreA, scoreB);
        });

        // Initialize search
        queue.add(startId);
        visited.add(startId);
        distanceSoFar.put(startId, 0.0);

        boolean pathFound = false;

        while (!queue.isEmpty() && !pathFound) {
            String currentId = queue.poll();
            TowerDto currentTower = towerMap.get(currentId);

            if (currentId.equals(endId)) {
                pathFound = true;
                break;
            }

            // Find all possible next hops that are within range
            List<String> neighbors = new ArrayList<>();

            for (String towerId : towerMap.keySet()) {
                if (towerId.equals(currentId)) continue;

                TowerDto potentialNeighbor = towerMap.get(towerId);
                double distance = calculateDistance(
                        currentTower.getLatitude(), currentTower.getLongitude(),
                        potentialNeighbor.getLatitude(), potentialNeighbor.getLongitude()
                );

                if (distance <= MAX_TOWER_DISTANCE) {
                    // Check if this tower makes progress toward the destination
                    double currentToEndDist = calculateDistance(
                            currentTower.getLatitude(), currentTower.getLongitude(),
                            end.getLatitude(), end.getLongitude()
                    );

                    double neighborToEndDist = calculateDistance(
                            potentialNeighbor.getLatitude(), potentialNeighbor.getLongitude(),
                            end.getLatitude(), end.getLongitude()
                    );

                    // Only consider towers that don't take us too far off course
                    if (neighborToEndDist <= currentToEndDist + 3.0) {
                        neighbors.add(towerId);
                    }
                }
            }

            // Process all neighbors
            for (String neighborId : neighbors) {
                // Calculate new distance
                TowerDto neighbor = towerMap.get(neighborId);
                double segmentDistance = calculateDistance(
                        currentTower.getLatitude(), currentTower.getLongitude(),
                        neighbor.getLatitude(), neighbor.getLongitude()
                );

                double newDistance = distanceSoFar.get(currentId) + segmentDistance;

                // If we haven't visited this node or we found a shorter path
                if (!visited.contains(neighborId) || newDistance < distanceSoFar.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    visited.add(neighborId);
                    previous.put(neighborId, currentId);
                    distanceSoFar.put(neighborId, newDistance);

                    // Add to queue for processing
                    queue.add(neighborId);
                }
            }
        }

        // Reconstruct path
        List<TowerDto> path = new ArrayList<>();
        String currentId = endId;

        // If no path to end found
        if (!visited.contains(endId)) {
            // Fall back to interpolation approach
            return findPathByInterpolation(start, end, allTowers);
        }

        // Build path from end to start
        while (currentId != null) {
            path.add(0, towerMap.get(currentId));
            currentId = previous.get(currentId);
        }

        // Apply smoothing to eliminate zigzags
        path = smoothPath(path, allTowers);

        return path;
    }

    /**
     * Smooth the path to reduce zigzags while maintaining connectivity
     */
    private List<TowerDto> smoothPath(List<TowerDto> path, List<TowerDto> allTowers) {
        if (path.size() <= 3) {
            return path; // Nothing to smooth for very short paths
        }

        List<TowerDto> smoothedPath = new ArrayList<>(path);
        boolean changed;

        // Multiple iterations to incrementally improve the path
        for (int iteration = 0; iteration < 2; iteration++) {
            changed = false;

            // Look at each triplet of consecutive towers
            for (int i = 0; i < smoothedPath.size() - 2; i++) {
                TowerDto t1 = smoothedPath.get(i);
                TowerDto t2 = smoothedPath.get(i + 1);
                TowerDto t3 = smoothedPath.get(i + 2);

                // Check if we can skip the middle tower
                double distDirectly = calculateDistance(
                        t1.getLatitude(), t1.getLongitude(),
                        t3.getLatitude(), t3.getLongitude()
                );

                if (distDirectly <= MAX_TOWER_DISTANCE) {
                    // We can skip t2 entirely
                    smoothedPath.remove(i + 1);
                    changed = true;
                    i--; // Adjust index after removal
                    continue;
                }

                // Check for zigzag patterns
                double bearing1to2 = calculateBearing(
                        t1.getLatitude(), t1.getLongitude(),
                        t2.getLatitude(), t2.getLongitude()
                );

                double bearing2to3 = calculateBearing(
                        t2.getLatitude(), t2.getLongitude(),
                        t3.getLatitude(), t3.getLongitude()
                );

                double bearingChange = Math.abs(bearing1to2 - bearing2to3);
                if (bearingChange > 180) bearingChange = 360 - bearingChange;

                // If significant direction change, look for a better middle tower
                if (bearingChange > 45) {
                    // Calculate the ideal direct bearing
                    double directBearing = calculateBearing(
                            t1.getLatitude(), t1.getLongitude(),
                            t3.getLatitude(), t3.getLongitude()
                    );

                    // Find a better tower that creates a straighter path
                    TowerDto betterTower = findBetterMiddleTower(t1, t3, allTowers, smoothedPath);

                    if (betterTower != null && !getTowerId(betterTower).equals(getTowerId(t2))) {
                        smoothedPath.set(i + 1, betterTower);
                        changed = true;
                    }
                }
            }

            // If no changes made in this iteration, we're done
            if (!changed) break;
        }

        // Ensure we maintain connectivity
        boolean valid = true;
        for (int i = 0; i < smoothedPath.size() - 1; i++) {
            TowerDto current = smoothedPath.get(i);
            TowerDto next = smoothedPath.get(i + 1);

            double distance = calculateDistance(
                    current.getLatitude(), current.getLongitude(),
                    next.getLatitude(), next.getLongitude()
            );

            if (distance > MAX_TOWER_DISTANCE) {
                valid = false;
                break;
            }
        }

        // Return the original path if smoothing broke connectivity
        return valid ? smoothedPath : path;
    }

    /**
     * Find a better middle tower to reduce zigzag
     */
    private TowerDto findBetterMiddleTower(TowerDto t1, TowerDto t3, List<TowerDto> allTowers, List<TowerDto> currentPath) {
        double directBearing = calculateBearing(
                t1.getLatitude(), t1.getLongitude(),
                t3.getLatitude(), t3.getLongitude()
        );

        // Calculate midpoint along direct line as ideal position
        double midLat = (t1.getLatitude() + t3.getLatitude()) / 2;
        double midLon = (t1.getLongitude() + t3.getLongitude()) / 2;

        TowerDto bestTower = null;
        double bestScore = Double.MAX_VALUE;

        for (TowerDto tower : allTowers) {
            // Skip towers already in the path (except possibly at the middle position)
            if (currentPath.contains(tower) &&
                    !(currentPath.indexOf(tower) == currentPath.indexOf(t1) + 1)) {
                continue;
            }

            // Check if this tower is within range of both t1 and t3
            double distFromT1 = calculateDistance(
                    t1.getLatitude(), t1.getLongitude(),
                    tower.getLatitude(), tower.getLongitude()
            );

            double distToT3 = calculateDistance(
                    tower.getLatitude(), tower.getLongitude(),
                    t3.getLatitude(), t3.getLongitude()
            );

            if (distFromT1 <= MAX_TOWER_DISTANCE && distToT3 <= MAX_TOWER_DISTANCE) {
                // Calculate distance from ideal midpoint
                double distFromMidpoint = calculateDistance(
                        tower.getLatitude(), tower.getLongitude(),
                        midLat, midLon
                );

                // Calculate bearing deviations
                double bearingT1ToTower = calculateBearing(
                        t1.getLatitude(), t1.getLongitude(),
                        tower.getLatitude(), tower.getLongitude()
                );

                double bearingTowerToT3 = calculateBearing(
                        tower.getLatitude(), tower.getLongitude(),
                        t3.getLatitude(), t3.getLongitude()
                );

                double bearingDeviation1 = Math.abs(directBearing - bearingT1ToTower);
                if (bearingDeviation1 > 180) bearingDeviation1 = 360 - bearingDeviation1;

                double bearingDeviation2 = Math.abs(directBearing - bearingTowerToT3);
                if (bearingDeviation2 > 180) bearingDeviation2 = 360 - bearingDeviation2;

                // Score based on distance from midpoint and bearing smoothness
                double score = distFromMidpoint + (bearingDeviation1 * 0.2) + (bearingDeviation2 * 0.2);

                if (score < bestScore) {
                    bestScore = score;
                    bestTower = tower;
                }
            }
        }

        return bestTower;
    }

    /**
     * Fall back method for interpolation when BFS can't find a path
     */
    private List<TowerDto> findPathByInterpolation(TowerDto start, TowerDto end, List<TowerDto> allTowers) {
        List<TowerDto> path = new ArrayList<>();
        path.add(start);

        // Calculate direct distance and bearing
        double directDistance = calculateDistance(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()
        );

        double bearing = calculateBearing(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()
        );

        // Calculate number of segments needed based on max distance
        // Using 60% of max distance for more stable connections
        int numSegments = (int) Math.ceil(directDistance / (MAX_TOWER_DISTANCE * 0.6));

        // Track the current position as we build the path
        double currentLat = start.getLatitude();
        double currentLon = start.getLongitude();

        // For each segment
        for (int i = 0; i < numSegments; i++) {
            // Find the next ideal point along the path
            double segmentDistance = Math.min(MAX_TOWER_DISTANCE * 0.6,
                    calculateDistance(currentLat, currentLon, end.getLatitude(), end.getLongitude()));

            double[] nextPoint = calculateDestinationPoint(currentLat, currentLon, bearing, segmentDistance);

            // If we're close to the end, just add the end tower and break
            double distToEnd = calculateDistance(nextPoint[0], nextPoint[1],
                    end.getLatitude(), end.getLongitude());

            if (distToEnd <= MAX_TOWER_DISTANCE) {
                path.add(end);
                break;
            }

            // Find the closest tower to this ideal point that satisfies our constraints
            TowerDto bestTower = findClosestTowerToIdealPoint(
                    currentLat, currentLon, nextPoint[0], nextPoint[1],
                    end, bearing, allTowers, path);

            // If we found a suitable tower, add it and update our current position
            if (bestTower != null) {
                path.add(bestTower);
                currentLat = bestTower.getLatitude();
                currentLon = bestTower.getLongitude();
            } else {
                // No suitable tower found, create a virtual one at the ideal point
                TowerDto virtualTower = TowerDto.builder()
                        .tawalId("VIRTUAL_" + i)
                        .siteName("Virtual Intermediate Tower " + i)
                        .latitude(nextPoint[0])
                        .longitude(nextPoint[1])
                        .build();

                path.add(virtualTower);
                currentLat = nextPoint[0];
                currentLon = nextPoint[1];
            }

            // Check if we can reach the end from our new position
            double newDistToEnd = calculateDistance(
                    currentLat, currentLon,
                    end.getLatitude(), end.getLongitude()
            );

            if (newDistToEnd <= MAX_TOWER_DISTANCE) {
                path.add(end);
                break;
            }
        }

        // Ensure the end tower is included
        if (!getTowerId(path.get(path.size() - 1)).equals(getTowerId(end))) {
            path.add(end);
        }

        return path;
    }

    private TowerDto findClosestTowerToIdealPoint(
            double fromLat, double fromLon, double idealLat, double idealLon,
            TowerDto end, double directBearing, List<TowerDto> allTowers, List<TowerDto> existingPath) {

        TowerDto bestTower = null;
        double bestScore = Double.MAX_VALUE;

        for (TowerDto tower : allTowers) {
            // Skip towers we've already used
            if (existingPath.stream().anyMatch(t -> getTowerId(t).equals(getTowerId(tower)))) {
                continue;
            }

            // Check if this tower is within range of our current position
            double distFromCurrent = calculateDistance(
                    fromLat, fromLon,
                    tower.getLatitude(), tower.getLongitude()
            );

            if (distFromCurrent <= MAX_TOWER_DISTANCE) {
                // Calculate distance from ideal point
                double distFromIdeal = calculateDistance(
                        idealLat, idealLon,
                        tower.getLatitude(), tower.getLongitude()
                );

                // Calculate bearing deviation
                double towerBearing = calculateBearing(
                        fromLat, fromLon,
                        tower.getLatitude(), tower.getLongitude()
                );

                double bearingDiff = Math.abs(directBearing - towerBearing);
                if (bearingDiff > 180) bearingDiff = 360 - bearingDiff;

                // Calculate progress toward end
                double progressTowardEnd = calculateDistance(
                        tower.getLatitude(), tower.getLongitude(),
                        end.getLatitude(), end.getLongitude()
                );

                // Combined score (lower is better)
                double score = distFromIdeal * 0.7 + bearingDiff * 0.2 + progressTowardEnd * 0.1;

                if (score < bestScore) {
                    bestScore = score;
                    bestTower = tower;
                }
            }
        }

        return bestTower;
    }

    /**
     * Validates all segments in the path to ensure each tower-to-tower distance is within limits
     * @param path The path to validate
     * @return A map containing either an error message or nothing if path is valid
     */
    private Map<String, Object> validateAllPathSegments(List<TowerDto> path) {
        Map<String, Object> result = new HashMap<>();

        if (path.size() < 2) {
            return result; // Path is too short to validate
        }

        for (int i = 0; i < path.size() - 1; i++) {
            TowerDto currentTower = path.get(i);
            TowerDto nextTower = path.get(i + 1);

            double segmentDistance = calculateDistance(
                    currentTower.getLatitude(), currentTower.getLongitude(),
                    nextTower.getLatitude(), nextTower.getLongitude()
            );

            if (segmentDistance > MAX_TOWER_DISTANCE) {
                result.put("error", "Cannot complete the path. The distance between " +
                        describeLocation(currentTower) + " and " + describeLocation(nextTower) +
                        " (" + String.format("%.2f", segmentDistance) + " km) exceeds the maximum allowed distance of " +
                        MAX_TOWER_DISTANCE + " km.");
                break;
            }
        }

        return result;
    }

    /**
     * Helper method to generate a descriptive string for a tower location
     */
    private String describeLocation(TowerDto tower) {
        if ("START_VIRTUAL".equals(tower.getTawalId())) {
            return "starting point";
        } else if ("END_VIRTUAL".equals(tower.getTawalId())) {
            return "destination point";
        } else if (tower.getTawalId() != null && tower.getTawalId().startsWith("VIRTUAL_")) {
            return "virtual intermediate point";
        } else {
            return "tower " + (tower.getTawalId() != null ? tower.getTawalId() : "(unnamed)");
        }
    }

    private String getTowerId(TowerDto tower) {
        if (tower.getId() != null) {
            return tower.getId().toString();
        }
        return tower.getTawalId() != null ? tower.getTawalId() :
                tower.getLatitude() + ":" + tower.getLongitude();
    }

    // Convert Tower entity to TowerDto
    private TowerDto convertToDto(Tower tower) {
        return TowerDto.builder()
                .id(tower.getId())
                .tawalId(tower.getTawalId())
                .siteName(tower.getSiteName())
                .latitude(tower.getLatitude())
                .longitude(tower.getLongitude())
                .totalHeight(tower.getTotalHeight())
                .power(tower.getPower())
                .clutter(tower.getClutter())
                .build();
    }

    // Calculate distance between two coordinates using Haversine formula
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }

    // Calculate bearing (direction) from point 1 to point 2 in degrees
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);

        double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);

        double bearing = Math.atan2(y, x);

        return (Math.toDegrees(bearing) + 360) % 360;
    }

    // Calculate destination point given a starting point, bearing and distance
    private double[] calculateDestinationPoint(double lat, double lon, double bearing, double distance) {
        final int R = 6371; // Earth radius in km

        // Convert to radians
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);
        bearing = Math.toRadians(bearing);

        // Calculate new position
        double lat2 = Math.asin(Math.sin(lat) * Math.cos(distance / R) +
                Math.cos(lat) * Math.sin(distance / R) * Math.cos(bearing));

        double lon2 = lon + Math.atan2(Math.sin(bearing) * Math.sin(distance / R) * Math.cos(lat),
                Math.cos(distance / R) - Math.sin(lat) * Math.sin(lat2));

        // Convert back to degrees
        lat2 = Math.toDegrees(lat2);
        lon2 = Math.toDegrees(lon2);

        return new double[] {lat2, lon2};
    }
}