package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String historyTable;

    @Value("${spring.flyway.schemas:public}")
    private String schema;

    @Bean(name = "flyway")
    public Flyway flyway(@Qualifier("sfDatasource") DataSource dataSource) {
        log.info("Iniciando migraciones Flyway en schema '{}'", schema);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .table(historyTable)
                .schemas(schema)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        // Si el checksum guardado en flyway_schema_history ya no coincide
        // con el archivo actual (p. ej. por saltos de línea distintos al
        // editar en Windows), repair() actualiza el historial para que
        // vuelva a coincidir con los archivos de db/migration tal como
        // están ahora, sin borrar ni reaplicar nada. No hace nada si todo
        // ya está en orden.
        flyway.repair();
        flyway.migrate();
        return flyway;
    }
}