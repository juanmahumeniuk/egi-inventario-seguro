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

/**
 * Servicio encargado de gestionar las operaciones CRUD y lógica de negocio relacionadas con las personas.
 * Interactúa con el repositorio relacional {@link PersonaRepository} y realiza conversiones 
 * bidireccionales entre entidades y objetos de transferencia de datos (DTO).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;

    /**
     * Recupera todas las personas registradas en el sistema.
     *
     * @return Lista de objetos {@link PersonaDTO} correspondientes a las personas encontradas.
     */
    @Transactional(readOnly = true)
    public List<PersonaDTO> findAll() {
        return personaRepository.findAll().stream().map(this::toDTO).toList();
    }

    /**
     * Busca y retorna una persona específica a partir de su identificador.
     *
     * @param id Identificador único de la persona.
     * @return El DTO {@link PersonaDTO} de la persona encontrada.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si no se encuentra la persona.
     */
    @Transactional(readOnly = true)
    public PersonaDTO findById(Long id) {
        return personaRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + id));
    }

    /**
     * Crea y registra una nueva persona en el sistema a partir de los datos provistos.
     *
     * @param req Datos de entrada de la persona a crear.
     * @return El DTO {@link PersonaDTO} de la persona recién creada.
     */
    public PersonaDTO create(PersonaRequest req) {
        Persona persona = new Persona();
        apply(persona, req);
        return toDTO(personaRepository.save(persona));
    }

    /**
     * Actualiza los datos de una persona existente en el sistema.
     *
     * @param id Identificador de la persona a actualizar.
     * @param req Nuevos datos para aplicar a la persona.
     * @return El DTO {@link PersonaDTO} con los datos actualizados.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la persona no existe.
     */
    public PersonaDTO update(Long id, PersonaRequest req) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + id));
        apply(persona, req);
        return toDTO(personaRepository.save(persona));
    }

    /**
     * Elimina una persona del sistema a partir de su identificador.
     *
     * @param id Identificador de la persona a eliminar.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la persona no existe.
     */
    public void delete(Long id) {
        if (!personaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + id);
        }
        personaRepository.deleteById(id);
    }

    /**
     * Aplica los valores de un objeto request a la entidad Persona.
     *
     * @param persona Entidad Persona de destino.
     * @param req Datos de origen.
     */
    private void apply(Persona persona, PersonaRequest req) {
        persona.setNombre(req.getNombre());
        persona.setApellido(req.getApellido());
        persona.setCategoria(req.getCategoria());
    }

    /**
     * Convierte la entidad Persona a su correspondiente representación DTO.
     *
     * @param persona Entidad a convertir.
     * @return El objeto {@link PersonaDTO} mapeado.
     */
    public PersonaDTO toDTO(Persona persona) {
        PersonaDTO dto = new PersonaDTO();
        dto.setId(persona.getId());
        dto.setNombre(persona.getNombre());
        dto.setApellido(persona.getApellido());
        dto.setCategoria(persona.getCategoria());
        return dto;
    }
}
