package com.party.build.common.api;

public enum ErrorCode {
    OK(0, "ok"),
    UNAUTHORIZED(10001, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限访问"),
    ACCOUNT_DISABLED(10004, "账号已停用"),
    ACCOUNT_LOCKED(10005, "账号已锁定，请稍后再试"),
    BAD_CREDENTIALS(10006, "账号或密码错误"),
    VALIDATION_ERROR(20001, "参数错误"),
    FILE_TYPE_NOT_SUPPORTED(20002, "文件类型不支持"),
    FILE_TOO_LARGE(20003, "文件过大"),
    MATERIAL_INCOMPLETE(30001, "材料未齐备"),
    STAGE_LOCKED(30002, "当前阶段被锁定"),
    ALREADY_EXISTS(30003, "记录已存在"),
    STATE_NOT_ALLOWED(30004, "当前状态不允许该操作"),
    STATE_CHANGED(30005, "状态已变化，请刷新后重试"),
    LLM_TIMEOUT(40001, "模型调用超时"),
    OCR_FAILED(40002, "OCR 识别失败"),
    LLM_BAD_RESPONSE(40003, "模型返回无法解析"),
    DB_ERROR(50001, "数据库错误"),
    FILE_STORAGE_ERROR(50002, "文件存储错误"),
    UNKNOWN(50003, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}