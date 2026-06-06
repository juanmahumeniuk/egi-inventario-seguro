package com.itu.egi.inventarioseguro.service;

import com.itu.egi.inventarioseguro.dto.*;
import com.itu.egi.inventarioseguro.model.*;
import com.itu.egi.inventarioseguro.repository.sql.MaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaMaquinaRepository;
import com.itu.egi.inventarioseguro.repository.sql.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaquinaServiceTest {

    @Mock MaquinaRepository maquinaRepository;
    @Mock MongoTemplate mongoTemplate;
    @Mock PersonaMaquinaRepository asignacionRepository;
    @Mock PersonaRepository personaRepository;
    @Mock PersonaService personaService;

    @InjectMocks MaquinaService maquinaService;

    private Maquina maquina;
    private MaquinaHardware hardware;

    @BeforeEach
    void setUp() {
        maquina = new Maquina();
        maquina.setId(1L);
        maquina.setAula(Aula.LABORATORIO_SO);
        maquina.setNumeroMesa(5);
        maquina.setFechaMantenimiento(LocalDate.of(2026, 12, 1));

        Disco disco = new Disco();
        disco.setTipo("SSD");
        disco.setCapacidadGb(512);

        hardware = new MaquinaHardware();
        hardware.setId(1L);
        hardware.setFabricante("Dell");
        hardware.setModelo("OptiPlex 7090");
        hardware.setTipo(TipoEquipo.DESKTOP);
        hardware.setCpu("Intel Core i5-11500");
        hardware.setRamGb(16);
        hardware.setDisco(disco);
        hardware.setSistemaOperativo("Windows 11 Pro");
    }

    @Test
    void findAll_cuandoExistenMaquinas_devuelveLista() {
        when(maquinaRepository.findAll()).thenReturn(List.of(maquina));
        when(mongoTemplate.findById(1L, MaquinaHardware.class)).thenReturn(hardware);
        when(asignacionRepository.findByMaquinaId(1L)).thenReturn(List.of());

        List<MaquinaConAsignacionesDTO> resultado = maquinaService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(1L);
        assertThat(resultado.get(0).getEspecificaciones().getFabricante()).isEqualTo("Dell");
        assertThat(resultado.get(0).getEspecificaciones().getRamGb()).isEqualTo(16);
        assertThat(resultado.get(0).getAsignaciones()).isEmpty();
    }

    @Test
    void findAll_cuandoMongoNoTieneDatos_devuelveEspecificacionesVacias() {
        when(maquinaRepository.findAll()).thenReturn(List.of(maquina));
        when(mongoTemplate.findById(1L, MaquinaHardware.class)).thenReturn(null);
        when(asignacionRepository.findByMaquinaId(1L)).thenReturn(List.of());

        List<MaquinaConAsignacionesDTO> resultado = maquinaService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEspecificaciones().getFabricante()).isNull();
    }

    @Test
    void findById_cuandoExiste_devuelveDTOCompleto() {
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(mongoTemplate.findById(1L, MaquinaHardware.class)).thenReturn(hardware);
        when(asignacionRepository.findByMaquinaId(1L)).thenReturn(List.of());

        MaquinaConAsignacionesDTO resultado = maquinaService.findById(1L);

        assertThat(resultado.getNumeroMesa()).isEqualTo(5);
        assertThat(resultado.getAula()).isEqualTo(Aula.LABORATORIO_SO);
        assertThat(resultado.getEspecificaciones().getDisco().getTipo()).isEqualTo("SSD");
        assertThat(resultado.getEspecificaciones().getDisco().getCapacidadGb()).isEqualTo(512);
    }

    @Test
    void findById_cuandoNoExiste_lanza404() {
        when(maquinaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maquinaService.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Maquina no encontrada");
    }

    @Test
    void create_guardaEnSqlYEnMongo() {
        Maquina guardada = new Maquina();
        guardada.setId(10L);
        guardada.setAula(Aula.LABORATORIO_SO);
        guardada.setNumeroMesa(7);

        when(maquinaRepository.save(any())).thenReturn(guardada);

        maquinaService.create(buildRequest());

        verify(maquinaRepository).save(any(Maquina.class));
        verify(mongoTemplate).insert(any(MaquinaHardware.class));
    }

    @Test
    void delete_cuandoExiste_eliminaDeSqlYMongo() {
        when(maquinaRepository.existsById(1L)).thenReturn(true);

        maquinaService.delete(1L);

        verify(maquinaRepository).deleteById(1L);
        verify(mongoTemplate).remove(any(Query.class), eq(MaquinaHardware.class));
    }

    @Test
    void delete_cuandoNoExiste_lanza404SinTocarBD() {
        when(maquinaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> maquinaService.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Maquina no encontrada");

        verify(maquinaRepository, never()).deleteById(any());
        verify(mongoTemplate, never()).remove(any(Query.class), eq(MaquinaHardware.class));
    }

    private MaquinaRequest buildRequest() {
        DiscoRequest disco = new DiscoRequest();
        disco.setTipo("SSD");
        disco.setCapacidadGb(256);

        EspecificacionesRequest specs = new EspecificacionesRequest();
        specs.setFabricante("Lenovo");
        specs.setModelo("ThinkCentre M90n");
        specs.setTipo(TipoEquipo.DESKTOP);
        specs.setCpu("Intel Core i7");
        specs.setRamGb(32);
        specs.setDisco(disco);
        specs.setSistemaOperativo("Ubuntu 22.04");

        MaquinaRequest req = new MaquinaRequest();
        req.setAula(Aula.LABORATORIO_SO);
        req.setNumeroMesa(7);
        req.setEspecificaciones(specs);
        return req;
    }
}
