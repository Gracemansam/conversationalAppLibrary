package com.conversationalai.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.Generated;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

import com.conversationalai.dto.DatabaseSchema;
import com.conversationalai.dto.DatabaseSchema.TableInfo;
import com.conversationalai.dto.DatabaseSchema.ColumnInfo;
import com.conversationalai.dto.DatabaseSchema.ForeignKeyInfo;

@Component
public class DatabaseSchemaAnalyzer {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaAnalyzer.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseSchemaAnalyzer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseSchema analyzeSchema() {
        try {
            Map<String, DatabaseSchema.TableInfo> tables = new HashMap<>();
            DatabaseMetaData metaData = this.jdbcTemplate.getDataSource().getConnection().getMetaData();
            ResultSet tablesResult = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while(tablesResult.next()) {
                String tableName = tablesResult.getString("TABLE_NAME");
                if (!this.isSystemTable(tableName)) {
                    DatabaseSchema.TableInfo tableInfo = this.analyzeTable(metaData, tableName);
                    tables.put(tableName, tableInfo);
                }
            }

            return DatabaseSchema.builder().tables(tables).build();
        } catch (Exception e) {
            log.error("Error analyzing database schema", e);
            throw new RuntimeException("Failed to analyze database schema", e);
        }
    }

    private DatabaseSchema.TableInfo analyzeTable(DatabaseMetaData metaData, String tableName) throws Exception {
        Map<String, DatabaseSchema.ColumnInfo> columns = new HashMap<>();
        List<String> primaryKeys = new ArrayList<>();
        Map<String, DatabaseSchema.ForeignKeyInfo> foreignKeys = new HashMap<>();

        ResultSet columnsResult = metaData.getColumns(null, null, tableName, "%");

        while(columnsResult.next()) {
            String columnName = columnsResult.getString("COLUMN_NAME");
            String dataType = columnsResult.getString("TYPE_NAME");
            boolean nullable = columnsResult.getInt("NULLABLE") == 1;
            String defaultValue = columnsResult.getString("COLUMN_DEF");
            int columnSize = columnsResult.getInt("COLUMN_SIZE");
            boolean autoIncrement = "YES".equals(columnsResult.getString("IS_AUTOINCREMENT"));

            DatabaseSchema.ColumnInfo columnInfo = ColumnInfo.builder()
                    .columnName(columnName)
                    .dataType(dataType)
                    .nullable(nullable)
                    .autoIncrement(autoIncrement)
                    .defaultValue(defaultValue)
                    .maxLength(columnSize)
                    .build();
            columns.put(columnName, columnInfo);
        }

        ResultSet primaryKeysResult = metaData.getPrimaryKeys(null, null, tableName);
        while(primaryKeysResult.next()) {
            primaryKeys.add(primaryKeysResult.getString("COLUMN_NAME"));
        }

        ResultSet foreignKeysResult = metaData.getImportedKeys(null, null, tableName);
        while(foreignKeysResult.next()) {
            String fkColumnName = foreignKeysResult.getString("FKCOLUMN_NAME");
            String pkTableName = foreignKeysResult.getString("PKTABLE_NAME");
            String pkColumnName = foreignKeysResult.getString("PKCOLUMN_NAME");

            DatabaseSchema.ForeignKeyInfo fkInfo = ForeignKeyInfo.builder()
                    .referencedTable(pkTableName)
                    .referencedColumn(pkColumnName)
                    .build();
            foreignKeys.put(fkColumnName, fkInfo);
        }

        return TableInfo.builder()
                .tableName(tableName)
                .columns(columns)
                .primaryKeys(primaryKeys)
                .foreignKeys(foreignKeys)
                .build();
    }

    private boolean isSystemTable(String tableName) {
        String lowerTableName = tableName.toLowerCase();
        return lowerTableName.startsWith("sys") ||
                lowerTableName.startsWith("information_schema") ||
                lowerTableName.startsWith("mysql") ||
                lowerTableName.startsWith("performance_schema") ||
                lowerTableName.equals("dual");
    }
}