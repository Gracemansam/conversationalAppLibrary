package com.conversationalai.security;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SecurityValidator {

    private final List<Pattern> dangerousPatterns;
    private final List<String> blockedKeywords;

    public SecurityValidator() {
        this.dangerousPatterns = Arrays.asList(
                Pattern.compile("(?i)\\bDROP\\s+TABLE\\b"),
                Pattern.compile("(?i)\\bTRUNCATE\\b"),
                Pattern.compile("(?i)\\bDELETE\\s+FROM\\s+\\w+\\s*(?!WHERE)"),
                Pattern.compile("(?i)\\bUPDATE\\s+\\w+\\s+SET\\s+.*(?!WHERE)"),
                Pattern.compile("(?i)\\bALTER\\s+TABLE\\b"),
                Pattern.compile("(?i)\\bGRANT\\b"),
                Pattern.compile("(?i)\\bREVOKE\\b"),
                Pattern.compile("(?i);\\s*DROP\\b"),
                Pattern.compile("(?i);\\s*DELETE\\b"),
                Pattern.compile("(?i)\\bEXEC\\b"),
                Pattern.compile("(?i)\\bEXECUTE\\b")
        );

        this.blockedKeywords = Arrays.asList(
                "DROP", "TRUNCATE", "ALTER", "GRANT", "REVOKE", "EXEC", "EXECUTE"
        );
    }

    public boolean isQuerySafe(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : dangerousPatterns) {
            if (pattern.matcher(sql).find()) {
                log.warn("Blocked dangerous SQL pattern: {}", sql);
                return false;
            }
        }

        String upperSQL = sql.toUpperCase();
        for (String keyword : blockedKeywords) {
            if (upperSQL.contains(keyword)) {
                log.warn("Blocked SQL with dangerous keyword: {}", keyword);
                return false;
            }
        }

        return true;
    }

    public boolean isIntentAllowed(String intent) {
        List<String> allowedIntents = Arrays.asList("CREATE", "READ", "UPDATE", "DELETE");
        return allowedIntents.contains(intent.toUpperCase());
    }

    public boolean isOperationSafe(String operation, int affectedRows) {

        if (affectedRows > 100) {
            log.warn("Blocked operation affecting too many rows: {}", affectedRows);
            return false;
        }

        return true;
    }
}
