package com.party.build.warning.service;

import com.party.build.warning.domain.TimeRule;
import com.party.build.warning.dto.ScheduleNodeVO;
import com.party.build.warning.dto.ScheduleResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class ScheduleCalculator {
    private ScheduleCalculator() {
    }

    public static ScheduleResponse calculate(List<TimeRule> rules, String targetNode, LocalDate targetDate, LocalDate today) {
        Map<String, TimeRule> byCode = new LinkedHashMap<>();
        for (TimeRule rule : rules) {
            byCode.put(rule.getNodeCode(), rule);
        }
        if (!byCode.containsKey(targetNode)) {
            throw new IllegalArgumentException("目标节点不存在: " + targetNode);
        }

        // successor -> predecessors
        Map<String, List<String>> predecessors = new HashMap<>();
        for (TimeRule rule : rules) {
            if (rule.getDependsOnNode() != null && !rule.getDependsOnNode().isBlank()) {
                for (String dep : rule.getDependsOnNode().split(",")) {
                    predecessors.computeIfAbsent(rule.getNodeCode(), k -> new ArrayList<>()).add(dep.trim());
                }
            }
        }

        Map<String, LocalDate> due = new HashMap<>();
        due.put(targetNode, targetDate);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TimeRule rule : rules) {
                LocalDate nodeDue = due.get(rule.getNodeCode());
                if (nodeDue == null) {
                    continue;
                }
                LocalDate nodeStart = subtract(nodeDue, rule);
                for (String pred : predecessors.getOrDefault(rule.getNodeCode(), List.of())) {
                    LocalDate candidate = nodeStart;
                    if (!due.containsKey(pred) || candidate.isBefore(due.get(pred))) {
                        due.put(pred, candidate);
                        changed = true;
                    }
                }
            }
        }

        List<ScheduleNodeVO> nodes = new ArrayList<>();
        boolean feasible = true;
        List<String> critical = new ArrayList<>();
        for (TimeRule rule : rules) {
            LocalDate nodeDue = due.get(rule.getNodeCode());
            if (nodeDue == null) {
                continue;
            }
            LocalDate nodeStart = subtract(nodeDue, rule);
            ScheduleNodeVO vo = new ScheduleNodeVO();
            vo.setRuleCode(rule.getNodeCode());
            vo.setNodeName(rule.getNodeName());
            vo.setStage(rule.getStage());
            vo.setLatestDue(nodeDue);
            vo.setLatestStart(nodeStart);
            vo.setDependsOn(rule.getDependsOnNode());
            if (nodeStart.isBefore(today)) {
                vo.setStatus("INFEASIBLE");
                feasible = false;
                critical.add(rule.getNodeCode());
            } else if (ChronoUnit.DAYS.between(today, nodeStart) <= 7) {
                vo.setStatus("URGENT");
                critical.add(rule.getNodeCode());
            } else {
                vo.setStatus("NORMAL");
            }
            nodes.add(vo);
        }
        nodes.sort(Comparator.comparing(ScheduleNodeVO::getLatestDue));
        ScheduleResponse response = new ScheduleResponse();
        response.setTargetNode(targetNode);
        response.setTargetDate(targetDate);
        response.setFeasible(feasible);
        response.setNodes(nodes);
        response.setCriticalPath(critical);
        return response;
    }

    private static LocalDate subtract(LocalDate due, TimeRule rule) {
        int value = rule.getOffsetValue() == null ? 0 : rule.getOffsetValue();
        if ("MONTH".equalsIgnoreCase(rule.getOffsetUnit())) {
            return due.minusMonths(value);
        }
        return due.minusDays(value);
    }
}