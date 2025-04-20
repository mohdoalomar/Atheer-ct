package com.example.atheer_ct.services;

import com.example.atheer_ct.dto.UserDTO;
import com.example.atheer_ct.entities.User;

import com.example.atheer_ct.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for User entity.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;

        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all users.
     * 
     * @return list of all users
     */
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a user by its ID.
     * 
     * @param id the user ID
     * @return the user if found, null otherwise
     */
    public UserDTO getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(this::convertToDTO).orElse(null);
    }

    /**
     * Get a user by its username.
     * 
     * @param username the username
     * @return the user if found, null otherwise
     */
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).get();
        return user != null ? convertToDTO(user) : null;
    }

    /**
     * Get a user by its email.
     * 
     * @param email the email
     * @return the user if found, null otherwise
     */
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).get();
        return user != null ? convertToDTO(user) : null;
    }


    public UserDTO createUser(UserDTO userDTO) {
        User user = convertToEntity(userDTO);
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    /**
     * Update a user.
     * 
     * @param id      the user ID
     * @param userDTO the user DTO
     * @return the updated user
     */


    /**
     * Delete a user by its ID.
     * 
     * @param id the user ID
     * @return true if the user was deleted, false otherwise
     */
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        userRepository.deleteById(id);
        return true;
    }

    /**
     * Convert a User entity to a UserDTO.
     * 
     * @param user the user entity
     * @return the user DTO
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword()).build();


    }

    /**
     * Convert a UserDTO to a User entity.
     * 
     * @param userDTO the user DTO
     * @return the user entity
     */
    private User convertToEntity(UserDTO userDTO) {
       return User.builder()
                .id(userDTO.getId())
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .password(userDTO.getPassword()).build();


    }
}
