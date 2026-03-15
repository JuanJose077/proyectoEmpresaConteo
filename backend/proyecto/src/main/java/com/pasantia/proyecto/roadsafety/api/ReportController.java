package com.pasantia.proyecto.roadsafety.api;

import com.pasantia.proyecto.roadsafety.repository.EmpresaMunicipioRepository;
import com.pasantia.proyecto.roadsafety.repository.MetasRepository;
import com.pasantia.proyecto.roadsafety.service.*;
import com.pasantia.proyecto.roadsafety.util.ReportResponseBuilder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/reportes")
public class ReportController {

    private final HelmetReportService helmetReportService;
    private final SeatbeltReportService seatbeltReportService;
    private final SpeedReportService speedReportService;
    private final LightsReportService lightsReportService;
    private final ReflectiveReportService reflectiveReportService;
    private final ManeuversReportService maneuversReportService;
    private final PedestrianCrossingReportService pedestrianCrossingReportService;
    private final ChildRestraintReportService childRestraintReportService;
    private final OveroccupancyReportService overoccupancyReportService;
    private final DistractoresReportService distractoresReportService;
    private final ConteoPorPuntoService conteoPorPuntoService;
    private final CatalogValidationService catalogValidationService;
    private final MetasRepository metasRepository;
    private final EmpresaMunicipioRepository empresaMunicipioRepository;

