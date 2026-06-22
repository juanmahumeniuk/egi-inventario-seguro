package com.itu.egi.inventarioseguro.repository.sql;

import com.itu.egi.inventarioseguro.model.PersonaMaquina;
import com.itu.egi.inventarioseguro.model.PersonaMaquinaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaMaquinaRepository extends JpaRepository<PersonaMaquina, PersonaMaquinaId> {

    @Query("SELECT pm FROM PersonaMaquina pm WHERE pm.maquina.id = :maquinaId")
    List<PersonaMaquina> findByMaquinaId(@Param("maquinaId") Long maquinaId);

    @Query("SELECT pm FROM PersonaMaquina pm WHERE pm.persona.id = :personaId")
    List<PersonaMaquina> findByPersonaId(@Param("personaId") Long personaId);
}
