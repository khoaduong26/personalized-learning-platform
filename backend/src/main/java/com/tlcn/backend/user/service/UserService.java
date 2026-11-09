package com.tlcn.backend.user.service;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class UserService {
    private final UserRepository repository;
    public UserService(UserRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<User> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public User get(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found")); }
    @Transactional public User save(User user) { return repository.save(user); }
}
