package com.example.atheer_ct.services;

import com.example.atheer_ct.dto.TowerDto;
import com.example.atheer_ct.entities.Tower;
import com.example.atheer_ct.repo.TowerRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class POPService {

    private final OldPathService oldPathService;
    private final TowerRepository towerRepository;

    public POPService(OldPathService oldPathService, TowerRepository towerRepository) {
        this.oldPathService = oldPathService;
        this.towerRepository = towerRepository;
    }

    /**
     * Finds optimized paths from a single POP (Point of Presence) to multiple destinations
     * while trying to minimize the total number of towers used
     *
     * @param popLat POP latitude
     * @param popLon POP longitude
     * @param destinationPoints List of destination points to connect to
     * @return Map containing the optimized paths and statistics
     */


    /**
     * Optimized version that creates a network structure connecting to all destinations
     * while minimizing the total tower count, treating destinations as receivers only
     */
    public Map<String, Object> findMinimumTowerPOPPaths(double popLat, double popLon,
                                                        List<Map<String, Double>> destinationPoints) {
        Map<String, Object> result = new HashMap<>();

        // Get all towers from database
        List<Tower> dbTowers = towerRepository.findAll();
        List<TowerDto> allTowers = dbTowers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // Create virtual tower for POP point
        TowerDto popTower = TowerDto.builder()
                .tawalId("POP_VIRTUAL")
                .siteName("Virtual POP Tower")
                .latitude(popLat)
                .longitude(popLon)
                .build();

        // Quick reachability check without full path calculation
        List<Map<String, Double>> reachableDestinations = new ArrayList<>();
        List<Map<String, Object>> unreachableDestinations = new ArrayList<>();

        final double MAX_TOWER_DISTANCE = 10.1;

        // First, identify towers within range of the POP (do this once)
        Set<TowerDto> towersWithinRangeOfPOP = new HashSet<>();
        for (TowerDto tower : allTowers) {
            double distFromPop = calculateDistance(
                    popLat, popLon,
                    tower.getLatitude(), tower.getLongitude()
            );

            if (distFromPop <= MAX_TOWER_DISTANCE) {
                towersWithinRangeOfPOP.add(tower);
            }
        }

        for (Map<String, Double> dest : destinationPoints) {
            double destLat = dest.get("latitude");
            double destLon = dest.get("longitude");

            // STEP 1: Check if direct connection is possible
            double directDistance = calculateDistance(popLat, popLon, destLat, destLon);
            if (directDistance <= MAX_TOWER_DISTANCE) {
                // Directly reachable
                reachableDestinations.add(dest);
                continue;
            }

            // STEP 2: Check if any single tower can connect POP to destination
            boolean canReachWithOneTower = false;
            for (TowerDto tower : towersWithinRangeOfPOP) {
                double distToDest = calculateDistance(
                        tower.getLatitude(), tower.getLongitude(),
                        destLat, destLon
                );

                if (distToDest <= MAX_TOWER_DISTANCE) {
                    canReachWithOneTower = true;
                    break;
                }
            }

            if (canReachWithOneTower) {
                reachableDestinations.add(dest);
                continue;
            }

            // STEP 3: Check if destination is within range of ANY tower
            // This is different from Step 2 because we're checking all towers,
            // not just those within range of the POP
            boolean destWithinRangeOfAnyTower = false;
            for (TowerDto tower : allTowers) {
                // Skip towers we already checked in Step 2
                if (towersWithinRangeOfPOP.contains(tower)) {
                    continue;
                }

                double distToDest = calculateDistance(
                        tower.getLatitude(), tower.getLongitude(),
                        destLat, destLon
                );

                if (distToDest <= MAX_TOWER_DISTANCE) {
                    destWithinRangeOfAnyTower = true;
                    break;
                }
            }

            // STEP 4: Do full path calculation only when needed
            if (destWithinRangeOfAnyTower) {
                // A tower can reach the destination, but we need to check if there's a full path
                Map<String, Object> pathResult = oldPathService.findShortestPath(
                        popLat, popLon, destLat, destLon
                );

                if (!pathResult.containsKey("error")) {
                    reachableDestinations.add(dest);
                } else {
                    Map<String, Object> unreachableInfo = new HashMap<>();
                    unreachableInfo.put("destination", dest);
                    unreachableInfo.put("reason", pathResult.get("error"));
                    unreachableDestinations.add(unreachableInfo);
                }
            } else {
                // No tower can reach the destination, so it's definitely unreachable
                Map<String, Object> unreachableInfo = new HashMap<>();
                unreachableInfo.put("destination", dest);
                unreachableInfo.put("reason", "Destination is not within range of any available tower");
                unreachableDestinations.add(unreachableInfo);
            }
        }

        // If no destinations are reachable, return early
        if (reachableDestinations.isEmpty()) {
            result.put("error", "None of the destination points are reachable.");
            result.put("unreachableDestinations", unreachableDestinations);
            return result;
        }

        // Create virtual towers for all reachable destinations
        List<TowerDto> destTowers = new ArrayList<>();
        for (int i = 0; i < reachableDestinations.size(); i++) {
            Map<String, Double> dest = reachableDestinations.get(i);
            TowerDto destTower = TowerDto.builder()
                    .tawalId("DEST_" + i + "_VIRTUAL")
                    .siteName("Virtual Destination Tower " + i)
                    .latitude(dest.get("latitude"))
                    .longitude(dest.get("longitude"))
                    .build();
            destTowers.add(destTower);
        }

        // Create combined list of all usable intermediate towers (POP + DB towers, NOT destinations)
        List<TowerDto> intermediateNodes = new ArrayList<>();
        intermediateNodes.add(popTower);
        intermediateNodes.addAll(allTowers);

        // Collect the IDs of all destination towers for quick lookups
        Set<String> destTowerIds = destTowers.stream()
                .map(this::getTowerId)
                .collect(Collectors.toSet());

        // Create a list of all nodes (intermediate + destinations)
        List<TowerDto> allNodes = new ArrayList<>(intermediateNodes);
        allNodes.addAll(destTowers);

        // Create a network graph where edges represent possible connections
        // This graph treats destinations as normal nodes for connectivity calculation purposes
        Map<String, Set<String>> fullNetworkGraph = buildNetworkGraph(allNodes);

        // Map to keep track of all towers by ID
        Map<String, TowerDto> towerMap = new HashMap<>();
        for (TowerDto tower : allNodes) {
            towerMap.put(getTowerId(tower), tower);
        }

        // Calculate the minimum set of intermediate towers needed to connect all destinations
        // using a modified Steiner tree approach
        Set<String> selectedTowers = findMinimumBackboneNetwork(
                popTower,
                destTowers,
                intermediateNodes,
                fullNetworkGraph,
                towerMap
        );

        // Ensure POP is always included
        selectedTowers.add(getTowerId(popTower));

        // Create a subgraph containing only the selected intermediate towers (backbone network)
        Map<String, Set<String>> backboneGraph = new HashMap<>();
        for (String towerId : selectedTowers) {
            backboneGraph.put(towerId, new HashSet<>());

            for (String neighbor : fullNetworkGraph.get(towerId)) {
                if (selectedTowers.contains(neighbor)) {
                    backboneGraph.get(towerId).add(neighbor);
                }
            }
        }

        // For each destination, find the best path from POP through the backbone network
        Map<String, List<TowerDto>> optimizedPaths = new HashMap<>();

        for (TowerDto destTower : destTowers) {
            String destId = getTowerId(destTower);

            // Find the best connecting point from backbone to this destination
            String bestConnectingTower = null;
            double shortestDistance = Double.MAX_VALUE;

            for (String towerId : selectedTowers) {
                TowerDto intermediateTower = towerMap.get(towerId);

                double distance = calculateDistance(
                        intermediateTower.getLatitude(), intermediateTower.getLongitude(),
                        destTower.getLatitude(), destTower.getLongitude()
                );

                if (distance <= 10.1 && distance < shortestDistance) { // Within range
                    shortestDistance = distance;
                    bestConnectingTower = towerId;
                }
            }

            if (bestConnectingTower != null) {
                // Find path from POP to connecting tower through backbone
                List<TowerDto> backbonePath = findShortestPath(
                        popTower,
                        towerMap.get(bestConnectingTower),
                        backboneGraph,
                        towerMap
                );

                // Add destination to the end
                List<TowerDto> fullPath = new ArrayList<>(backbonePath);
                fullPath.add(destTower);

                optimizedPaths.put(destId, fullPath);
            } else {
                // No connecting tower found within range, try direct connection from POP
                double directDistance = calculateDistance(
                        popTower.getLatitude(), popTower.getLongitude(),
                        destTower.getLatitude(), destTower.getLongitude()
                );

                if (directDistance <= 10.1) {
                    optimizedPaths.put(destId, List.of(popTower, destTower));
                } else {
                    // Find a regular path as fallback
                    List<TowerDto> fallbackPath = findShortestPath(
                            popTower, destTower, fullNetworkGraph, towerMap
                    );
                    optimizedPaths.put(destId, fallbackPath);
                }
            }
        }

        // Format the results
        List<Map<String, Object>> pathsInfo = new ArrayList<>();
        for (TowerDto destTower : destTowers) {
            String destId = getTowerId(destTower);
            List<TowerDto> path = optimizedPaths.get(destId);

            Map<String, Object> pathInfo = new HashMap<>();
            pathInfo.put("destination", Map.of(
                    "latitude", destTower.getLatitude(),
                    "longitude", destTower.getLongitude()
            ));
            pathInfo.put("path", path);
            pathInfo.put("towerCount", path.size());
            pathInfo.put("distance", calculateTotalPathDistance(path));

            pathsInfo.add(pathInfo);
        }

        // Count unique intermediate towers used (excluding POP tower and destinations)
        Set<String> uniqueIntermediateTowers = new HashSet<>(selectedTowers);
        uniqueIntermediateTowers.remove(getTowerId(popTower)); // Remove POP

        int uniqueTowersUsed = uniqueIntermediateTowers.size();
        double totalDistance = pathsInfo.stream()
                .mapToDouble(info -> (Double) info.get("distance"))
                .sum();

        result.put("paths", pathsInfo);
        result.put("statistics", Map.of(
                "uniqueTowersUsed", uniqueTowersUsed,
                "totalDestinations", reachableDestinations.size(),
                "unreachableDestinations", unreachableDestinations.size(),
                "totalDistance", totalDistance,
                "networkTopology", "tree"
        ));

        // Add information about unreachable destinations to the result
        if (!unreachableDestinations.isEmpty()) {
            result.put("unreachableDestinations", unreachableDestinations);
        }

        return result;
    }

    /**
     * Find the minimum set of intermediate towers needed to form a backbone network
     * that can reach all destinations. Uses a greedy approach inspired by the
     * Steiner Tree problem solution.
     */
    private Set<String> findMinimumBackboneNetwork(
            TowerDto popTower,
            List<TowerDto> destTowers,
            List<TowerDto> intermediateNodes,
            Map<String, Set<String>> networkGraph,
            Map<String, TowerDto> towerMap) {

        // Start with just the POP tower
        Set<String> selectedTowers = new HashSet<>();
        selectedTowers.add(getTowerId(popTower));

        // Create a set of all destination IDs
        Set<String> destIds = destTowers.stream()
                .map(this::getTowerId)
                .collect(Collectors.toSet());

        // Set of destinations that have been connected
        Set<String> connectedDests = new HashSet<>();

        // Map of shortest paths from any selected tower to each destination
        Map<String, List<TowerDto>> shortestPathsToDestinations = new HashMap<>();

        // Keep adding towers until all destinations are connected
        while (connectedDests.size() < destTowers.size()) {
            // Find all possible paths from current selected towers to unconnected destinations
            for (String towerId : selectedTowers) {
                TowerDto sourceTower = towerMap.get(towerId);

                for (TowerDto destTower : destTowers) {
                    String destId = getTowerId(destTower);
                    if (connectedDests.contains(destId)) {
                        continue; // Skip already connected destinations
                    }

                    // Check direct connection first
                    double directDistance = calculateDistance(
                            sourceTower.getLatitude(), sourceTower.getLongitude(),
                            destTower.getLatitude(), destTower.getLongitude()
                    );

                    if (directDistance <= 10.1) {
                        // Direct connection possible
                        shortestPathsToDestinations.put(destId, List.of(sourceTower, destTower));
                        connectedDests.add(destId);
                    }
                }
            }

            // If we connected all destinations directly, we're done
            if (connectedDests.size() == destTowers.size()) {
                break;
            }

            // Find the best intermediate tower to add to our network
            TowerDto bestTower = null;
            int mostNewConnections = 0;

            for (TowerDto candidate : intermediateNodes) {
                String candidateId = getTowerId(candidate);
                if (selectedTowers.contains(candidateId)) {
                    continue; // Skip already selected towers
                }

                // Add this candidate temporarily
                selectedTowers.add(candidateId);

                // Count how many new destinations could be connected
                int newConnections = 0;
                for (TowerDto destTower : destTowers) {
                    String destId = getTowerId(destTower);
                    if (connectedDests.contains(destId)) {
                        continue; // Skip already connected destinations
                    }

                    boolean canConnect = false;
                    // Check if this tower can connect to any selected tower
                    for (String existingTower : selectedTowers) {
                        if (networkGraph.get(existingTower).contains(candidateId)) {
                            canConnect = true;
                            break;
                        }
                    }

                    if (canConnect) {
                        // Check if this candidate can connect to the destination
                        double distance = calculateDistance(
                                candidate.getLatitude(), candidate.getLongitude(),
                                destTower.getLatitude(), destTower.getLongitude()
                        );

                        if (distance <= 10.1) {
                            newConnections++;
                        }
                    }
                }

                // Remove the candidate and see if it's the best so far
                selectedTowers.remove(candidateId);
                if (newConnections > mostNewConnections) {
                    mostNewConnections = newConnections;
                    bestTower = candidate;
                }
            }

            // If we found a good tower, add it
            if (bestTower != null) {
                selectedTowers.add(getTowerId(bestTower));

                // Update connected destinations
                for (TowerDto destTower : destTowers) {
                    String destId = getTowerId(destTower);
                    if (connectedDests.contains(destId)) {
                        continue;
                    }

                    double distance = calculateDistance(
                            bestTower.getLatitude(), bestTower.getLongitude(),
                            destTower.getLatitude(), destTower.getLongitude()
                    );

                    if (distance <= 10.1) {
                        connectedDests.add(destId);
                    }
                }
            } else {
                // If we didn't find any good tower, add the closest tower to any unconnected destination
                double minDistance = Double.MAX_VALUE;
                TowerDto closestTower = null;
                String closestDestId = null;

                for (TowerDto candidate : intermediateNodes) {
                    String candidateId = getTowerId(candidate);
                    if (selectedTowers.contains(candidateId)) {
                        continue;
                    }

                    // Check if this tower can connect to any selected tower
                    boolean canConnectToSelected = false;
                    for (String existingTower : selectedTowers) {
                        if (networkGraph.get(existingTower).contains(candidateId)) {
                            canConnectToSelected = true;
                            break;
                        }
                    }

                    if (canConnectToSelected) {
                        for (TowerDto destTower : destTowers) {
                            String destId = getTowerId(destTower);
                            if (connectedDests.contains(destId)) {
                                continue;
                            }

                            double distance = calculateDistance(
                                    candidate.getLatitude(), candidate.getLongitude(),
                                    destTower.getLatitude(), destTower.getLongitude()
                            );

                            if (distance < minDistance) {
                                minDistance = distance;
                                closestTower = candidate;
                                closestDestId = destId;
                            }
                        }
                    }
                }

                if (closestTower != null) {
                    selectedTowers.add(getTowerId(closestTower));
                } else {
                    // If we couldn't find any tower that connects, let's just pick the closest tower
                    // to the POP that can reach any unconnected destination
                    minDistance = Double.MAX_VALUE;

                    for (TowerDto candidate : intermediateNodes) {
                        String candidateId = getTowerId(candidate);
                        if (selectedTowers.contains(candidateId)) {
                            continue;
                        }

                        double distanceToPop = calculateDistance(
                                popTower.getLatitude(), popTower.getLongitude(),
                                candidate.getLatitude(), candidate.getLongitude()
                        );

                        if (distanceToPop <= 10.1 && distanceToPop < minDistance) {
                            boolean canReachDest = false;

                            for (TowerDto destTower : destTowers) {
                                String destId = getTowerId(destTower);
                                if (connectedDests.contains(destId)) {
                                    continue;
                                }

                                double destDistance = calculateDistance(
                                        candidate.getLatitude(), candidate.getLongitude(),
                                        destTower.getLatitude(), destTower.getLongitude()
                                );

                                if (destDistance <= 10.1) {
                                    canReachDest = true;
                                    break;
                                }
                            }

                            if (canReachDest) {
                                minDistance = distanceToPop;
                                closestTower = candidate;
                            }
                        }
                    }

                    if (closestTower != null) {
                        selectedTowers.add(getTowerId(closestTower));
                    } else {
                        // If we still can't find any tower, just add all remaining destinations
                        // to the connected set to break out of the loop
                        for (TowerDto destTower : destTowers) {
                            connectedDests.add(getTowerId(destTower));
                        }
                    }
                }
            }
        }

        // Connect the backbone network
        ensureConnectedBackbone(selectedTowers, networkGraph, intermediateNodes, towerMap);

        return selectedTowers;
    }

    /**
     * Ensure that all selected towers form a connected backbone network
     */
    private void ensureConnectedBackbone(
            Set<String> selectedTowers,
            Map<String, Set<String>> networkGraph,
            List<TowerDto> intermediateNodes,
            Map<String, TowerDto> towerMap) {

        // Use a simple BFS to check connectivity
        String startTower = selectedTowers.iterator().next();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startTower);
        visited.add(startTower);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (String neighbor : networkGraph.get(current)) {
                if (selectedTowers.contains(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // If not all towers are visited, we need to add more towers to connect them
        if (visited.size() < selectedTowers.size()) {
            // Identify disconnected components
            List<Set<String>> components = new ArrayList<>();
            Set<String> unvisited = new HashSet<>(selectedTowers);
            unvisited.removeAll(visited);

            components.add(visited);

            while (!unvisited.isEmpty()) {
                String startNode = unvisited.iterator().next();
                Set<String> component = new HashSet<>();
                Queue<String> componentQueue = new LinkedList<>();

                componentQueue.add(startNode);
                component.add(startNode);
                unvisited.remove(startNode);

                while (!componentQueue.isEmpty()) {
                    String current = componentQueue.poll();

                    for (String neighbor : networkGraph.get(current)) {
                        if (selectedTowers.contains(neighbor) &&
                                !component.contains(neighbor) &&
                                unvisited.contains(neighbor)) {
                            component.add(neighbor);
                            unvisited.remove(neighbor);
                            componentQueue.add(neighbor);
                        }
                    }
                }

                components.add(component);
            }

            // Connect components by adding intermediate towers
            for (int i = 0; i < components.size() - 1; i++) {
                Set<String> comp1 = components.get(i);
                Set<String> comp2 = components.get(i + 1);

                // Find the best tower to connect these components
                TowerDto bestConnector = null;
                double minTotalDistance = Double.MAX_VALUE;

                for (TowerDto candidate : intermediateNodes) {
                    String candidateId = getTowerId(candidate);
                    if (selectedTowers.contains(candidateId)) {
                        continue; // Skip already selected towers
                    }

                    // Check if this candidate can connect the components
                    boolean canConnectComp1 = false;
                    boolean canConnectComp2 = false;
                    double minDistComp1 = Double.MAX_VALUE;
                    double minDistComp2 = Double.MAX_VALUE;

                    for (String towerId : comp1) {
                        if (networkGraph.get(towerId).contains(candidateId)) {
                            canConnectComp1 = true;
                            double dist = calculateDistance(
                                    candidate.getLatitude(), candidate.getLongitude(),
                                    towerMap.get(towerId).getLatitude(), towerMap.get(towerId).getLongitude()
                            );
                            minDistComp1 = Math.min(minDistComp1, dist);
                        }
                    }

                    for (String towerId : comp2) {
                        if (networkGraph.get(towerId).contains(candidateId)) {
                            canConnectComp2 = true;
                            double dist = calculateDistance(
                                    candidate.getLatitude(), candidate.getLongitude(),
                                    towerMap.get(towerId).getLatitude(), towerMap.get(towerId).getLongitude()
                            );
                            minDistComp2 = Math.min(minDistComp2, dist);
                        }
                    }

                    if (canConnectComp1 && canConnectComp2) {
                        double totalDist = minDistComp1 + minDistComp2;
                        if (totalDist < minTotalDistance) {
                            minTotalDistance = totalDist;
                            bestConnector = candidate;
                        }
                    }
                }

                if (bestConnector != null) {
                    selectedTowers.add(getTowerId(bestConnector));
                } else {
                    // If no single tower can connect them, use two towers in sequence
                    TowerDto bestFirst = null;
                    TowerDto bestSecond = null;
                    minTotalDistance = Double.MAX_VALUE;

                    for (TowerDto first : intermediateNodes) {
                        String firstId = getTowerId(first);
                        if (selectedTowers.contains(firstId)) {
                            continue;
                        }

                        boolean canConnectComp1 = false;
                        for (String towerId : comp1) {
                            if (networkGraph.get(towerId).contains(firstId)) {
                                canConnectComp1 = true;
                                break;
                            }
                        }

                        if (canConnectComp1) {
                            for (TowerDto second : intermediateNodes) {
                                String secondId = getTowerId(second);
                                if (selectedTowers.contains(secondId) || secondId.equals(firstId)) {
                                    continue;
                                }

                                if (networkGraph.get(firstId).contains(secondId)) {
                                    boolean canConnectComp2 = false;
                                    for (String towerId : comp2) {
                                        if (networkGraph.get(towerId).contains(secondId)) {
                                            canConnectComp2 = true;
                                            break;
                                        }
                                    }

                                    if (canConnectComp2) {
                                        // We found a valid pair
                                        double firstDist = Double.MAX_VALUE;
                                        double secondDist = Double.MAX_VALUE;

                                        for (String towerId : comp1) {
                                            double dist = calculateDistance(
                                                    first.getLatitude(), first.getLongitude(),
                                                    towerMap.get(towerId).getLatitude(), towerMap.get(towerId).getLongitude()
                                            );
                                            firstDist = Math.min(firstDist, dist);
                                        }

                                        for (String towerId : comp2) {
                                            double dist = calculateDistance(
                                                    second.getLatitude(), second.getLongitude(),
                                                    towerMap.get(towerId).getLatitude(), towerMap.get(towerId).getLongitude()
                                            );
                                            secondDist = Math.min(secondDist, dist);
                                        }

                                        double middleDist = calculateDistance(
                                                first.getLatitude(), first.getLongitude(),
                                                second.getLatitude(), second.getLongitude()
                                        );

                                        double totalDist = firstDist + middleDist + secondDist;
                                        if (totalDist < minTotalDistance) {
                                            minTotalDistance = totalDist;
                                            bestFirst = first;
                                            bestSecond = second;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (bestFirst != null && bestSecond != null) {
                        selectedTowers.add(getTowerId(bestFirst));
                        selectedTowers.add(getTowerId(bestSecond));
                    }
                }
            }
        }
    }

    /**
     * Find the shortest path between two towers in the network graph
     */
    private List<TowerDto> findShortestPath(TowerDto start, TowerDto end,
                                            Map<String, Set<String>> graph,
                                            Map<String, TowerDto> towerMap) {
        // Maps for Dijkstra's algorithm
        Map<String, String> previous = new HashMap<>();
        Map<String, Double> distance = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // Initialize
        for (String towerId : graph.keySet()) {
            distance.put(towerId, Double.MAX_VALUE);
        }

        String startId = getTowerId(start);
        String endId = getTowerId(end);
        distance.put(startId, 0.0);

        PriorityQueue<String> queue = new PriorityQueue<>(
                Comparator.comparingDouble(distance::get)
        );
        queue.add(startId);

        // Dijkstra's algorithm
        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(endId)) {
                break; // Found the destination
            }

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            // Process all neighbors
            for (String neighbor : graph.getOrDefault(current, Collections.emptySet())) {
                if (visited.contains(neighbor)) {
                    continue;
                }

                TowerDto currentTower = towerMap.get(current);
                TowerDto neighborTower = towerMap.get(neighbor);

                double segmentDist = calculateDistance(
                        currentTower.getLatitude(), currentTower.getLongitude(),
                        neighborTower.getLatitude(), neighborTower.getLongitude()
                );

                double newDist = distance.get(current) + segmentDist;

                if (newDist < distance.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distance.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        // Reconstruct the path
        List<TowerDto> path = new ArrayList<>();
        String current = endId;

        // If no path found
        if (!previous.containsKey(endId) && !startId.equals(endId)) {
            return List.of(start, end); // Return direct connection as fallback
        }

        // Build path from end to start
        while (current != null) {
            path.add(0, towerMap.get(current)); // Add to beginning of list
            current = previous.get(current);     // Move to previous tower
        }

        return path;
    }

    /**
     * Builds a network graph representing possible connections between all towers
     */
    private Map<String, Set<String>> buildNetworkGraph(List<TowerDto> allPossibleTowers) {
        // Create the graph as an adjacency list
        Map<String, Set<String>> graph = new HashMap<>();

        // Initialize empty sets for each tower
        for (TowerDto tower : allPossibleTowers) {
            graph.put(getTowerId(tower), new HashSet<>());
        }

        // Fill the graph with valid connections (within MAX_TOWER_DISTANCE)
        final double MAX_TOWER_DISTANCE = 10.1; // Same as in PathService

        for (int i = 0; i < allPossibleTowers.size(); i++) {
            TowerDto tower1 = allPossibleTowers.get(i);
            String id1 = getTowerId(tower1);

            for (int j = i + 1; j < allPossibleTowers.size(); j++) {
                TowerDto tower2 = allPossibleTowers.get(j);
                String id2 = getTowerId(tower2);

                double distance = calculateDistance(
                        tower1.getLatitude(), tower1.getLongitude(),
                        tower2.getLatitude(), tower2.getLongitude()
                );

                if (distance <= MAX_TOWER_DISTANCE) {
                    // Add bidirectional connection
                    graph.get(id1).add(id2);
                    graph.get(id2).add(id1);
                }
            }
        }

        return graph;
    }

    /**
     * Calculates the total distance of a path
     */
    private double calculateTotalPathDistance(List<TowerDto> path) {
        double totalDistance = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            TowerDto current = path.get(i);
            TowerDto next = path.get(i + 1);

            totalDistance += calculateDistance(
                    current.getLatitude(), current.getLongitude(),
                    next.getLatitude(), next.getLongitude()
            );
        }

        return totalDistance;
    }

    /**
     * Helper method to get a unique ID for a tower
     */
    private String getTowerId(TowerDto tower) {
        if (tower.getId() != null) {
            return tower.getId().toString();
        }
        return tower.getTawalId() != null ? tower.getTawalId() :
                tower.getLatitude() + ":" + tower.getLongitude();
    }

    /**
     * Convert Tower entity to TowerDto
     */
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

    /**
     * Calculate distance between two coordinates using Haversine formula
     * Same as in PathService
     */
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
}