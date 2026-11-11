package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import org.springframework.stereotype.Component;

@Component
public class ProcessGuard implements StageGuard {
    @Override
    public String name() {
        return "流程锁";
    }

    @Override
    public GuardResult check(GuardContext context) {
        GuardResult result = new GuardResult();
        var summary = context.getMaterialSummary();
        if (summary != null && summary.getReturnedCount() > 0) {
            result.getBlockers().add(new Blocker("PROCESS", "存在未处理完毕的退回材料", "先完成整改并重新核验", "退回材料未通过"));
        }
        if (summary != null && summary.getPreviousStageIssues() != null) {
            for (String issue : summary.getPreviousStageIssues()) {
                result.getBlockers().add(new Blocker("PROCESS", issue, "先完成上一阶段归档", issue));
            }
        }
        return result;
    }
}