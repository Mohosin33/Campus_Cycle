package com.campuscycle.service;

import com.campuscycle.dao.UserDao;
import com.campuscycle.model.User;

import java.util.Optional;

/**
 * Authentication and Session management service.
 */
public class AuthService {
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

    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        Optional<User> userOpt = userDao.findByUsername(username.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password.trim())) {
                this.currentUser = user;
                return true;
            }
        }
        return false;
    }

    public boolean register(User newUser) {
        if (newUser == null || newUser.getUsername() == null) return false;
        Optional<User> existing = userDao.findByUsername(newUser.getUsername().trim());
        if (existing.isPresent()) {
            return false; // Username already taken
        }
        User saved = userDao.save(newUser);
        return saved != null && saved.getId() > 0;
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
