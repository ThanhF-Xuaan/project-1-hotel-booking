package vn.edu.utc.hotel_booking.modules.ai.rag.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.PostgresPgVectorStore;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import java.util.Properties;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "hotel.ai.rag", name = "store-type", havingValue = "pgvector")
public class RagVectorStoreConfiguration {
    @Bean
    @Lazy
    public RagVectorStorePort persistentRagStore(Environment environment, RagEmbeddingPort embedding) {
        if (environment.getProperty("hotel.ai.rag.demo-corpus.enabled", Boolean.class, false)) {
            throw new IllegalStateException("Persistent RAG cannot use the in-memory demo corpus initializer");
        }
        String url = environment.getRequiredProperty("hotel.ai.rag.pgvector.jdbc-url");
        if (!url.matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]{1,5}/t12_pgvector_[A-Za-z0-9_]+")) {
            throw new IllegalStateException("The local RAG profile requires a loopback PostgreSQL target without URL credentials");
        }
        String username = environment.getRequiredProperty("hotel.ai.rag.pgvector.username");
        String password = environment.getRequiredProperty("hotel.ai.rag.pgvector.password");
        if (username.isBlank() || password.isBlank()) throw new IllegalStateException("RAG credentials are required");
        // Deliberately not exposed as a DataSource bean: never replace the primary business datasource.
        DriverManagerDataSource dedicated = new DriverManagerDataSource(url, username, password);
        Properties connection = new Properties();
        connection.setProperty("connectTimeout", "3");
        connection.setProperty("socketTimeout", "5");
        connection.setProperty("ApplicationName", "hotel-rag-store");
        dedicated.setConnectionProperties(connection);
        return new PostgresPgVectorStore(dedicated, embedding);
    }
}
