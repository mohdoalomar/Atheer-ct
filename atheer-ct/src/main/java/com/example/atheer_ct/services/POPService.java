package com.example.atheer_ct.services;

import com.example.atheer_ct.dto.TowerDto;
import com.example.atheer_ct.entities.Tower;
import com.example.atheer_ct.repo.TowerRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class POPService {

    private final PathService pathService;
    private final TowerRepository towerRepository;

    public POPService(PathService pathService, TowerRepository towerRepository) {
        this.pathService = pathService;
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
    public Map<String, Object> findOptimizedPOPPaths(double popLat, double popLon,
                                                     List<Map<String, Double>> destinationPoints) {
        Map<String, Object> result = new HashMap<>();

        // Create a map to track all used towers across all paths
        Map<String, TowerDto> allUsedTowers = new HashMap<>();

        // Create virtual tower for POP point
        TowerDto popTower = TowerDto.builder()
                .tawalId("POP_VIRTUAL")
                .siteName("Virtual POP Tower")
                .latitude(popLat)
                .longitude(popLon)
                .build();

        // Add POP tower to the used towers
        allUsedTowers.put(getTowerId(popTower), popTower);

        // Create a list to store information about all paths
        List<Map<String, Object>> pathsInfo = new ArrayList<>();

        // Process each destination
        for (Map<String, Double> destination : destinationPoints) {
            double destLat = destination.get("latitude");
            double destLon = destination.get("longitude");

            // Find shortest path to this destination
            Map<String, Object> pathResult = pathService.findShortestPath(popLat, popLon, destLat, destLon);

            if (pathResult.containsKey("path")) {
                @SuppressWarnings("unchecked")
                List<TowerDto> path = (List<TowerDto>) pathResult.get("path");

                // Track towers used in this path
                for (TowerDto tower : path) {
                    String towerId = getTowerId(tower);
                    if (!allUsedTowers.containsKey(towerId)) {
                        allUsedTowers.put(towerId, tower);
                    }
                }

                // Create info object for this path
                Map<String, Object> pathInfo = new HashMap<>();
                pathInfo.put("destination", Map.of(
                        "latitude", destLat,
                        "longitude", destLon
                ));
                pathInfo.put("path", path);
                pathInfo.put("towerCount", path.size());
                pathInfo.put("distance", calculateTotalPathDistance(path));

                pathsInfo.add(pathInfo);
            } else {
                // Handle error for this destination
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("destination", Map.of(
                        "latitude", destLat,
                        "longitude", destLon
                ));
                errorInfo.put("error", pathResult.get("error"));
                pathsInfo.add(errorInfo);
            }
        }

        // Calculate network statistics
        int uniqueTowersUsed = allUsedTowers.size() - 1; // Subtract 1 for the POP tower
        double totalDistance = pathsInfo.stream()
                .filter(info -> info.containsKey("distance"))
                .mapToDouble(info -> (Double) info.get("distance"))
                .sum();

        // Add results and statistics
        result.put("paths", pathsInfo);
        result.put("statistics", Map.of(
                "uniqueTowersUsed", uniqueTowersUsed,
                "totalDestinations", destinationPoints.size(),
                "totalDistance", totalDistance,
                "successfulPaths", pathsInfo.stream().filter(p -> !p.containsKey("error")).count()
        ));

        return result;
    }

    /**
     * Optimized version that creates a ring-like network structure connecting all destinations
     * while minimizing the total tower count by using destinations as relay points
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

        // Create virtual towers for all destinations
        List<TowerDto> destTowers = new ArrayList<>();
        for (int i = 0; i < destinationPoints.size(); i++) {
            Map<String, Double> dest = destinationPoints.get(i);
            TowerDto destTower = TowerDto.builder()
                    .tawalId("DEST_" + i + "_VIRTUAL")
                    .siteName("Virtual Destination Tower " + i)
                    .latitude(dest.get("latitude"))
                    .longitude(dest.get("longitude"))
                    .build();
            destTowers.add(destTower);
        }

        // Create combined list of all towers including destinations as potential intermediate towers
        List<TowerDto> allPossibleTowers = new ArrayList<>();
        allPossibleTowers.add(popTower);
        allPossibleTowers.addAll(allTowers);
        allPossibleTowers.addAll(destTowers);

        // Create a network graph where edges represent possible connections
        Map<String, Set<String>> networkGraph = buildNetworkGraph(allPossibleTowers);

        // Map to keep track of all towers by ID
        Map<String, TowerDto> towerMap = new HashMap<>();
        for (TowerDto tower : allPossibleTowers) {
            towerMap.put(getTowerId(tower), tower);
        }

        // Collect the IDs of all destination towers for quick lookups
        Set<String> destTowerIds = destTowers.stream()
                .map(this::getTowerId)
                .collect(Collectors.toSet());

        // Create a minimum spanning tree (MST) connecting all destinations
        // This will form the basis of our ring-like network
        Set<String> includedTowers = new HashSet<>();
        Map<String, List<TowerDto>> optimizedPaths = new HashMap<>();

        // First, create a complete graph between just the destinations
        // to help us find the most efficient connections between them
        List<EdgeWithWeight> allDestEdges = new ArrayList<>();
        for (int i = 0; i < destTowers.size(); i++) {
            TowerDto destA = destTowers.get(i);
            String idA = getTowerId(destA);

            for (int j = i + 1; j < destTowers.size(); j++) {
                TowerDto destB = destTowers.get(j);
                String idB = getTowerId(destB);

                // Find the shortest path between these destinations
                List<TowerDto> shortestPath = findShortestPath(destA, destB, networkGraph, towerMap);
                double pathDistance = calculateTotalPathDistance(shortestPath);

                allDestEdges.add(new EdgeWithWeight(idA, idB, pathDistance, shortestPath));
            }

            // Also add connections from POP to each destination
            List<TowerDto> popToDestPath = findShortestPath(popTower, destA, networkGraph, towerMap);
            double popPathDistance = calculateTotalPathDistance(popToDestPath);
            allDestEdges.add(new EdgeWithWeight(getTowerId(popTower), idA, popPathDistance, popToDestPath));
        }

        // Sort all edges by weight
        allDestEdges.sort(Comparator.comparingDouble(EdgeWithWeight::getWeight));

        // Apply a modified Kruskal's algorithm to form a ring-like MST
        // that connects all destinations and the POP
        DisjointSet disjointSet = new DisjointSet();
        disjointSet.makeSet(getTowerId(popTower));
        for (TowerDto dest : destTowers) {
            disjointSet.makeSet(getTowerId(dest));
        }

        // Track edges that form the MST
        List<EdgeWithWeight> mstEdges = new ArrayList<>();

        // First pass: build the MST
        for (EdgeWithWeight edge : allDestEdges) {
            if (!disjointSet.connected(edge.getSource(), edge.getTarget())) {
                disjointSet.union(edge.getSource(), edge.getTarget());
                mstEdges.add(edge);
            }

            // Stop once we have n-1 edges (for n nodes)
            if (mstEdges.size() == destTowers.size()) {
                break;
            }
        }

        // Second pass: add extra edges to form loops (ring structure)
        // This creates redundancy and more direct routes
        int extraEdges = Math.min(3, destTowers.size() / 3); // Add some extra edges based on network size
        int edgesAdded = 0;

        for (EdgeWithWeight edge : allDestEdges) {
            // Skip edges already in the MST
            if (mstEdges.contains(edge)) {
                continue;
            }

            // Add some strategic extra edges to form rings
            // Focus on edges between destinations (not from POP)
            if (edgesAdded < extraEdges &&
                    destTowerIds.contains(edge.getSource()) &&
                    destTowerIds.contains(edge.getTarget())) {
                mstEdges.add(edge);
                edgesAdded++;
            }
        }

        // Now build the actual network from the MST edges
        Set<String> networkTowers = new HashSet<>();
        networkTowers.add(getTowerId(popTower)); // Always include POP

        // Add all towers from the MST paths
        for (EdgeWithWeight edge : mstEdges) {
            for (TowerDto tower : edge.getPath()) {
                networkTowers.add(getTowerId(tower));
            }
        }

        // For each destination, find the shortest path from POP using only network towers
        for (TowerDto destTower : destTowers) {
            String destId = getTowerId(destTower);
            List<TowerDto> path = findMstPath(popTower, destTower, networkGraph, towerMap, networkTowers);
            optimizedPaths.put(destId, path);
        }

        // Store the full set of included towers
        includedTowers = networkTowers;

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
        Set<String> uniqueIntermediateTowers = includedTowers.stream()
                .filter(id -> !id.equals(getTowerId(popTower)) &&
                        !destTowers.stream().anyMatch(dest -> getTowerId(dest).equals(id)))
                .collect(Collectors.toSet());

        int uniqueTowersUsed = uniqueIntermediateTowers.size();
        double totalDistance = pathsInfo.stream()
                .mapToDouble(info -> (Double) info.get("distance"))
                .sum();

        result.put("paths", pathsInfo);
        result.put("statistics", Map.of(
                "uniqueTowersUsed", uniqueTowersUsed,
                "totalDestinations", destinationPoints.size(),
                "totalDistance", totalDistance,
                "networkTopology", "ring"
        ));

        return result;
    }

    /**
     * Helper class to represent an edge with weight and associated path
     */
    private static class EdgeWithWeight {
        private final String source;
        private final String target;
        private final double weight;
        private final List<TowerDto> path;

        public EdgeWithWeight(String source, String target, double weight, List<TowerDto> path) {
            this.source = source;
            this.target = target;
            this.weight = weight;
            this.path = path;
        }

        public String getSource() {
            return source;
        }

        public String getTarget() {
            return target;
        }

        public double getWeight() {
            return weight;
        }

        public List<TowerDto> getPath() {
            return path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeWithWeight that = (EdgeWithWeight) o;
            return (source.equals(that.source) && target.equals(that.target)) ||
                    (source.equals(that.target) && target.equals(that.source));
        }

        @Override
        public int hashCode() {
            // Symmetric hash for undirected edges
            return source.hashCode() + target.hashCode();
        }
    }

    /**
     * Helper class for union-find algorithm (disjoint set)
     */
    private static class DisjointSet {
        private final Map<String, String> parent = new HashMap<>();
        private final Map<String, Integer> rank = new HashMap<>();

        public void makeSet(String x) {
            parent.put(x, x);
            rank.put(x, 0);
        }

        public String find(String x) {
            if (!parent.get(x).equals(x)) {
                parent.put(x, find(parent.get(x))); // Path compression
            }
            return parent.get(x);
        }

        public void union(String x, String y) {
            String rootX = find(x);
            String rootY = find(y);

            if (rootX.equals(rootY)) {
                return;
            }

            // Union by rank
            if (rank.get(rootX) < rank.get(rootY)) {
                parent.put(rootX, rootY);
            } else {
                parent.put(rootY, rootX);
                if (rank.get(rootX).equals(rank.get(rootY))) {
                    rank.put(rootX, rank.get(rootX) + 1);
                }
            }
        }

        public boolean connected(String x, String y) {
            return find(x).equals(find(y));
        }
    }

    /**
     * Find the shortest path between two towers using only towers in the networkTowers set
     */
    private List<TowerDto> findMstPath(TowerDto start, TowerDto end,
                                       Map<String, Set<String>> graph,
                                       Map<String, TowerDto> towerMap,
                                       Set<String> networkTowers) {
        // Maps for Dijkstra's algorithm
        Map<String, String> previous = new HashMap<>();
        Map<String, Double> distance = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // Initialize
        for (String towerId : graph.keySet()) {
            // Only consider towers in our network
            if (networkTowers.contains(towerId)) {
                distance.put(towerId, Double.MAX_VALUE);
            }
        }

        String startId = getTowerId(start);
        String endId = getTowerId(end);

        // Ensure start and end are in our distance map
        distance.put(startId, 0.0);
        distance.put(endId, Double.MAX_VALUE);

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

            // Process all neighbors (if they're in our network)
            for (String neighbor : graph.get(current)) {
                if (visited.contains(neighbor) || !networkTowers.contains(neighbor)) {
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
            for (String neighbor : graph.get(current)) {
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

                if (newDist < distance.get(neighbor)) {
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
     * Counts how many destinations are used as relay points for other destinations
     * Note: Modified to identify destinations by checking against the destTowers list
     */
    private int countDestinationsUsedAsRelays(Map<String, List<TowerDto>> paths, List<TowerDto> destTowers) {
        Set<String> relayDestinations = new HashSet<>();

        // Create a set of destination tower IDs for faster lookup
        Set<String> destTowerIds = destTowers.stream()
                .map(this::getTowerId)
                .collect(Collectors.toSet());

        // For each path
        for (List<TowerDto> path : paths.values()) {
            // Check each tower in the path (except first and last)
            for (int i = 1; i < path.size() - 1; i++) {
                TowerDto tower = path.get(i);
                String towerId = getTowerId(tower);

                // If this intermediate tower is a destination
                if (destTowerIds.contains(towerId)) {
                    relayDestinations.add(towerId);
                }
            }
        }

        return relayDestinations.size();
    }

    /**
     * Find destinations that can be directly connected to the POP
     */
    private Map<String, TowerDto> findDirectConnections(TowerDto popTower, List<TowerDto> destTowers) {
        Map<String, TowerDto> directConnections = new HashMap<>();
        final double MAX_TOWER_DISTANCE = 10.1;

        for (TowerDto destTower : destTowers) {
            double distance = calculateDistance(
                    popTower.getLatitude(), popTower.getLongitude(),
                    destTower.getLatitude(), destTower.getLongitude()
            );

            if (distance <= MAX_TOWER_DISTANCE) {
                directConnections.put(getTowerId(destTower), destTower);
            }
        }

        return directConnections;
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
     * Enhanced path finding that can use destinations as relay points
     */
    private List<TowerDto> findEnhancedPath(TowerDto start, TowerDto end,
                                            Map<String, Set<String>> graph,
                                            Map<String, TowerDto> towerMap,
                                            Set<String> includedTowers) {
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

        // Priority queue with modified comparator that:
        // 1. Prefers included towers
        // 2. Gives slight preference to destination towers that can act as relays
        PriorityQueue<String> queue = new PriorityQueue<>((a, b) -> {
            // Get base distances
            double distA = distance.get(a);
            double distB = distance.get(b);

            // Apply preference for included towers (small bonus)
            if (includedTowers.contains(a) && !includedTowers.contains(b)) {
                distA -= 0.5; // Make included towers slightly more attractive
            } else if (!includedTowers.contains(a) && includedTowers.contains(b)) {
                distB -= 0.5;
            }

            // No special preference for destination towers as relays
            // All towers are treated equally based on their position in the network

            return Double.compare(distA, distB);
        });

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
            for (String neighbor : graph.get(current)) {
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

                if (newDist < distance.get(neighbor)) {
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