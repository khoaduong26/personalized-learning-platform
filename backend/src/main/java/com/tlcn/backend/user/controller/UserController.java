package com.tlcn.backend.user.controller;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }
    @GetMapping public List<UserSummary> list() { return service.findAll().stream().map(UserSummary::from).toList(); }
    @GetMapping("/{id}") public UserSummary get(@PathVariable Long id) { return UserSummary.from(service.get(id)); }
}
