package com.itu.egi.inventarioseguro.service;

import com.itu.egi.inventarioseguro.dto.PersonaDTO;
import com.itu.egi.inventarioseguro.dto.PersonaRequest;
import com.itu.egi.inventarioseguro.model.Persona;
import com.itu.egi.inventarioseguro.repository.sql.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;

    @Transactional(readOnly = true)
    public List<PersonaDTO> findAll() {
        return personaRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PersonaDTO findById(Long id) {
        return personaRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + id));
    }

    public PersonaDTO create(PersonaRequest req) {
        Persona persona = new Persona();
        apply(persona, req);
        return toDTO(personaRepository.save(persona));
    }

    public PersonaDTO update(Long id, PersonaRequest req) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + id));
        apply(persona, req);
        return toDTO(personaRepository.save(persona));
    }

    public void delete(Long id) {
        if (!personaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + id);
        }
        personaRepository.deleteById(id);
    }

    private void apply(Persona persona, PersonaRequest req) {
        persona.setNombre(req.getNombre());
        persona.setApellido(req.getApellido());
        persona.setCategoria(req.getCategoria());
    }

    public PersonaDTO toDTO(Persona persona) {
        PersonaDTO dto = new PersonaDTO();
        dto.setId(persona.getId());
        dto.setNombre(persona.getNombre());
        dto.setApellido(persona.getApellido());
        dto.setCategoria(persona.getCategoria());
        return dto;
    }
}
