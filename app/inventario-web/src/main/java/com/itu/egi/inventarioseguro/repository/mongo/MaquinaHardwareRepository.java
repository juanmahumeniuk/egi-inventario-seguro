package com.itu.egi.inventarioseguro.repository.mongo;

import com.itu.egi.inventarioseguro.model.MaquinaHardware;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaquinaHardwareRepository extends MongoRepository<MaquinaHardware, Long> {
}
