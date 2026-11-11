package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import org.springframework.stereotype.Component;

@Component
public class ConfirmGuard implements StageGuard {
    @Override
    public String name() {
        return "二次确认";
    }

    @Override
    public GuardResult check(GuardContext context) {
        GuardResult result = new GuardResult();
        if (context.getRequest() == null) {
            result.getRequiredFields().add("meetingDate");
            result.getRequiredFields().add("reason");
            result.getRequiredFields().add("confirm");
            return result;
        }
        if (context.getRequest().getMeetingDate() == null) {
            result.getBlockers().add(new Blocker("CONFIRM", "请填写推进依据（支部会议日期）", "填写会议日期", "meetingDate 为空"));
        }
        if (context.getRequest().getReason() == null || context.getRequest().getReason().trim().length() < 5) {
            result.getBlockers().add(new Blocker("CONFIRM", "请填写推进依据说明（至少 5 字）", "填写推进依据", "reason 为空或过短"));
        }
        if (!context.getRequest().isConfirm()) {
            result.getBlockers().add(new Blocker("CONFIRM", "请勾选二次确认", "勾选确认后提交", "confirm=false"));
        }
        return result;
    }
}