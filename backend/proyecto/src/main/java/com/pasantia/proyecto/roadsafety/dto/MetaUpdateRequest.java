package com.pasantia.proyecto.roadsafety.dto;

public record MetaUpdateRequest(
        long id,
        Long meta,
        Boolean activo
) {}
