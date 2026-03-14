package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.repository.MetasRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class MetasTestController {

    private final MetasRepository metasRepository;

    public MetasTestController(MetasRepository metasRepository) {
        this.metasRepository = metasRepository;
    }

    @GetMapping("/metas")
    public Map<String, Long> testMetas(
            @RequestParam Integer municipioId,
            @RequestParam String behavior
    ) {
        return metasRepository.findMetasByMunicipioAndBehavior(municipioId, behavior);
    }
}