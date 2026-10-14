package com.tlcn.backend;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class TestController {
    @GetMapping("/home")
    public String hello() {
        return "serrver chay ngon roi";
    }
    
}
