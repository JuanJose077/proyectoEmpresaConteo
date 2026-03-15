package com.pasantia.proyecto.roadsafety.dto;

public record MetaCellDto(
        long id,
        int municipioId,
        String behavior,
        String claseVehiculo,
        long meta,
        boolean activo
) {}
