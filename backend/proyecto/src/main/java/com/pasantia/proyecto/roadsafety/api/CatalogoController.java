package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.service.CatalogoService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/departamentos")
    public Map<String, Object> departamentos() {
        List<String> items = catalogoService.getDepartamentosConDatos();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        return response;
    }
    @GetMapping("/departamentos-todos")
    public Map<String, Object> getTodosLosDepartamentos() {
        List<String> items = catalogoService.getTodosLosDepartamentos();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        return response;
    }

    @GetMapping("/municipios")
    public Map<String, Object> municipios(@RequestParam String departamento) {
        List<CatalogoService.MunicipioItem> items =
                catalogoService.getMunicipiosConDatosPorDepartamento(departamento);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("departamento", departamento);
        response.put("items", items);
        return response;
    }
}