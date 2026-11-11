package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import org.springframework.stereotype.Component;

@Component
public class MaterialGuard implements StageGuard {
    @Override
    public String name() {
        return "材料锁";
    }

    @Override
    public GuardResult check(GuardContext context) {
        GuardResult result = new GuardResult();
        var summary = context.getMaterialSummary();
        if (summary == null) {
            result.getBlockers().add(new Blocker("MATERIAL", "材料信息缺失", "去材料页查看", "无法读取材料汇总"));
            return result;
        }
        if (summary.getRequiredCount() == 0) {
            result.getBlockers().add(new Blocker("MATERIAL", "当前阶段未配置材料清单", "联系管理员", "清单模板为空"));
            return result;
        }
        if (summary.getIssues() != null) {
            for (String issue : summary.getIssues()) {
                result.getBlockers().add(new Blocker("MATERIAL", issue, "去补齐材料", issue));
            }
        }
        return result;
    }
}