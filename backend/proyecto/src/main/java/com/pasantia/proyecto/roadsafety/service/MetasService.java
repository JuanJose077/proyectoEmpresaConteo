package com.pasantia.proyecto.roadsafety.service;

import com.pasantia.proyecto.roadsafety.dto.MetaCellDto;
import com.pasantia.proyecto.roadsafety.dto.MetaUpdateRequest;
import com.pasantia.proyecto.roadsafety.repository.MetasRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetasService {

    private final MetasRepository metasRepository;

    public MetasService(MetasRepository metasRepository) {
        this.metasRepository = metasRepository;
    }

    public List<MetaCellDto> getMetasByMunicipio(int municipioId) {
        return metasRepository.findMetasByMunicipio(municipioId);
    }

    public int[] updateBatch(List<MetaUpdateRequest> updates) {
        return metasRepository.updateMetasBatch(updates);
    }
}
