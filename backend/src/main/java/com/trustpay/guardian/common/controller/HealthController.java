package com.trustpay.guardian.common.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health and operational status REST controller.
 * Exposes non-sensitive health check for frontend and diagnostic verification.
 */
@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin(origins = "${app.security.cors.allowed-origin:http://localhost:5173}")
public class HealthController {

    @Value("${spring.application.name:trustpay-guardian-backend}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "TrustPay Guardian: Fraud-Aware Digital Banking Platform",
                "service", applicationName,
                "version", "0.1.0-SNAPSHOT",
                "profile", activeProfile,
                "simulationMode", true,
                "timestamp", Instant.now().toString()
        ));
    }
}
