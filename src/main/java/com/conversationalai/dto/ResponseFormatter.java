package com.conversationalai.dto;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ResponseFormatter {

    public String formatDataResponse(String intent, List<Map<String, Object>> data, String baseMessage) {
        if (data == null || data.isEmpty()) {
            return "🔍 **No Results Found**\n\nI couldn't find any records matching your search criteria. You might want to:\n• Check your spelling\n• Try a broader search term\n• Use partial matches (e.g., \"John\" instead of \"Johnathan\")";
        }

        StringBuilder response = new StringBuilder();
        response.append(baseMessage).append("\n\n");

        if (data.size() == 1) {

            Map<String, Object> record = data.get(0);
            response.append("📄 **Record Details:**\n");
            record.forEach((key, value) -> {
                if (!"password".equalsIgnoreCase(key)) { // Hide sensitive data
                    String displayKey = formatFieldName(key);
                    response.append("• **").append(displayKey).append(":** ").append(value).append("\n");
                }
            });
        } else {

            response.append("📊 **Found ").append(data.size()).append(" records:**\n\n");

            for (int i = 0; i < Math.min(data.size(), 10); i++) {
                Map<String, Object> record = data.get(i);
                response.append("**Record ").append(i + 1).append(":**\n");

                if (record.containsKey("name")) {
                    response.append("• Name: ").append(record.get("name")).append("\n");
                }
                if (record.containsKey("email")) {
                    response.append("• Email: ").append(record.get("email")).append("\n");
                }
                if (record.containsKey("id")) {
                    response.append("• ID: ").append(record.get("id")).append("\n");
                }
                response.append("\n");
            }

            if (data.size() > 10) {
                response.append("... and ").append(data.size() - 10).append(" more records.\n");
            }
        }

        response.append("\n💡 **Need something else?** Just ask me to search, update, create, or delete records!");

        return response.toString();
    }

    public String formatCountResponse(List<Map<String, Object>> data, String baseMessage) {
        if (data != null && !data.isEmpty()) {
            Object count = data.get(0).values().iterator().next();
            return "🔢 **Count Results**\n\nI found **" + count + "** records that match your criteria.\n\n" +
                    "Would you like me to show you the actual records or perform another search?";
        }
        return baseMessage;
    }

    public String formatUpdateResponse(Object result, String baseMessage) {
        if (result instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) result;
            int affectedRows = (Integer) resultMap.getOrDefault("affectedRows", 0);

            if (affectedRows > 0) {
                return "✅ **Update Successful**\n\n" +
                        "I've successfully updated **" + affectedRows + "** record" +
                        (affectedRows > 1 ? "s" : "") + " in the database.\n\n" +
                        "The changes have been saved and are now active.";
            } else {
                return "⚠️ **No Records Updated**\n\n" +
                        "No records were found matching your criteria, so no updates were made.\n\n" +
                        "Please check your search criteria and try again.";
            }
        }
        return baseMessage;
    }

    private String formatFieldName(String fieldName) {
        // Convert snake_case or camelCase to readable format
        return Arrays.stream(fieldName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}