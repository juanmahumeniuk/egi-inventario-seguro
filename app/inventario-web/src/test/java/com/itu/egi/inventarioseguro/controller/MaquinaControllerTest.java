package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.dto.*;
import com.itu.egi.inventarioseguro.model.Aula;
import com.itu.egi.inventarioseguro.model.TipoEquipo;
import com.itu.egi.inventarioseguro.security.JwtAuthenticationEntryPoint;
import com.itu.egi.inventarioseguro.security.JwtFilter;
import com.itu.egi.inventarioseguro.security.JwtService;
import com.itu.egi.inventarioseguro.service.MaquinaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaquinaController.class)
class MaquinaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  MaquinaService maquinaService;

    // Mocks de seguridad para el contexto de WebMvcTest
    @MockBean JwtService jwtService;
    @MockBean JwtFilter jwtFilter;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private MaquinaConAsignacionesDTO dtoEjemplo() {
        DiscoDTO disco = new DiscoDTO();
        disco.setTipo("SSD");
        disco.setCapacidadGb(512);

        EspecificacionesDTO specs = new EspecificacionesDTO();
        specs.setMaquinaId(1L);
        specs.setFabricante("Dell");
        specs.setModelo("OptiPlex 7090");
        specs.setTipo(TipoEquipo.DESKTOP);
        specs.setCpu("Intel Core i5-11500");
        specs.setRamGb(16);
        specs.setDisco(disco);
        specs.setSistemaOperativo("Windows 11 Pro");

        MaquinaConAsignacionesDTO dto = new MaquinaConAsignacionesDTO();
        dto.setId(1L);
        dto.setAula(Aula.LABORATORIO_SO);
        dto.setNumeroMesa(5);
        dto.setFechaMantenimiento(LocalDate.of(2026, 12, 1));
        dto.setEspecificaciones(specs);
        dto.setAsignaciones(List.of());
        return dto;
    }

    @Test
    @WithMockUser(roles = "READONLY")
    void GET_maquinas_devuelve200_conSnakeCase() throws Exception {
        when(maquinaService.findAll()).thenReturn(List.of(dtoEjemplo()));

        mockMvc.perform(get("/api/maquinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero_mesa").value(5))
                .andExpect(jsonPath("$[0].fecha_mantenimiento").value("2026-12-01"))
                .andExpect(jsonPath("$[0].especificaciones.fabricante").value("Dell"))
                .andExpect(jsonPath("$[0].especificaciones.ram_gb").value(16))
                .andExpect(jsonPath("$[0].especificaciones.tipo").value("desktop"))
                .andExpect(jsonPath("$[0].especificaciones.disco.tipo").value("SSD"))
                .andExpect(jsonPath("$[0].especificaciones.disco.capacidad_gb").value(512))
                .andExpect(jsonPath("$[0].especificaciones.sistema_operativo").value("Windows 11 Pro"))
                .andExpect(jsonPath("$[0].asignaciones").isArray());
    }

    @Test
    @WithMockUser(roles = "READONLY")
    void GET_maquina_porId_devuelve200() throws Exception {
        when(maquinaService.findById(1L)).thenReturn(dtoEjemplo());

        mockMvc.perform(get("/api/maquinas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.aula").value("LABORATORIO_SO"))
                .andExpect(jsonPath("$.especificaciones.maquina_id").value(1));
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void POST_maquinas_conBodyValido_devuelve201() throws Exception {
        when(maquinaService.create(any())).thenReturn(dtoEjemplo());

        String body = """
                {
                  "aula": "LABORATORIO_SO",
                  "numero_mesa": 5,
                  "especificaciones": {
                    "fabricante": "Dell",
                    "modelo": "OptiPlex 7090",
                    "tipo": "desktop",
                    "cpu": "Intel Core i5-11500",
                    "ram_gb": 16,
                    "disco": { "tipo": "SSD", "capacidad_gb": 512 },
                    "sistema_operativo": "Windows 11 Pro"
                  }
                }
                """;

        mockMvc.perform(post("/api/maquinas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void POST_maquinas_sinCamposObligatorios_devuelve400() throws Exception {
        String body = """
                {
                  "aula": "LABORATORIO_SO",
                  "numero_mesa": 5
                }
                """;

        mockMvc.perform(post("/api/maquinas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void PUT_maquina_devuelve200() throws Exception {
        when(maquinaService.update(eq(1L), any())).thenReturn(dtoEjemplo());

        String body = """
                {
                  "aula": "LABORATORIO_SO",
                  "numero_mesa": 5,
                  "especificaciones": {
                    "fabricante": "Dell",
                    "modelo": "OptiPlex 7090",
                    "tipo": "desktop",
                    "cpu": "Intel Core i5-11500",
                    "ram_gb": 16,
                    "disco": { "tipo": "SSD", "capacidad_gb": 512 },
                    "sistema_operativo": "Windows 11 Pro"
                  }
                }
                """;

        mockMvc.perform(put("/api/maquinas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void DELETE_maquina_devuelve204() throws Exception {
        doNothing().when(maquinaService).delete(1L);

        mockMvc.perform(delete("/api/maquinas/1"))
                .andExpect(status().isNoContent());
    }
}
