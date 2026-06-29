package io.comp.backend.shared.auth.controller;

import io.comp.backend.shared.auth.dto.*;
import io.comp.backend.shared.auth.jwt.JwtUtils;
import io.comp.backend.shared.auth.model.RefreshToken;
import io.comp.backend.shared.auth.model.User;
import io.comp.backend.shared.auth.repository.UserRepository;
import io.comp.backend.shared.auth.service.RefreshTokenService;
import io.comp.backend.shared.exception.TokenRefreshException;
import io.comp.backend.shared.statistics.model.Statistics;
import io.comp.backend.shared.statistics.repository.StatisticsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername());
        
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .eloRating(user.getEloRating())
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .passwordHash(encoder.encode(signUpRequest.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        // Initialize user statistics
        Statistics statistics = Statistics.builder()
                .user(savedUser)
                .build();
        statisticsRepository.save(statistics);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateJwtToken(user.getUsername());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    return ResponseEntity.ok(new TokenRefreshResponse(token, newRefreshToken.getToken()));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        
        refreshTokenService.findByToken(requestRefreshToken)
                .ifPresent(token -> refreshTokenService.deleteByUserId(token.getUser().getId()));
                
        return ResponseEntity.ok(new MessageResponse("Log out successful!"));
    }
}
