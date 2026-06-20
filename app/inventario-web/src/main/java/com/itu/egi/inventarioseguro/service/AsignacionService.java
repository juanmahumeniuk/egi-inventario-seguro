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

/**
 * Servicio encargado de gestionar las asignaciones y desasignaciones de máquinas a personas.
 * Esta clase contiene la lógica de negocio para validar la existencia de las entidades involucradas,
 * asegurar la consistencia transaccional y guardar/eliminar registros de la tabla intermedia PersonaMaquina.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AsignacionService {

    private final PersonaMaquinaRepository asignacionRepository;
    private final PersonaRepository personaRepository;
    private final MaquinaRepository maquinaRepository;

    /**
     * Asigna una máquina a una persona específica en una fecha determinada.
     * Si no se provee una fecha de asignación, se utiliza la fecha actual del sistema.
     *
     * @param req Objeto DTO {@link AsignacionRequest} que contiene el ID de la persona, el ID de la máquina y la fecha opcional.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la persona o la máquina no existen.
     * @throws ResponseStatusException Con estado 409 (CONFLICT) si la asignación ya existe en la base de datos.
     */
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

    /**
     * Elimina la asignación existente entre una persona y una máquina.
     *
     * @param personaId Identificador de la persona.
     * @param maquinaId Identificador de la máquina.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la asignación no existe en la base de datos.
     */
    public void desasignar(Long personaId, Long maquinaId) {
        PersonaMaquinaId id = new PersonaMaquinaId(personaId, maquinaId);
        if (!asignacionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
        }
        asignacionRepository.deleteById(id);
    }
}
