package com.leadera.leadera.controller;

import com.leadera.leadera.model.Interaccion;
import com.leadera.leadera.service.InteraccionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/leads/{leadId}/interacciones")
public class InteraccionController {
    private final InteraccionService interaccionService;

    public InteraccionController(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }

    @PostMapping
    public ResponseEntity<Interaccion> crear(@PathVariable Long leadId,
                                             @RequestBody Interaccion interaccion,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime proximoContacto) {
        return ResponseEntity.ok(interaccionService.crearInteraccion(leadId, interaccion, proximoContacto));
    }

}
