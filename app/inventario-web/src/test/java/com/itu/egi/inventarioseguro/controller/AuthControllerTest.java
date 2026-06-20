package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.security.JwtAuthenticationEntryPoint;
import com.itu.egi.inventarioseguro.security.JwtFilter;
import com.itu.egi.inventarioseguro.security.JwtService;
import com.itu.egi.inventarioseguro.security.LdapAuthenticationService;
import com.itu.egi.inventarioseguro.security.RoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

        @Autowired
        MockMvc mockMvc;

        @MockBean
        LdapAuthenticationService ldapAuthenticationService;
        @MockBean
        RoleMapper roleMapper;
        @MockBean
        JwtService jwtService;
        @MockBean
        JwtFilter jwtFilter;
        @MockBean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Test
        void login_conAdmin_devuelveTokenYRolAdmin() throws Exception {
                when(ldapAuthenticationService.authenticateAndGetGroups("admin", "admin123"))
                                .thenReturn(Set.of("Grupo_BD_Laboratorio_A"));
                when(roleMapper.mapGroupsToRoles(anyCollection()))
                                .thenReturn(Set.of("ROLE_ADMIN"));
                when(jwtService.generateToken(anyString(), anyCollection()))
                                .thenReturn("mock-real-jwt");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("admin"))
                                .andExpect(jsonPath("$.token").value("mock-real-jwt"))
                                .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        void login_conUsuarioNoAdmin_devuelveRolReadonly() throws Exception {
                when(ldapAuthenticationService.authenticateAndGetGroups("juan", "cualquiercosa"))
                                .thenReturn(Set.of("Grupo_BD_Laboratorio_R"));
                when(roleMapper.mapGroupsToRoles(anyCollection()))
                                .thenReturn(Set.of("ROLE_READONLY"));
                when(jwtService.generateToken(anyString(), anyCollection()))
                                .thenReturn("mock-real-jwt-user");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"juan\",\"password\":\"cualquiercosa\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("juan"))
                                .andExpect(jsonPath("$.role").value("READONLY"));
        }

        @Test
        void login_conBodyVacio_devuelve400() throws Exception {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }
}
