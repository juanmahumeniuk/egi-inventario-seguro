package com.itu.egi.inventarioseguro.service;

import com.itu.egi.inventarioseguro.dto.AsignacionRequest;
import com.itu.egi.inventarioseguro.model.Maquina;
import com.itu.egi.inventarioseguro.model.Persona;
import com.itu.egi.inventarioseguro.model.PersonaMaquina;
import com.itu.egi.inventarioseguro.model.PersonaMaquinaId;
import com.itu.egi.inventarioseguro.repository.sql.MaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaMaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class AsignacionService {

    private final PersonaMaquinaRepository asignacionRepository;
    private final PersonaRepository personaRepository;
    private final MaquinaRepository maquinaRepository;

    public void asignar(AsignacionRequest req) {
        Persona persona = personaRepository.findById(req.getPersonaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + req.getPersonaId()));
        Maquina maquina = maquinaRepository.findById(req.getMaquinaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + req.getMaquinaId()));

        PersonaMaquinaId id = new PersonaMaquinaId(req.getPersonaId(), req.getMaquinaId());
        if (asignacionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignación ya existe");
        }

        PersonaMaquina pm = new PersonaMaquina();
        pm.setPersona(persona);
        pm.setMaquina(maquina);
        pm.setFechaAsignado(req.getFechaAsignado() != null ? req.getFechaAsignado() : LocalDate.now());
        asignacionRepository.save(pm);
    }

    public void desasignar(Long personaId, Long maquinaId) {
        PersonaMaquinaId id = new PersonaMaquinaId(personaId, maquinaId);
        if (!asignacionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
        }
        asignacionRepository.deleteById(id);
    }
}
