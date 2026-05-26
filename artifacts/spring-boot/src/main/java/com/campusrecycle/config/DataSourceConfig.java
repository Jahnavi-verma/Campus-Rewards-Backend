package com.campusrecycle.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Primary
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        try {
            String raw = databaseUrl;
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("DATABASE_URL environment variable is not set.");
            }

            // Normalise to a standard URI by replacing postgres:// → postgresql://
            raw = raw.replaceFirst("^postgres://", "postgresql://");

            // Strip query string so URI parses cleanly, then carry it forward
            String query = "";
            int qIdx = raw.indexOf('?');
            if (qIdx >= 0) {
                query = raw.substring(qIdx + 1);
                raw = raw.substring(0, qIdx);
            }

            URI uri = new URI(raw);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath(); // e.g. "/heliumdb"

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts.length > 1 ? parts[1] : "";
            }

            // Build a clean JDBC URL (no embedded credentials, no sslmode param that confuses HikariCP)
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d%s", host, port, path);

            config.setJdbcUrl(jdbcUrl);
            if (username != null) config.setUsername(username);
            if (password != null) config.setPassword(password);

            // Pass through safe JDBC properties from the original query string
            if (query.contains("sslmode=disable")) {
                config.addDataSourceProperty("sslmode", "disable");
            } else if (query.contains("sslmode=require")) {
                config.addDataSourceProperty("sslmode", "require");
            }

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            return new HikariDataSource(config);

        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot parse DATABASE_URL: " + databaseUrl, e);
        }
    }
}
