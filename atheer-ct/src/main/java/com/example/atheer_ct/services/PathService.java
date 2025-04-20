package com.example.atheer_ct.services;

import com.example.atheer_ct.dto.TowerDto;
import com.example.atheer_ct.entities.Tower;
import com.example.atheer_ct.repo.TowerRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PathService {

    private final TowerRepository towerRepository;
    private final double MAX_TOWER_DISTANCE = 10.1; // Strict 10km constraint

    public PathService(TowerRepository towerRepository) {
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

        // If no towers in database, return only virtual towers
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

        // Try BFS for fewest towers solution
        List<TowerDto> path = findMinimumTowerPath(startTower, endTower, allTowers);

        // If no valid path found, try the interpolation approach
        if (path.size() <= 2) {
            path = findPathByInterpolation(startTower, endTower, allTowers);
        }

        // Validate all segments in the path
        Map<String, Object> validationResult = validateAllPathSegments(path);
        if (validationResult.containsKey("error")) {
            return validationResult;
        }

        result.put("path", path);
        return result;
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
        } else {
            return "tower " + (tower.getTawalId() != null ? tower.getTawalId() : "(unnamed)");
        }
    }

    private List<TowerDto> findMinimumTowerPath(TowerDto start, TowerDto end, List<TowerDto> allTowers) {
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
            return Arrays.asList(start, end);
        }

        // Build path from end to start
        while (currentId != null) {
            path.add(0, towerMap.get(currentId));
            currentId = previous.get(currentId);
        }

        return path;
    }

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
        int numSegments = (int) Math.ceil(directDistance / (MAX_TOWER_DISTANCE * 0.7));

        // Track the current position as we build the path
        double currentLat = start.getLatitude();
        double currentLon = start.getLongitude();

        // For each segment
        for (int i = 0; i < numSegments; i++) {
            // Find the next ideal point along the path
            double segmentDistance = Math.min(MAX_TOWER_DISTANCE * 0.7,
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
            TowerDto bestTower = null;
            double bestScore = Double.MAX_VALUE;

            for (TowerDto tower : allTowers) {
                // Skip towers we've already used
                if (path.stream().anyMatch(t -> getTowerId(t).equals(getTowerId(tower)))) {
                    continue;
                }

                // Check if this tower is within range of our current position
                double distFromCurrent = calculateDistance(
                        currentLat, currentLon,
                        tower.getLatitude(), tower.getLongitude()
                );

                if (distFromCurrent <= MAX_TOWER_DISTANCE) {
                    // Calculate how far this tower is from our ideal path
                    double distFromIdealPoint = calculateDistance(
                            nextPoint[0], nextPoint[1],
                            tower.getLatitude(), tower.getLongitude()
                    );

                    // Calculate progress toward end
                    double progressTowardEnd = calculateDistance(
                            start.getLatitude(), start.getLongitude(),
                            tower.getLatitude(), tower.getLongitude()
                    );

                    // Calculate deviation from ideal path
                    double idealBearing = calculateBearing(
                            start.getLatitude(), start.getLongitude(),
                            end.getLatitude(), end.getLongitude()
                    );

                    double towerBearing = calculateBearing(
                            start.getLatitude(), start.getLongitude(),
                            tower.getLatitude(), tower.getLongitude()
                    );

                    double bearingDiff = Math.abs(idealBearing - towerBearing);
                    if (bearingDiff > 180) bearingDiff = 360 - bearingDiff;

                    // Create a composite score - prefer towers that are:
                    // 1. Close to the ideal point
                    // 2. Make good progress toward the end
                    // 3. Don't deviate too much from the ideal bearing
                    double score = (distFromIdealPoint * 0.5) +
                            (bearingDiff * 0.3) -
                            (progressTowardEnd * 0.2);

                    if (score < bestScore) {
                        bestScore = score;
                        bestTower = tower;
                    }
                }
            }

            // If we found a suitable tower, add it and update our current position
            if (bestTower != null) {
                path.add(bestTower);
                currentLat = bestTower.getLatitude();
                currentLon = bestTower.getLongitude();
            } else {
                // No suitable tower found, try a different approach
                // Create a virtual tower at the ideal point
                double distToNext = calculateDistance(
                        currentLat, currentLon, nextPoint[0], nextPoint[1]
                );

                if (distToNext > 0.1) { // Only move if we're making significant progress
                    currentLat = nextPoint[0];
                    currentLon = nextPoint[1];
                } else {
                    // We're stuck, try to find any available tower that gets us closer to the end
                    TowerDto closestToEnd = findTowerClosestToEnd(
                            currentLat, currentLon, end, allTowers, path);

                    if (closestToEnd != null) {
                        path.add(closestToEnd);
                        currentLat = closestToEnd.getLatitude();
                        currentLon = closestToEnd.getLongitude();
                    } else {
                        // If all else fails, we'll just have to add the end and accept a longer segment
                        path.add(end);
                        break;
                    }
                }
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

        // Verify the path has valid segments
        path = validateAndFixPath(path, allTowers);

        return path;
    }

    private TowerDto findTowerClosestToEnd(double currentLat, double currentLon,
                                           TowerDto end, List<TowerDto> allTowers, List<TowerDto> existingPath) {
        TowerDto bestTower = null;
        double bestDistance = Double.MAX_VALUE;

        for (TowerDto tower : allTowers) {
            // Skip if already in the path
            if (existingPath.stream().anyMatch(t -> getTowerId(t).equals(getTowerId(tower)))) {
                continue;
            }

            // Check if within range of current position
            double distFromCurrent = calculateDistance(
                    currentLat, currentLon,
                    tower.getLatitude(), tower.getLongitude()
            );

            if (distFromCurrent <= MAX_TOWER_DISTANCE) {
                // Calculate distance to end
                double distToEnd = calculateDistance(
                        tower.getLatitude(), tower.getLongitude(),
                        end.getLatitude(), end.getLongitude()
                );

                if (distToEnd < bestDistance) {
                    bestDistance = distToEnd;
                    bestTower = tower;
                }
            }
        }

        return bestTower;
    }

    private List<TowerDto> validateAndFixPath(List<TowerDto> path, List<TowerDto> allTowers) {
        if (path.size() <= 2) return path;

        List<TowerDto> validatedPath = new ArrayList<>();
        validatedPath.add(path.get(0)); // Add start

        // Check each segment
        for (int i = 1; i < path.size(); i++) {
            TowerDto prevTower = validatedPath.get(validatedPath.size() - 1);
            TowerDto currentTower = path.get(i);

            double distance = calculateDistance(
                    prevTower.getLatitude(), prevTower.getLongitude(),
                    currentTower.getLatitude(), currentTower.getLongitude()
            );

            if (distance <= MAX_TOWER_DISTANCE) {
                // Segment is valid, add the current tower
                validatedPath.add(currentTower);
            } else {
                // Segment is too long, need to insert towers
                List<TowerDto> fixedSegment = fixSegment(prevTower, currentTower, allTowers, path);

                // Add all towers except the first one (which is already in validatedPath)
                validatedPath.addAll(fixedSegment.subList(1, fixedSegment.size()));
            }
        }

        return validatedPath;
    }

    private List<TowerDto> fixSegment(TowerDto start, TowerDto end, List<TowerDto> allTowers, List<TowerDto> existingPath) {
        // Find intermediate towers for this segment
        List<TowerDto> segment = new ArrayList<>();
        segment.add(start);

        // Calculate direct distance
        double directDistance = calculateDistance(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()
        );

        // If we need multiple hops
        if (directDistance > MAX_TOWER_DISTANCE) {
            // Try to find a set of towers that create a valid path
            List<TowerDto> candidates = allTowers.stream()
                    .filter(t -> !existingPath.contains(t) || t.equals(start) || t.equals(end))
                    .filter(t -> {
                        double distFromStart = calculateDistance(
                                start.getLatitude(), start.getLongitude(),
                                t.getLatitude(), t.getLongitude()
                        );

                        double distToEnd = calculateDistance(
                                t.getLatitude(), t.getLongitude(),
                                end.getLatitude(), end.getLongitude()
                        );

                        return distFromStart <= MAX_TOWER_DISTANCE &&
                                distToEnd < directDistance; // Ensures we make progress
                    })
                    .sorted(Comparator.comparingDouble(t ->
                            calculateDistance(t.getLatitude(), t.getLongitude(),
                                    end.getLatitude(), end.getLongitude())))
                    .collect(Collectors.toList());

            // If we have candidates
            if (!candidates.isEmpty()) {
                TowerDto midTower = candidates.get(0);

                // Recursively fix each sub-segment if needed
                List<TowerDto> firstHalf = fixSegment(start, midTower, allTowers, existingPath);
                List<TowerDto> secondHalf = fixSegment(midTower, end, allTowers, existingPath);

                // Combine the segments (avoiding duplicate midTower)
                segment = new ArrayList<>(firstHalf);
                segment.addAll(secondHalf.subList(1, secondHalf.size()));

                return segment;
            }
        }

        // If we can't fix it or don't need to, return the original segment
        segment.add(end);
        return segment;
    }

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
}