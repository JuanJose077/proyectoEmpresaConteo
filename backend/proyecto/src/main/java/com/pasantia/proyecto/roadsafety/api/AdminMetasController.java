package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.dto.MetaUpdateRequest;
import com.pasantia.proyecto.roadsafety.service.MetasService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/admin/metas")
public class AdminMetasController {

    private final MetasService metasService;

    public AdminMetasController(MetasService metasService) {
        this.metasService = metasService;
    }

    @PutMapping("/batch")
    public Map<String, Object> updateBatch(@RequestBody List<MetaUpdateRequest> updates) {
        int[] results = metasService.updateBatch(updates);
        int updated = IntStream.of(results).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("updated", updated);
        return response;
    }
}
