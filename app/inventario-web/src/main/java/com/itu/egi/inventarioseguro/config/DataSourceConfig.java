package com.itu.egi.inventarioseguro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.SessionSynchronization;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.itu.egi.inventarioseguro.repository.sql")
@EnableMongoRepositories(basePackages = "com.itu.egi.inventarioseguro.repository.mongo")
public class DataSourceConfig {

    // Evita que Spring Data MongoDB intente enlazarse a transacciones JPA activas.
    // Sin esto, las escrituras a MongoDB dentro de un @Transactional (JPA) se pierden silenciosamente.
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate template = new MongoTemplate(factory);
        template.setSessionSynchronization(SessionSynchronization.NEVER);
        return template;
    }
}
