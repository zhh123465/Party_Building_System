package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TimeGuard implements StageGuard {
    @Override
    public String name() {
        return "时限锁";
    }

    @Override
    public GuardResult check(GuardContext context) {
        GuardResult result = new GuardResult();
        if (context.getWarnings() == null) {
            return result;
        }
        for (var warning : context.getWarnings()) {
            if ("OVERDUE".equals(warning.getLevel()) && "OPEN".equals(warning.getStatus())) {
                result.getBlockers().add(new Blocker("TIME",
                        "节点已逾期：" + warning.getNodeCode() + "，应完成日 " + warning.getDueDate(),
                        "由支部书记填写逾期说明并处置预警",
                        warning.getCycleKey()));
            } else if (List.of("URGENT", "NEAR").contains(warning.getLevel())) {
                result.getWarnings().add("节点 " + warning.getNodeCode() + " 临期，应完成日 " + warning.getDueDate());
            }
        }
        return result;
    }
}