package com.citizens.digital.twin.api.controller;

import com.citizens.digital.twin.api.dto.AuthConfigResponse;
import com.citizens.digital.twin.api.dto.LoginRequest;
import com.citizens.digital.twin.api.dto.LoginResponse;
import com.citizens.digital.twin.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @GetMapping("/config")
  public AuthConfigResponse config() {
    return authService.config();
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
