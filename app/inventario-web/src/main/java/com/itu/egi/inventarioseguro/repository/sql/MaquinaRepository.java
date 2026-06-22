package com.itu.egi.inventarioseguro.repository.sql;

import com.itu.egi.inventarioseguro.model.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {
}
