package utp.edu.com.crediticio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // Mapea la página principal (index.html)
    @GetMapping("/")
    public String mostrarIndex() {
        return "index"; 
    }

    // Mapea la página de login (login.html)
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login"; 
    }
}
