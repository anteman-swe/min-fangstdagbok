package org.example.fiskeexperten;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Startar en tillfällig PostGIS i Docker åt testerna och kopplar appens DataSource till den
 * (@ServiceConnection). Därmed rör testerna aldrig din riktiga databas. Kräver att Docker körs.
 * Flyway-migreringarna körs mot containern precis som mot den riktiga databasen.
 * Samma PostGIS-image som i compose.yaml. Byt båda om din dator inte kan köra den
 * (t.ex. en ARM-dator).
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostGisTestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgis() {
        return new PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"));
    }
}
