package com.example.atheer_ct.repo;


import com.example.atheer_ct.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 */
@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    /**
     * Find all users.
     * 
     * @return list of all users
     */
    @Override
    List<User> findAll();

    /**
     * Find users by username.
     * 
     * @param username the username
     * @return the user if found, null otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find users by email.
     * 
     * @param email the email
     * @return the user if found, null otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Find users by family.
     * 
     * @param family the family
     * @return list of users belonging to the given family
     */

}
