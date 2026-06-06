package com.itu.egi.inventarioseguro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void login_conAdmin_devuelveTokenYRolAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_conUsuarioNoAdmin_devuelveRolTecnico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"juan\",\"password\":\"cualquiercosa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("juan"))
                .andExpect(jsonPath("$.role").value("TECNICO"));
    }

    @Test
    void login_conBodyVacio_devuelve200ConUserVacio() throws Exception {
        // El endpoint es permisivo por diseño (LDAP real pendiente)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
