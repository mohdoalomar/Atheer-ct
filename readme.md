# Tower Pathfinding Services

This project contains different services for finding communication paths between two geographical points using a network of towers. Each service employs distinct algorithms and strategies to achieve this, with the core constraint that the distance between any two consecutive towers in a path cannot exceed a predefined maximum (typically 10.1 km).

## Core Files and Algorithms

### 1. `OldPathService.java`

*   **Purpose:** To find a path between a start and end point, prioritizing the **minimum number of towers**.
*   **Primary Algorithm: Breadth-First Search (BFS)**
    *   **How it works:** BFS explores the tower network level by level, starting from the `startTower`. It guarantees that the first time the `endTower` is reached, the path taken has the fewest possible hops (towers).
    *   **Goal:** Achieve the absolute minimum tower count for the path.
*   **Fallback Algorithm: Heuristic Interpolation (`findPathByInterpolation`)**
    *   **Trigger:** Used if BFS fails to find a multi-hop path (i.e., only returns the start and end points).
    *   **How it works:**
        1.  Calculates an ideal straight-line path between start and end.
        2.  Iteratively tries to place intermediate towers:
            *   Determines an "ideal next point" along the straight line.
            *   Searches for the best *existing* tower near this ideal point based on proximity, progress towards the destination, and deviation from the direct bearing.
            *   If no suitable tower is found, it adjusts its current reference point or attempts to pick any tower that gets it closer to the end.
        3.  Includes a `validateAndFixPath` mechanism that recursively attempts to insert existing towers if any segment in the interpolated path is too long.
    *   **Goal:** Provide a "best-effort" path using existing towers when BFS fails, generally trying to follow a direct route.
*   **Final Step:** `validateAllPathSegments` ensures all hops in the chosen path meet the `MAX_TOWER_DISTANCE` constraint.

---

### 2. `PathService.java` (Original/A* version)

*   **Purpose:** To find a path, attempting a balance between **fewer towers and a geographically direct route**.
*   **Primary Algorithm: A\* (A-star) Search (`findMinimumTowerPath`)**
    *   **How it works:** A\* is an informed search algorithm. It uses a priority queue and evaluates nodes based on a cost function: `f(n) = g(n) + w*h(n)`
        *   `g(n)`: The actual distance traveled from the start tower to the current tower `n`.
        *   `h(n)`: A heuristic estimate of the distance from tower `n` to the `endTower` (typically straight-line distance).
        *   `w`: A weight that can be used to adjust the balance between `g(n)` and `h(n)`.
    *   **Goal:** Find a path that is efficient in terms of both hops and geographical directness. The weight `w` allows tuning this balance.
*   **Fallback Algorithm: Heuristic Interpolation (`findPathByInterpolation`)**
    *   **Trigger:** Used if A\* fails to find a multi-hop path.
    *   **How it works:** Similar to the interpolation in `OldPathService`. It tries to pick existing towers along an ideal straight line. It also includes the `validateAndFixPath` and `fixSegment` logic for repairing overly long segments using existing towers.
    *   **Goal:** Provide a best-effort path when A\* fails.
*   **Final Step:** `validateAllPathSegments` ensures all hops meet the `MAX_TOWER_DISTANCE` constraint.

---

### 3. `CombinedPathService.java`

*   **Purpose:** To find an optimal path by considering **both minimum tower count and geographical directness explicitly**, then choosing the best option.
*   **Algorithm 1: Breadth-First Search (BFS) (`findMinimumTowerCountPath`)**
    *   **How it works:** Same as in `OldPathService`. Finds the path with the absolute minimum number of towers.
    *   **Goal:** Secure a path with the fewest hops.
*   **Algorithm 2: A\* (A-star) Search with Bearing Penalties & Smoothing (`findDirectPath`)**
    *   **How it works:**
        *   Uses A\* search, but its cost function is enhanced to penalize paths that deviate significantly in bearing from the direct start-to-end line (discouraging zigzags).
        *   Includes a `smoothPath` post-processing step that attempts to:
            *   Remove unnecessary intermediate towers if a direct connection is possible.
            *   Replace towers in zigzag patterns with "better" intermediate towers to straighten the path.
    *   **Goal:** Find a geographically direct and smooth path.
*   **Path Selection Logic:**
    1.  Both BFS and A\* (with smoothing) paths are generated.
    2.  If the A\* path has the **same number of towers or fewer** than the BFS path, the A\* path is chosen (as it's likely more direct).
    3.  Otherwise, the BFS path (with the absolute minimum tower count) is chosen.
*   **Fallback Algorithm (for both BFS and A\* if they fail): Heuristic Interpolation with Virtual Towers (`findPathByInterpolation`)**
    *   **Trigger:** If either BFS or A\* cannot find a path with existing towers.
    *   **How it works:** Similar to other interpolation methods, but with a key difference:
        *   If no suitable *existing* tower is found for an intermediate segment, this version **creates a "virtual intermediate tower"** as a placeholder.
    *   **Goal:** Ensure a path is always proposed, even if it means highlighting segments where new infrastructure (virtual towers) might be needed.
*   **Final Step:** `validateAllPathSegments` ensures all hops (between real or virtual towers) meet the `MAX_TOWER_DISTANCE` constraint.

## Common Helper Methods

All services share utility methods for:

*   `calculateDistance()`: Computes Haversine distance between two lat/lon points.
*   `calculateBearing()`: Computes the geographical bearing from one point to another.
*   `calculateDestinationPoint()`: Calculates a new lat/lon point given a start, bearing, and distance.
*   `convertToDto()`: Converts `Tower` entities to `TowerDto` objects.
*   `getTowerId()`: Generates a unique ID string for a tower.
*   `describeLocation()`: Provides a user-friendly description of a tower (start, end, virtual, or named).

---

## Performance Considerations & Future Optimizations

**Note:** The current implementations prioritize algorithmic exploration and correctness over raw performance. Significant performance optimizations are possible and likely necessary for production use with large datasets or high request volumes.

**Key Areas for Optimization:**

1.  **Neighbor Finding in Search Algorithms (BFS/A\*):**
    *   Currently, finding neighbors for a tower involves iterating through *all* other towers and calculating distances. This is O(N) for each node expansion, leading to poor scalability (e.g., O(N^2) in dense graphs for the search part).
    *   **Potential Solution:** Implement spatial indexing (e.g., Quadtrees, R-trees in-memory, or using database spatial extensions if available) to quickly find towers within the `MAX_TOWER_DISTANCE` of a given tower. This would change neighbor lookup to be much faster (e.g., O(log N) or O(constant) on average for sparse regions).
2.  **A\* Priority Queue Comparator:**
    *   In the original `PathService.java`, the A\* comparator (`findMinimumTowerPath`) was recalculating the path distance (`g(n)`) from scratch. This has been addressed in `CombinedPathService.java` by using the `distanceSoFar` map, but it's a critical optimization to be aware of.
3.  **`CombinedPathService` Overhead:**
    *   Running two full pathfinding algorithms (BFS and A\*) in `CombinedPathService` inherently increases computation time compared to running just one. The benefits of the combined approach need to be weighed against this cost.

