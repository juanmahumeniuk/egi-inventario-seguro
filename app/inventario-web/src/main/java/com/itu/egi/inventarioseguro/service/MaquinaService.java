package com.itu.egi.inventarioseguro.service;

import com.itu.egi.inventarioseguro.dto.*;
import com.itu.egi.inventarioseguro.model.*;
import com.itu.egi.inventarioseguro.repository.sql.MaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaMaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final MongoTemplate mongoTemplate;
    private final PersonaMaquinaRepository asignacionRepository;
    private final PersonaRepository personaRepository;
    private final PersonaService personaService;

    @Transactional(readOnly = true)
    public List<MaquinaConAsignacionesDTO> findAll() {
        return maquinaRepository.findAll().stream()
                .map(m -> {
                    MaquinaHardware hw = mongoTemplate.findById(m.getId(), MaquinaHardware.class);
                    List<PersonaMaquina> asignaciones = asignacionRepository.findByMaquinaId(m.getId());
                    return toDTO(m, hw, asignaciones);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MaquinaConAsignacionesDTO findById(Long id) {
        Maquina m = getMaquinaOrThrow(id);
        MaquinaHardware hw = getHardwareOrThrow(id);
        List<PersonaMaquina> asignaciones = asignacionRepository.findByMaquinaId(id);
        return toDTO(m, hw, asignaciones);
    }

    @Transactional
    public MaquinaConAsignacionesDTO create(MaquinaRequest req) {
        Maquina maquina = new Maquina();
        applySql(maquina, req);
        maquina = maquinaRepository.save(maquina);

        MaquinaHardware hw = buildHardware(req.getEspecificaciones(), maquina.getId());
        mongoTemplate.insert(hw);

        List<PersonaMaquina> asignaciones = guardarAsignaciones(maquina, req.getAsignaciones());
        return toDTO(maquina, hw, asignaciones);
    }

    @Transactional
    public MaquinaConAsignacionesDTO update(Long id, MaquinaRequest req) {
        Maquina maquina = getMaquinaOrThrow(id);
        applySql(maquina, req);
        maquina = maquinaRepository.save(maquina);

        MaquinaHardware hw = getHardwareOrThrow(id);
        applyHardware(hw, req.getEspecificaciones());
        mongoTemplate.save(hw);

        asignacionRepository.deleteAll(asignacionRepository.findByMaquinaId(id));
        List<PersonaMaquina> asignaciones = guardarAsignaciones(maquina, req.getAsignaciones());
        return toDTO(maquina, hw, asignaciones);
    }

    @Transactional
    public void delete(Long id) {
        if (!maquinaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id);
        }
        maquinaRepository.deleteById(id);
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)), MaquinaHardware.class);
    }

    private List<PersonaMaquina> guardarAsignaciones(Maquina maquina, List<AsignacionEnMaquinaRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) return List.of();
        return reqs.stream().map(r -> {
            Persona persona = personaRepository.findById(r.getPersonaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Persona no encontrada: " + r.getPersonaId()));
            PersonaMaquina pm = new PersonaMaquina();
            pm.setPersona(persona);
            pm.setMaquina(maquina);
            pm.setFechaAsignado(r.getFechaAsignado() != null ? r.getFechaAsignado() : LocalDate.now());
            return asignacionRepository.save(pm);
        }).toList();
    }

    private Maquina getMaquinaOrThrow(Long id) {
        return maquinaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id));
    }

    private MaquinaHardware getHardwareOrThrow(Long id) {
        MaquinaHardware h = mongoTemplate.findById(id, MaquinaHardware.class);
        if (h == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Hardware no encontrado para maquina: " + id);
        return h;
    }

    private void applySql(Maquina m, MaquinaRequest req) {
        m.setAula(req.getAula());
        m.setNumeroMesa(req.getNumeroMesa());
        m.setFechaMantenimiento(req.getFechaMantenimiento());
    }

    private MaquinaHardware buildHardware(EspecificacionesRequest req, Long id) {
        MaquinaHardware h = new MaquinaHardware();
        h.setId(id);
        applyHardware(h, req);
        return h;
    }

    private void applyHardware(MaquinaHardware h, EspecificacionesRequest req) {
        h.setFabricante(req.getFabricante());
        h.setModelo(req.getModelo());
        h.setTipo(req.getTipo());
        h.setCpu(req.getCpu());
        h.setRamGb(req.getRamGb());
        if (req.getDisco() != null) {
            Disco disco = new Disco();
            disco.setTipo(req.getDisco().getTipo());
            disco.setCapacidadGb(req.getDisco().getCapacidadGb());
            h.setDisco(disco);
        }
        h.setSistemaOperativo(req.getSistemaOperativo());
        if (req.getPerifericos() != null) {
            Perifericos p = new Perifericos();
            p.setMonitor(req.getPerifericos().getMonitor());
            p.setMouse(req.getPerifericos().getMouse());
            p.setTeclado(req.getPerifericos().getTeclado());
            h.setPerifericos(p);
        }
    }

    private MaquinaConAsignacionesDTO toDTO(Maquina m, MaquinaHardware hw, List<PersonaMaquina> asignaciones) {
        MaquinaConAsignacionesDTO dto = new MaquinaConAsignacionesDTO();
        dto.setId(m.getId());
        dto.setNumeroMesa(m.getNumeroMesa());
        dto.setFechaMantenimiento(m.getFechaMantenimiento());
        dto.setAula(m.getAula());
        dto.setEspecificaciones(toEspecificaciones(m.getId(), hw));
        dto.setAsignaciones(asignaciones.stream().map(this::toAsignacionDTO).toList());
        return dto;
    }

    private EspecificacionesDTO toEspecificaciones(Long maquinaId, MaquinaHardware hw) {
        EspecificacionesDTO dto = new EspecificacionesDTO();
        dto.setMaquinaId(maquinaId);
        if (hw == null) return dto;
        dto.setFabricante(hw.getFabricante());
        dto.setModelo(hw.getModelo());
        dto.setTipo(hw.getTipo());
        dto.setCpu(hw.getCpu());
        dto.setRamGb(hw.getRamGb());
        if (hw.getDisco() != null) {
            DiscoDTO d = new DiscoDTO();
            d.setTipo(hw.getDisco().getTipo());
            d.setCapacidadGb(hw.getDisco().getCapacidadGb());
            dto.setDisco(d);
        }
        dto.setSistemaOperativo(hw.getSistemaOperativo());
        if (hw.getPerifericos() != null) {
            PerifericosDTO p = new PerifericosDTO();
            p.setMonitor(hw.getPerifericos().getMonitor());
            p.setMouse(hw.getPerifericos().getMouse());
            p.setTeclado(hw.getPerifericos().getTeclado());
            dto.setPerifericos(p);
        }
        return dto;
    }

    private AsignacionDTO toAsignacionDTO(PersonaMaquina pm) {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setPersona(personaService.toDTO(pm.getPersona()));
        dto.setFechaAsignado(pm.getFechaAsignado());
        return dto;
    }
}
