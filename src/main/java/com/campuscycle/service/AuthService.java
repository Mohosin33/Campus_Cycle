package com.campuscycle.service;

import com.campuscycle.dao.UserDao;
import com.campuscycle.model.User;
import com.campuscycle.security.PasswordHasher;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Production Authentication & Session Service with Cryptographic Verification.
 */
public class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private static AuthService instance;
    private final UserDao userDao;
    private User currentUser;

    private AuthService() {
        this.userDao = new UserDao();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Authenticates user using salted SHA-256 hash comparison.
     */
    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        Optional<User> userOpt = userDao.findByUsername(username.trim());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Account suspension check
            if (!user.isActive()) {
                LOGGER.warning("Login rejected for suspended user: " + username);
                return false;
            }

            // Cryptographic verification
            boolean match = PasswordHasher.verifyPassword(password.trim(), user.getPasswordHash(), user.getPasswordSalt());
            if (match) {
                this.currentUser = user;
                LOGGER.info("User authenticated successfully: " + username + " (" + user.getRole() + ")");
                return true;
            }
        }
        LOGGER.warning("Failed login attempt for username: " + username);
        return false;
    }

    /**
     * Registers a new user with generated cryptographic salt and SHA-256 hash.
     */
    public boolean register(User newUser, String plainPassword) {
        if (newUser == null || newUser.getUsername() == null || plainPassword == null) {
            return false;
        }

        Optional<User> existing = userDao.findByUsername(newUser.getUsername().trim());
        if (existing.isPresent()) {
            return false; // Username already taken
        }

        // Generate salt and hash
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(plainPassword.trim(), salt);

        newUser.setPasswordSalt(salt);
        newUser.setPasswordHash(hash);

        User saved = userDao.save(newUser);
        return saved != null && saved.getId() > 0;
    }

    public void refreshCurrentUser() {
        if (currentUser != null) {
            userDao.findById(currentUser.getId()).ifPresent(u -> this.currentUser = u);
        }
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
