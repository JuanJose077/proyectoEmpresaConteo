package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.service.CoberturaMunicipioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cobertura")
public class CoberturaMunicipioController {

    private final CoberturaMunicipioService service;

    public CoberturaMunicipioController(CoberturaMunicipioService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public CoberturaMunicipioService.ResumenCobertura getResumen(
            @RequestParam String departamento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String jornada,
            @RequestParam(required = false) String dia
    ) {
        return service.getResumenPorDepartamento(departamento, fechaInicio, fechaFin, jornada, dia);
    }

    @GetMapping("/detalle")
    public List<CoberturaMunicipioService.DetalleMunicipio> getDetalle(
            @RequestParam String departamento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String jornada,
            @RequestParam(required = false) String dia
    ) {
        return service.getDetallePorDepartamento(departamento, fechaInicio, fechaFin, jornada, dia);
    }

    @GetMapping("/ranking-mas")
    public List<CoberturaMunicipioService.RankingMunicipio> getRankingMas(
            @RequestParam String departamento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String jornada,
            @RequestParam(required = false) String dia,
            @RequestParam(defaultValue = "5") int limite
    ) {
        return service.getRankingMasConteos(departamento, fechaInicio, fechaFin, jornada, dia, limite);
    }

    @GetMapping("/ranking-menos")
    public List<CoberturaMunicipioService.RankingMunicipio> getRankingMenos(
            @RequestParam String departamento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String jornada,
            @RequestParam(required = false) String dia,
            @RequestParam(defaultValue = "5") int limite
    ) {
        return service.getRankingMenosConteos(departamento, fechaInicio, fechaFin, jornada, dia, limite);
    }
}