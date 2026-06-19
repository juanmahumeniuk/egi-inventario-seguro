package com.itu.egi.inventarioseguro.repository.sql;

import com.itu.egi.inventarioseguro.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
}
