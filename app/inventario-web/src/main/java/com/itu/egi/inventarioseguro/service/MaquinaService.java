package com.itu.egi.inventarioseguro.service;

import com.itu.egi.inventarioseguro.dto.*;
import com.itu.egi.inventarioseguro.model.Maquina;
import com.itu.egi.inventarioseguro.model.MaquinaHardware;
import com.itu.egi.inventarioseguro.model.PersonaMaquina;
import com.itu.egi.inventarioseguro.repository.sql.MaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaMaquinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final MongoTemplate mongoTemplate;
    private final PersonaMaquinaRepository asignacionRepository;
    private final PersonaService personaService;

    @Transactional(readOnly = true)
    public List<MaquinaResumenDTO> findAll() {
        return maquinaRepository.findAll().stream().map(this::toResumen).toList();
    }

    @Transactional(readOnly = true)
    public MaquinaDetalleDTO findById(Long id) {
        Maquina maquina = getMaquinaOrThrow(id);
        MaquinaHardware hardware = getHardwareOrThrow(id);
        List<PersonaMaquina> asignaciones = asignacionRepository.findByMaquinaId(id);
        return toDetalle(maquina, hardware, asignaciones);
    }

    public MaquinaDetalleDTO create(MaquinaRequest req) {
        Maquina maquina = new Maquina();
        applySql(maquina, req);
        maquina = maquinaRepository.save(maquina);

        MaquinaHardware hardware = buildHardware(req.getHardware(), maquina.getId());
        mongoTemplate.insert(hardware);

        return toDetalle(maquina, hardware, List.of());
    }

    public MaquinaDetalleDTO update(Long id, MaquinaRequest req) {
        Maquina maquina = getMaquinaOrThrow(id);
        applySql(maquina, req);
        maquina = maquinaRepository.save(maquina);

        MaquinaHardware hardware = getHardwareOrThrow(id);
        applyHardware(hardware, req.getHardware());
        mongoTemplate.save(hardware);

        List<PersonaMaquina> asignaciones = asignacionRepository.findByMaquinaId(id);
        return toDetalle(maquina, hardware, asignaciones);
    }

    public void delete(Long id) {
        if (!maquinaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id);
        }
        maquinaRepository.deleteById(id);
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)), MaquinaHardware.class);
    }

    @Transactional(readOnly = true)
    public List<PersonaDTO> findPersonasByMaquinaId(Long id) {
        if (!maquinaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id);
        }
        return asignacionRepository.findByMaquinaId(id).stream()
                .map(pm -> personaService.toDTO(pm.getPersona()))
                .toList();
    }

    private Maquina getMaquinaOrThrow(Long id) {
        return maquinaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id));
    }

    private MaquinaHardware getHardwareOrThrow(Long id) {
        MaquinaHardware h = mongoTemplate.findById(id, MaquinaHardware.class);
        if (h == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hardware no encontrado para maquina: " + id);
        return h;
    }

    private void applySql(Maquina maquina, MaquinaRequest req) {
        maquina.setAula(req.getAula());
        maquina.setNumeroMesa(req.getNumeroMesa());
        maquina.setFechaMantenimiento(req.getFechaMantenimiento());
    }

    private MaquinaHardware buildHardware(HardwareRequest req, Long id) {
        MaquinaHardware h = new MaquinaHardware();
        h.setId(id);
        applyHardware(h, req);
        return h;
    }

    private void applyHardware(MaquinaHardware h, HardwareRequest req) {
        h.setFabricante(req.getFabricante());
        h.setModelo(req.getModelo());
        h.setCpu(req.getCpu());
        h.setRam(req.getRam());
        h.setDisco(req.getDisco());
        h.setTipo(req.getTipo());
        h.setOs(req.getOs());
        h.setMonitor(req.getMonitor());
        h.setMouse(req.getMouse());
        h.setTeclado(req.getTeclado());
    }

    private MaquinaResumenDTO toResumen(Maquina m) {
        MaquinaResumenDTO dto = new MaquinaResumenDTO();
        dto.setId(m.getId());
        dto.setAula(m.getAula());
        dto.setNumeroMesa(m.getNumeroMesa());
        dto.setFechaMantenimiento(m.getFechaMantenimiento());
        return dto;
    }

    private MaquinaDetalleDTO toDetalle(Maquina m, MaquinaHardware h, List<PersonaMaquina> asignaciones) {
        MaquinaDetalleDTO dto = new MaquinaDetalleDTO();
        dto.setId(m.getId());
        dto.setAula(m.getAula());
        dto.setNumeroMesa(m.getNumeroMesa());
        dto.setFechaMantenimiento(m.getFechaMantenimiento());
        dto.setFabricante(h.getFabricante());
        dto.setModelo(h.getModelo());
        dto.setCpu(h.getCpu());
        dto.setRam(h.getRam());
        dto.setDisco(h.getDisco());
        dto.setTipo(h.getTipo());
        dto.setOs(h.getOs());
        dto.setMonitor(h.getMonitor());
        dto.setMouse(h.getMouse());
        dto.setTeclado(h.getTeclado());
        dto.setPersonas(asignaciones.stream().map(pm -> personaService.toDTO(pm.getPersona())).toList());
        return dto;
    }
}
