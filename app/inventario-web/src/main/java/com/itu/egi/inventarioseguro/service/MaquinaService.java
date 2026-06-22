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

/**
 * Servicio encargado de la gestión de la lógica de negocio de las máquinas de inventario.
 * Coordina el acceso híbrido a bases de datos relacionales (SQL a través de {@link MaquinaRepository} y 
 * {@link PersonaMaquinaRepository}) para el registro básico y las asignaciones, y bases de datos NoSQL 
 * (MongoDB a través de {@link MongoTemplate}) para las especificaciones detalladas de hardware de cada máquina.
 */
@Service
@RequiredArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final MongoTemplate mongoTemplate;
    private final PersonaMaquinaRepository asignacionRepository;
    private final PersonaRepository personaRepository;
    private final PersonaService personaService;

    /**
     * Recupera todas las máquinas registradas junto con sus especificaciones de hardware y asignaciones de personas.
     *
     * @return Lista de objetos {@link MaquinaConAsignacionesDTO} con toda la información consolidada.
     */
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

    /**
     * Busca una máquina por su identificador único, consolidando su información SQL, NoSQL y de asignaciones.
     *
     * @param id Identificador único de la máquina.
     * @return DTO consolidado {@link MaquinaConAsignacionesDTO} con los datos de la máquina.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la máquina o sus especificaciones de hardware no existen.
     */
    @Transactional(readOnly = true)
    public MaquinaConAsignacionesDTO findById(Long id) {
        Maquina m = getMaquinaOrThrow(id);
        MaquinaHardware hw = getHardwareOrThrow(id);
        List<PersonaMaquina> asignaciones = asignacionRepository.findByMaquinaId(id);
        return toDTO(m, hw, asignaciones);
    }

    /**
     * Crea un nuevo registro de máquina. Registra los datos de ubicación en la base de datos SQL,
     * inserta las especificaciones de hardware en MongoDB, guarda las asignaciones iniciales en SQL,
     * y retorna el objeto consolidado creado.
     *
     * @param req Datos de creación de la máquina (incluye especificaciones y asignaciones opcionales).
     * @return El DTO {@link MaquinaConAsignacionesDTO} de la máquina creada.
     */
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

    /**
     * Actualiza los datos de una máquina existente. Modifica los registros en la base de datos SQL
     * y en MongoDB (especificaciones de hardware), sobrescribe el listado de asignaciones asociadas,
     * y retorna el objeto consolidado actualizado.
     *
     * @param id Identificador de la máquina a actualizar.
     * @param req Nuevos datos a aplicar.
     * @return El DTO {@link MaquinaConAsignacionesDTO} actualizado.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la máquina o sus especificaciones no existen.
     */
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

    /**
     * Elimina por completo una máquina del inventario, borrando tanto su registro en la base de datos SQL
     * como sus especificaciones de hardware en MongoDB.
     *
     * @param id Identificador de la máquina a eliminar.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la máquina no existe.
     */
    @Transactional
    public void delete(Long id) {
        if (!maquinaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id);
        }
        maquinaRepository.deleteById(id);
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)), MaquinaHardware.class);
    }

    /**
     * Guarda la lista de asignaciones de personas a una máquina determinada.
     *
     * @param maquina Entidad Maquina a la cual asignar.
     * @param reqs Lista de solicitudes de asignación en máquina.
     * @return Lista de entidades de asignación {@link PersonaMaquina} guardadas.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si alguna persona en el listado no existe.
     */
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

    /**
     * Busca la entidad Maquina en la base de datos relacional u obtiene una excepción si no existe.
     *
     * @param id Identificador de la máquina.
     * @return La entidad {@link Maquina} encontrada.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si la máquina no se encuentra.
     */
    private Maquina getMaquinaOrThrow(Long id) {
        return maquinaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maquina no encontrada: " + id));
    }

    /**
     * Busca el documento MaquinaHardware en MongoDB u obtiene una excepción si no existe.
     *
     * @param id Identificador de la máquina (que coincide con el _id del hardware).
     * @return El documento {@link MaquinaHardware} encontrado.
     * @throws ResponseStatusException Con estado 404 (NOT_FOUND) si el hardware no se encuentra.
     */
    private MaquinaHardware getHardwareOrThrow(Long id) {
        MaquinaHardware h = mongoTemplate.findById(id, MaquinaHardware.class);
        if (h == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Hardware no encontrado para maquina: " + id);
        return h;
    }

    /**
     * Aplica los campos de ubicación y mantenimiento del DTO a la entidad Maquina.
     *
     * @param m Entidad Maquina de destino.
     * @param req Datos de origen.
     */
    private void applySql(Maquina m, MaquinaRequest req) {
        m.setAula(req.getAula());
        m.setNumeroMesa(req.getNumeroMesa());
        m.setFechaMantenimiento(req.getFechaMantenimiento());
    }

    /**
     * Crea un nuevo documento MaquinaHardware para MongoDB a partir del ID de máquina y las especificaciones.
     *
     * @param req Especificaciones de hardware origen.
     * @param id Identificador que se usará para el documento.
     * @return El documento {@link MaquinaHardware} construido.
     */
    private MaquinaHardware buildHardware(EspecificacionesRequest req, Long id) {
        MaquinaHardware h = new MaquinaHardware();
        h.setId(id);
        applyHardware(h, req);
        return h;
    }

    /**
     * Aplica los valores de especificaciones del DTO al documento MaquinaHardware.
     *
     * @param h Documento MaquinaHardware de destino.
     * @param req Datos de origen.
     */
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

    /**
     * Consolida la entidad Maquina, el documento MaquinaHardware y sus asignaciones en un DTO unificado.
     *
     * @param m Entidad Maquina.
     * @param hw Documento MaquinaHardware.
     * @param asignaciones Lista de asignaciones activas.
     * @return El DTO consolidado {@link MaquinaConAsignacionesDTO}.
     */
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

    /**
     * Convierte el documento MaquinaHardware a su DTO correspondiente.
     *
     * @param maquinaId Identificador de la máquina asociada.
     * @param hw Documento MaquinaHardware origen.
     * @return El DTO {@link EspecificacionesDTO} mapeado.
     */
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

    /**
     * Convierte la entidad de asignación PersonaMaquina a su correspondiente DTO.
     *
     * @param pm Entidad de asignación.
     * @return El DTO {@link AsignacionDTO} mapeado.
     */
    private AsignacionDTO toAsignacionDTO(PersonaMaquina pm) {
        AsignacionDTO dto = new AsignacionDTO();
        dto.setPersona(personaService.toDTO(pm.getPersona()));
        dto.setFechaAsignado(pm.getFechaAsignado());
        return dto;
    }
}