    public ReportController(HelmetReportService helmetReportService,
                            SeatbeltReportService seatbeltReportService,
                            SpeedReportService speedReportService,
                            LightsReportService lightsReportService,
                            ReflectiveReportService reflectiveReportService,
                            ManeuversReportService maneuversReportService,
                            PedestrianCrossingReportService pedestrianCrossingReportService,
                            ChildRestraintReportService childRestraintReportService,
                            OveroccupancyReportService overoccupancyReportService,
                            DistractoresReportService distractoresReportService,
                            ConteoPorPuntoService conteoPorPuntoService,
                            CatalogValidationService catalogValidationService,
                            MetasRepository metasRepository,
                            EmpresaMunicipioRepository empresaMunicipioRepository
    ) {
        this.helmetReportService = helmetReportService;
        this.seatbeltReportService = seatbeltReportService;
        this.speedReportService = speedReportService;
        this.lightsReportService = lightsReportService;
        this.reflectiveReportService = reflectiveReportService;
        this.maneuversReportService = maneuversReportService;
        this.pedestrianCrossingReportService = pedestrianCrossingReportService;
        this.childRestraintReportService = childRestraintReportService;
        this.overoccupancyReportService = overoccupancyReportService;
        this.distractoresReportService = distractoresReportService;
        this.conteoPorPuntoService = conteoPorPuntoService;
        this.catalogValidationService = catalogValidationService;
        this.metasRepository = metasRepository;
        this.empresaMunicipioRepository = empresaMunicipioRepository;
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s);
    }

    // =========================
    // Conteo por punto (ranking)
    // =========================
    @GetMapping("/conteo-por-punto")
    public List<ConteoPorPuntoService.ConteoPorPuntoItem> conteoPorPunto(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia,
            @RequestParam(required = false) Integer limite
    ) {
        if (municipioId != null && puntoId != null) {
            boolean ok = catalogValidationService.puntoPerteneceAMunicipio(puntoId, municipioId);
            if (!ok) {
                throw new IllegalArgumentException(
                        "El puntoId " + puntoId + " no pertenece al municipioId " + municipioId
                );
            }
        }

        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        return conteoPorPuntoService.getRankingPorPunto(
                departamento, municipioId, puntoId, ini, fin, jornada, dia, limite
        );
    }

    // =========================
    // Actividad diaria por punto
    // =========================
    @GetMapping("/conteo-por-punto/actividad-diaria")
    public List<ConteoPorPuntoService.ActividadDiariaItem> actividadDiariaPorPunto(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        if (municipioId != null && puntoId != null) {
            boolean ok = catalogValidationService.puntoPerteneceAMunicipio(puntoId, municipioId);
            if (!ok) {
                throw new IllegalArgumentException(
                        "El puntoId " + puntoId + " no pertenece al municipioId " + municipioId
                );
            }
        }

        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        return conteoPorPuntoService.getActividadDiaria(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );
    }

    // =========================
    // Helmet date range
    // =========================
    @GetMapping("/helmet/date-range")
    public Map<String, Object> helmetDateRange(
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId
    ) {
        return helmetReportService.getHelmetDateRange(municipioId, puntoId);
    }

    // =========================
    // Helmet
    // =========================
    @GetMapping("/helmet")
    public Map<String, Object> helmet(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = helmetReportService.getHelmetStats(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "HELMET",
                "Uso del casco",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Seatbelt
    // =========================
    @GetMapping("/seatbelt")
    public Map<String, Object> seatbelt(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = seatbeltReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "SEATBELT",
                "Uso del cinturón de seguridad",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Speed
    // =========================
    @GetMapping("/speed")
    public Map<String, Object> speed(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = speedReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "SPEED",
                "Cumplimiento del límite de velocidad",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Lights
    // =========================
    @GetMapping("/lights")
    public Map<String, Object> lights(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = lightsReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "LIGHTS",
                "Uso del sistema de luces",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Reflective
    // =========================
    @GetMapping("/reflective")
    public Map<String, Object> reflective(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = reflectiveReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "REFLECTIVE",
                "Uso de prendas reflectivas",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }


    // =========================
    // Maneuvers
    // =========================
    @GetMapping("/maneuvers")
    public Map<String, Object> maneuvers(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = maneuversReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "MANEUVERS",
                "Maniobras de riesgo en vía",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Pedestrian crossing
    // =========================
    @GetMapping("/pedestrian-crossing")
    public Map<String, Object> pedestrianCrossing(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = pedestrianCrossingReportService.getStats(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "PEDESTRIAN_CROSSING",
                "Comportamiento de peatones en cruces",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Child restraint
    // =========================
    @GetMapping("/child-restraint")
    public Map<String, Object> childRestraint(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = childRestraintReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "CHILD_RESTRAINT",
                "Uso de sistemas de retención infantil",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Overoccupancy
    // =========================
    @GetMapping("/overoccupancy")
    public Map<String, Object> overoccupancy(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = overoccupancyReportService.byVehicle(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "OVEROCCUPANCY",
                "Sobreocupación",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }

    // =========================
    // Distractores (UNIFICADO)
    // =========================
    @GetMapping("/distractores")
    public Map<String, Object> distractores(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia
    ) {
        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> rows = distractoresReportService.resumen(
                departamento, municipioId, puntoId, ini, fin, jornada, dia
        );

        return ReportResponseBuilder.build(
                "DISTRACTORES",
                "Actor vial con elementos distractores",
                80.0,
                rows,
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        );
    }


    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
            @RequestParam(required = false) String departamento,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Integer puntoId,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "GENERAL") String jornada,
            @RequestParam(defaultValue = "TODOS") String dia

    ) {

        if (municipioId != null && puntoId != null) {
            boolean ok = catalogValidationService.puntoPerteneceAMunicipio(puntoId, municipioId);
            if (!ok) {
                throw new IllegalArgumentException(
                        "El puntoId " + puntoId + " no pertenece al municipioId " + municipioId
                );
            }
        }

        LocalDate ini = parseDateOrNull(fechaInicio);
        LocalDate fin = parseDateOrNull(fechaFin);

        List<Map<String, Object>> table = new ArrayList<>();

        table.add(extractRow(ReportResponseBuilder.build(
                "SEATBELT",
                "Uso del cinturón de seguridad",
                80.0,
                seatbeltReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "CHILD_RESTRAINT",
                "Uso de sistemas de retención infantil",
                80.0,
                childRestraintReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "SPEED",
                "Cumplimiento del límite de velocidad",
                80.0,
                speedReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "LIGHTS",
                "Uso del sistema de luces",
                80.0,
                lightsReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "HELMET",
                "Uso del casco",
                80.0,
                helmetReportService.getHelmetStats(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "REFLECTIVE",
                "Uso de prendas reflectivas",
                80.0,
                reflectiveReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "OVEROCCUPANCY",
                "Sobreocupación",
                80.0,
                overoccupancyReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "DISTRACTORES",
                "Actor vial con elementos distractores",
                80.0,
                distractoresReportService.resumen(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "MANEUVERS",
                "Maniobras de riesgo en vía",
                80.0,
                maneuversReportService.byVehicle(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        table.add(extractRow(ReportResponseBuilder.build(
                "PEDESTRIAN_CROSSING",
                "Comportamiento de peatones en cruces",
                80.0,
                pedestrianCrossingReportService.getStats(departamento, municipioId, puntoId, ini, fin, jornada, dia),
                municipioId, puntoId, fechaInicio, fechaFin, jornada, dia
        )));

        List<Map<String, Object>> enrichedTable = table.stream()
                .map(row -> enrichRowWithMetasAndCompliance(row, departamento, municipioId))
                .toList();

        Map<String, Object> metaInfo = new LinkedHashMap<>();
        metaInfo.put("departamento", departamento);
        metaInfo.put("municipioId", municipioId);
        metaInfo.put("puntoId", puntoId);
        metaInfo.put("fechaInicio", fechaInicio);
        metaInfo.put("fechaFin", fechaFin);
        metaInfo.put("jornada", jornada);
        metaInfo.put("dia", dia);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("table", enrichedTable);
        response.put("metaInfo", metaInfo);

        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRow(Map<String, Object> built) {
        List<Map<String, Object>> t = (List<Map<String, Object>>) built.get("table");
        return t.get(0);
    }


    private Map<String, Long> resolveMetas(String departamento, Integer municipioId, String behavior) {
        if (municipioId != null) {
            return metasRepository.findMetasByMunicipioAndBehavior(municipioId, behavior);
        }

        if (departamento == null || departamento.isBlank()) {
            return Map.of();
        }

        List<Integer> municipioIds = empresaMunicipioRepository.findMunicipioIdsByDepartamento(departamento);
        if (municipioIds.isEmpty()) {
            return Map.of();
        }

        return metasRepository.findMetasByMunicipiosAndBehavior(municipioIds, behavior);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enrichRowWithMetasAndCompliance(
            Map<String, Object> row,
            String departamento,
            Integer municipioId
    ) {
        String behavior = (String) row.get("behavior");
        Map<String, Long> metaByVehicle = resolveMetas(departamento, municipioId, behavior);

        List<Map<String, Object>> byVehicle = (List<Map<String, Object>>) row.get("byVehicle");
        if (byVehicle == null) byVehicle = List.of();

        Map<String, Long> observByVehicle = new LinkedHashMap<>();
        Map<String, Double> avanceByVehicle = new LinkedHashMap<>();
        Map<String, Double> cumplimientoByVehicle = new LinkedHashMap<>();
        Map<String, Boolean> cumpleMeta80ByVehicle = new LinkedHashMap<>();

        Set<String> allVehicles = new LinkedHashSet<>();
        allVehicles.addAll(metaByVehicle.keySet());

        for (Map<String, Object> item : byVehicle) {
            String clase = stringValue(item.get("claseVehiculo"));
            if (clase == null) clase = stringValue(item.get("clase_vehiculo"));
            if (clase != null) {
                allVehicles.add(clase);
            }
        }

        for (String clase : allVehicles) {
            long meta = metaByVehicle.getOrDefault(clase, 0L);

            long total = 0L;
            long cumple = 0L;

            for (Map<String, Object> item : byVehicle) {
                String claseItem = stringValue(item.get("claseVehiculo"));
                if (claseItem == null) claseItem = stringValue(item.get("clase_vehiculo"));

                if (clase.equals(claseItem)) {
                    total = longValue(item.get("total"));
                    cumple = longValue(item.get("cumple"));
                    break;
                }
            }

            observByVehicle.put(clase, total);

            double avance = meta > 0 ? (total * 100.0 / meta) : 0.0;
            avanceByVehicle.put(clase, round2(avance));

            double cumplimiento = total > 0 ? (cumple * 100.0 / total) : 0.0;
            cumplimientoByVehicle.put(clase, round2(cumplimiento));

            cumpleMeta80ByVehicle.put(clase, cumplimiento >= 80.0);
        }

        row.put("metaByVehicle", metaByVehicle);
        row.put("observByVehicle", observByVehicle);
        row.put("avanceByVehicle", avanceByVehicle);
        row.put("cumplimientoByVehicle", cumplimientoByVehicle);
        row.put("cumpleMeta80ByVehicle", cumpleMeta80ByVehicle);

        return row;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private long longValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
