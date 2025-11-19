package fin.service.spring;

import fin.model.User;
import org.springframework.stereotype.Service;

/**
 * Spring-compatible User service for JWT authentication
 * Simplified version for Spring Boot migration
 */
@Service
public class UserService {

    /**
     * Get user by ID (simplified for Spring Boot migration)
     * In a full implementation, this would query the database
     */
    public User getUserById(Long userId) {
        // For now, return a mock user for testing
        // In production, this would query the database
        if (userId == 1L) {
            User user = new User();
            user.setId(1L);
            user.setEmail("test@example.com");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setActive(true);
            user.setEmail("test@example.com");
            return user;
        }
        return null;
    }
}