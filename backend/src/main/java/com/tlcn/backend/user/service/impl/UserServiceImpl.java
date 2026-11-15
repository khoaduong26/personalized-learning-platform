package com.tlcn.backend.user.service.impl;

import com.tlcn.backend.user.repository.UserRepository;
import com.tlcn.backend.user.service.UserService;
import com.tlcn.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User save(User user) {
        return repository.save(user);
    }
}
