package com.conversationalai.database;

import com.conversationalai.dto.DatabaseSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SchemaCache {

    private final DatabaseSchemaAnalyzer schemaAnalyzer;
    private DatabaseSchema cachedSchema;
    private long lastCacheTime = 0;
    private static final long CACHE_TTL_MS = 300_000;

    public SchemaCache(DatabaseSchemaAnalyzer schemaAnalyzer) {
        this.schemaAnalyzer = schemaAnalyzer;
    }

    public DatabaseSchema getSchema() {
        long currentTime = System.currentTimeMillis();

        if (cachedSchema == null || (currentTime - lastCacheTime) > CACHE_TTL_MS) {
            log.debug("Refreshing database schema cache");
            cachedSchema = schemaAnalyzer.analyzeSchema();
            lastCacheTime = currentTime;
        }

        return cachedSchema;
    }

    public void invalidateCache() {
        cachedSchema = null;
        lastCacheTime = 0;
    }
}