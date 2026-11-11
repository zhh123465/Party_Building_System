package com.party.build.process.guard;

import com.party.build.process.dto.Blocker;
import org.springframework.stereotype.Component;

@Component
public class PermissionGuard implements StageGuard {
    @Override
    public String name() {
        return "操作权限";
    }

    @Override
    public GuardResult check(GuardContext context) {
        GuardResult result = new GuardResult();
        String role = context.getPrincipal().getRoleCode();
        if (!"R1".equals(role) && !"R2".equals(role)) {
            result.getBlockers().add(new Blocker("PERMISSION",
                    "当前角色没有推进权限", "由支部书记或管理员操作", "仅 R1/R2 可推进"));
        }
        return result;
    }
}