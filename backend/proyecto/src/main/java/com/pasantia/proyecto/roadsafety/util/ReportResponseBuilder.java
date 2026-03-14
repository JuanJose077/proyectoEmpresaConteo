package com.pasantia.proyecto.roadsafety.util;

import java.util.*;

public final class ReportResponseBuilder {

    private ReportResponseBuilder() {}

    public static Map<String, Object> build(
            String behavior,
            String label,
            double targetPercent,
            List<Map<String, Object>> byVehicleRows,
            Integer municipioId,
            Integer puntoId,
            String fechaInicio,
            String fechaFin,
            String jornada,
            String dia
    ) {

        long totalAll = 0L;
        long cumpleAll = 0L;

        List<Map<String, Object>> normalized = new ArrayList<>();

        for (Map<String, Object> r : byVehicleRows) {
            long total = toLong(r.get("total"));
            long cumple = toLong(r.get("cumple"));
            long noCumple = Math.max(0L, total - cumple);

            totalAll += total;
            cumpleAll += cumple;

            Map<String, Object> nr = new LinkedHashMap<>(r);
            nr.put("total", total);
            nr.put("cumple", cumple);
            nr.put("noCumple", noCumple);
            normalized.add(nr);
        }

        long noCumpleAll = Math.max(0L, totalAll - cumpleAll);

        double compliancePercent = totalAll == 0 ? 0.0 : (cumpleAll * 100.0) / totalAll;
        compliancePercent = Math.round(compliancePercent * 100.0) / 100.0;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("behavior", behavior);
        row.put("label", label);
        row.put("total", totalAll);
        row.put("cumple", cumpleAll);
        row.put("noCumple", noCumpleAll);
        row.put("compliancePercent", compliancePercent);
        row.put("byVehicle", normalized);
        row.put("targetPercent", targetPercent);

        Map<String, Object> metaInfo = new LinkedHashMap<>();
        metaInfo.put("municipioId", municipioId);
        metaInfo.put("puntoId", puntoId);
        metaInfo.put("fechaInicio", fechaInicio);
        metaInfo.put("fechaFin", fechaFin);
        metaInfo.put("jornada", jornada);
        metaInfo.put("dia", dia);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("table", List.of(row));
        resp.put("metaInfo", metaInfo);
        return resp;
    }

    private static long toLong(Object x) {
        if (x == null) return 0L;
        if (x instanceof Number n) return n.longValue();
        try { return Long.parseLong(x.toString()); }
        catch (Exception e) { return 0L; }
    }
}