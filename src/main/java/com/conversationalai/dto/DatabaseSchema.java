package com.conversationalai.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Map;
import java.util.List;

@Data
@Builder
public class DatabaseSchema {
    private Map<String, TableInfo> tables;

    @Data
    @Builder
    public static class TableInfo {
        private String tableName;
        private Map<String, ColumnInfo> columns;
        private List<String> primaryKeys;
        private Map<String, ForeignKeyInfo> foreignKeys;
    }

    @Data
    @Builder
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private boolean nullable;
        private boolean autoIncrement;
        private String defaultValue;
        private Integer maxLength;
    }

    @Data
    @Builder
    public static class ForeignKeyInfo {
        private String referencedTable;
        private String referencedColumn;
    }
}
