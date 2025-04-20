package com.example.atheer_ct.controllers;

import com.example.atheer_ct.dto.UserDTO;
import com.example.atheer_ct.services.UserService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User entity.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * Constructor.
     *
     * @param userService the user service
     */
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    public UserDTO getCurrentUser() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // Get the current user's ID
            return userService.getUserByUsername(authentication.getName());
        }
        return null;

    }
    @GetMapping("/check-auth")
    public ResponseEntity<String> checkAuth() {
        // If we get here, the user is authenticated due to Spring Security
        return ResponseEntity.ok("Authenticated");
    }

    /**
     * Get a user by its ID.
     *
     * @param id the user ID
     * @return ResponseEntity containing the user if found, null otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId() == id) {
            // Check if requested ID matches current user's ID
            return ResponseEntity.ok(currentUser);
        }

        // If not authenticated or user not found
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

    }




    /**
     * Create a new user.
     *
     * @param userDTO the user DTO
     * @return ResponseEntity containing the created user
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.createUser(userDTO));
    }


}
