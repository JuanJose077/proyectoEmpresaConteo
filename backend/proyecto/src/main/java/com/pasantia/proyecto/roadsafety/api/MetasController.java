package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.dto.MetaCellDto;
import com.pasantia.proyecto.roadsafety.service.MetasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/metas")
public class MetasController {

    private final MetasService metasService;

    public MetasController(MetasService metasService) {
        this.metasService = metasService;
    }

    @GetMapping
    public List<MetaCellDto> getMetas(@RequestParam int municipioId) {
        return metasService.getMetasByMunicipio(municipioId);
    }
}
