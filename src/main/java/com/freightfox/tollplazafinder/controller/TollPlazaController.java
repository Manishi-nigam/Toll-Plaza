package com.freightfox.tollplazafinder.controller;

import com.freightfox.tollplazafinder.entity.TollPlaza;
import com.freightfox.tollplazafinder.service.TollPlazaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/toll-plazas")
public class TollPlazaController {
    private final TollPlazaService tollPlazaService;

    public TollPlazaController(TollPlazaService tollPlazaService) {
        this.tollPlazaService = tollPlazaService;
    }

    @GetMapping
    public List<TollPlaza> getAllTollPlazas() {
        return tollPlazaService.getAllTollPlazas();
    }
}
