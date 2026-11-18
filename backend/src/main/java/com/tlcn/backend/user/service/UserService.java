package com.tlcn.backend.user.service;

import java.util.List;
import com.tlcn.backend.user.entity.User;

public interface UserService {

    List<User> findAll();

    User get(Long id);

    User save(User user);
}
