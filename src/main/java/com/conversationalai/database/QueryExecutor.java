package com.conversationalai.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QueryExecutor {
    private static final Logger log = LoggerFactory.getLogger(QueryExecutor.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public QueryExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeQuery(String sql, Object[] parameters) {
        try {
            log.debug("Executing query: {} with parameters: {}", sql, parameters);
            return jdbcTemplate.queryForList(sql, parameters);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Query returned no results: {}", sql);
            return List.of(); // Return empty list instead of null
        } catch (Exception e) {
            log.error("Error executing query: {} with parameters: {}", sql, parameters, e);
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    public int executeUpdate(String sql, Object[] parameters) {
        try {
            log.debug("Executing update: {} with parameters: {}", sql, parameters);
            return jdbcTemplate.update(sql, parameters);
        } catch (Exception e) {
            log.error("Error executing update: {} with parameters: {}", sql, parameters, e);
            throw new RuntimeException("Failed to execute update", e);
        }
    }
}