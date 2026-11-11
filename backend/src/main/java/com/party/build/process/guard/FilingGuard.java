package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilingGuard implements StageGuard {
    @Override
    public String name() {
        return "备案完整性";
    }

    @Override
    public GuardResult check(GuardContext context) {
        GuardResult result = new GuardResult();
        String target = context.getTargetStage();
        if (List.of("S1", "S2").contains(target)) {
            if (context.getRequest() == null) {
                result.getRequiredFields().add("filingDate");
            } else if (context.getRequest().getFilingDate() == null) {
                result.getBlockers().add(new Blocker("FILING",
                        "推进到 " + target + " 需要登记上级备案日期", "填写备案日期", "filingDate 为空"));
            }
        }
        if (List.of("S3", "S4").contains(target)) {
            if (context.getRequest() == null) {
                result.getRequiredFields().add("approvalDate");
            } else if (context.getRequest().getApprovalDate() == null) {
                result.getBlockers().add(new Blocker("FILING",
                        "推进到 " + target + " 需要登记上级批复日期", "填写批复日期", "approvalDate 为空"));
            }
        }
        return result;
    }
}