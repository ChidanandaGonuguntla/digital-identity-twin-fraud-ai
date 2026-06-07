package com.aegis.digitaltwin.controller;

import com.aegis.digitaltwin.dto.CreateTwinRequest;
import com.aegis.digitaltwin.entity.CustomerTwin;
import com.aegis.digitaltwin.service.DigitalTwinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/twins")
@RequiredArgsConstructor
public class DigitalTwinController {
    private final DigitalTwinService digitalTwinService;

    @GetMapping
    public List<CustomerTwin> findAll() {
        return digitalTwinService.findAll();
    }

    @GetMapping("/{customerId}")
    public CustomerTwin getTwin(@PathVariable String customerId) {
        return digitalTwinService.getTwin(customerId);
    }

    @PostMapping
    public CustomerTwin createTwin(@Valid @RequestBody CreateTwinRequest request) {
        return digitalTwinService.createTwin(request);
    }
}
