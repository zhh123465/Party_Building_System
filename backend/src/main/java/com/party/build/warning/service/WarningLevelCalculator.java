package com.party.build.warning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WarningLevelCalculator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WarningLevelCalculator() {
    }

    public static String level(LocalDate dueDate, LocalDate today, String warnOffsetsJson) {
        long days = ChronoUnit.DAYS.between(today, dueDate);
        if (days < 0) {
            return "OVERDUE";
        }
        List<Integer> offsets = parseOffsets(warnOffsetsJson);
        offsets.sort(Comparator.naturalOrder());
        // 默认把最小的两档视为紧迫/临期，较大的两档视为提示
        if (offsets.isEmpty()) {
            offsets = List.of(3, 7, 15, 30);
        }
        for (int offset : offsets) {
            if (days <= offset) {
                if (offset <= 3) return "URGENT";
                if (offset <= 15) return "NEAR";
                return "INFO";
            }
        }
        return "NONE";
    }

    public static int severity(String level) {
        return switch (level) {
            case "OVERDUE" -> 4;
            case "URGENT" -> 3;
            case "NEAR" -> 2;
            case "INFO" -> 1;
            default -> 0;
        };
    }

    private static List<Integer> parseOffsets(String json) {
        List<Integer> list = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return list;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isNumber()) {
                        list.add(item.asInt());
                    } else if (item.has("days")) {
                        list.add(item.path("days").asInt());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }
}