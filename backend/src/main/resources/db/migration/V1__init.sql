-- 党员发展智能管理系统 初始库表
-- MySQL 8.0 / utf8mb4
-- 说明：外键在应用层校验，不建数据库外键；所有业务表含 created_at/updated_at/deleted。

CREATE TABLE IF NOT EXISTS sys_branch (
    branch_id BIGINT NOT NULL AUTO_INCREMENT,
    branch_code VARCHAR(32) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    college_name VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (branch_id),
    UNIQUE KEY uk_branch_code (branch_code),
    KEY idx_college (college_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    person_id BIGINT NULL,
    branch_id BIGINT NOT NULL,
    role_code VARCHAR(8) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    failed_count INT NOT NULL DEFAULT 0,
    lock_until DATETIME(3) NULL,
    token_version INT NOT NULL DEFAULT 0,
    must_change_password TINYINT NOT NULL DEFAULT 0,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    KEY idx_branch (branch_id),
    KEY idx_person (person_id),
    KEY idx_role (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_config (
    config_id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(255) NULL,
    updated_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS academic_year (
    year_id BIGINT NOT NULL AUTO_INCREMENT,
    year_code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (year_id),
    UNIQUE KEY uk_year_code (year_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS person (
    person_id BIGINT NOT NULL AUTO_INCREMENT,
    student_no VARCHAR(32) NOT NULL,
    name VARCHAR(50) NOT NULL,
    gender CHAR(1) NULL,
    college VARCHAR(100) NULL,
    major VARCHAR(100) NULL,
    class_name VARCHAR(100) NULL,
    branch_id BIGINT NOT NULL,
    phone_cipher VARBINARY(256) NULL,
    phone_mask VARCHAR(20) NULL,
    id_card_cipher VARBINARY(256) NULL,
    id_card_mask VARCHAR(32) NULL,
    application_date DATE NOT NULL,
    current_stage VARCHAR(4) NOT NULL DEFAULT 'S0',
    stage_start_date DATE NOT NULL,
    template_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    source_batch_id BIGINT NULL,
    remark VARCHAR(200) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (person_id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_branch_stage (branch_id, current_stage),
    KEY idx_name (name),
    KEY idx_application (application_date),
    KEY idx_stage_start (stage_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS family_member (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    relation VARCHAR(20) NOT NULL,
    work_unit VARCHAR(200) NULL,
    political_status VARCHAR(30) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (member_id),
    KEY idx_person (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stage_record (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    from_stage VARCHAR(4) NULL,
    to_stage VARCHAR(4) NOT NULL,
    change_type VARCHAR(16) NOT NULL,
    change_date DATETIME(3) NOT NULL,
    operator_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    evidence JSON NULL,
    template_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (record_id),
    KEY idx_person_date (person_id, change_date),
    KEY idx_to_stage (to_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material_type (
    type_id BIGINT NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(100) NOT NULL,
    stage_scope VARCHAR(32) NOT NULL,
    file_formats VARCHAR(100) NOT NULL DEFAULT 'pdf,jpg,png,docx',
    default_required TINYINT NOT NULL DEFAULT 1,
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (type_id),
    UNIQUE KEY uk_type_code (type_code),
    KEY idx_stage (stage_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS checklist_template (
    template_id BIGINT NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    academic_year_id BIGINT NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    effective_from DATE NULL,
    effective_to DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_template_version (template_code, version_no),
    KEY idx_year_status (academic_year_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS checklist_item (
    item_id BIGINT NOT NULL AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    stage VARCHAR(4) NOT NULL,
    material_type_id BIGINT NOT NULL,
    required TINYINT NOT NULL DEFAULT 1,
    min_count INT NOT NULL DEFAULT 1,
    max_count INT NULL,
    min_pages INT NULL,
    verify_points JSON NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_template_stage_type (template_id, stage, material_type_id),
    KEY idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material (
    material_id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    stage VARCHAR(4) NOT NULL,
    material_type_id BIGINT NOT NULL,
    checklist_item_id BIGINT NOT NULL,
    required TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(4) NOT NULL DEFAULT 'M1',
    current_version_id BIGINT NULL,
    due_date DATE NULL,
    reviewer_id BIGINT NULL,
    last_review_at DATETIME(3) NULL,
    archived TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (material_id),
    UNIQUE KEY uk_person_stage_type (person_id, stage, material_type_id),
    KEY idx_status (status),
    KEY idx_due (due_date),
    KEY idx_person (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material_version (
    version_id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    page_count INT NULL,
    text_content LONGTEXT NULL,
    text_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ocr_used TINYINT NOT NULL DEFAULT 0,
    uploaded_by BIGINT NULL,
    uploaded_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_material_version (material_id, version_no),
    KEY idx_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS verify_task (
    task_id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempt_no INT NOT NULL DEFAULT 1,
    model_provider VARCHAR(32) NULL,
    model_name VARCHAR(64) NULL,
    prompt_version VARCHAR(32) NULL,
    error_code VARCHAR(64) NULL,
    error_msg VARCHAR(500) NULL,
    queued_at DATETIME(3) NOT NULL,
    started_at DATETIME(3) NULL,
    finished_at DATETIME(3) NULL,
    duration_ms INT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id),
    KEY idx_status_queue (status, queued_at),
    KEY idx_version (version_id),
    KEY idx_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS verify_report (
    report_id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NULL,
    version_id BIGINT NULL,
    task_id BIGINT NULL,
    rule_conclusion VARCHAR(16) NULL,
    semantic_conclusion VARCHAR(16) NULL,
    overall_conclusion VARCHAR(16) NOT NULL,
    confidence DECIMAL(5,4) NULL,
    rule_version VARCHAR(32) NULL,
    model_name VARCHAR(64) NULL,
    prompt_version VARCHAR(32) NULL,
    evidence JSON NULL,
    duration_ms INT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (report_id),
    KEY idx_material (material_id),
    KEY idx_version (version_id),
    KEY idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS verify_finding (
    finding_id BIGINT NOT NULL AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    layer VARCHAR(16) NOT NULL,
    rule_code VARCHAR(64) NULL,
    point_name VARCHAR(200) NULL,
    conclusion VARCHAR(16) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'SOFT',
    evidence_text VARCHAR(2000) NULL,
    page_no INT NULL,
    char_start INT NULL,
    char_end INT NULL,
    suggestion VARCHAR(1000) NULL,
    confidence DECIMAL(5,4) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (finding_id),
    KEY idx_report (report_id),
    KEY idx_layer_conclusion (layer, conclusion),
    KEY idx_rule (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS review_log (
    review_id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    report_id BIGINT NULL,
    reviewer_id BIGINT NOT NULL,
    conclusion VARCHAR(16) NOT NULL,
    reason_code VARCHAR(32) NULL,
    opinion VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (review_id),
    KEY idx_material (material_id),
    KEY idx_reviewer (reviewer_id),
    KEY idx_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS verify_rule (
    rule_id BIGINT NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    stage VARCHAR(4) NOT NULL,
    material_type_id BIGINT NULL,
    rule_type VARCHAR(32) NOT NULL,
    params JSON NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'HARD',
    enabled TINYINT NOT NULL DEFAULT 1,
    version_no INT NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_rule_code (rule_code),
    KEY idx_stage_type (stage, material_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS prompt_template (
    prompt_id BIGINT NOT NULL AUTO_INCREMENT,
    prompt_code VARCHAR(64) NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    content TEXT NOT NULL,
    variables JSON NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (prompt_id),
    UNIQUE KEY uk_prompt_version (prompt_code, version_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS time_rule (
    rule_id BIGINT NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(32) NOT NULL,
    academic_year_id BIGINT NOT NULL,
    node_code VARCHAR(32) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    start_event VARCHAR(64) NOT NULL,
    depends_on_node VARCHAR(64) NULL,
    offset_value INT NOT NULL,
    offset_unit VARCHAR(16) NOT NULL DEFAULT 'DAY',
    warn_offsets JSON NULL,
    responsible_role VARCHAR(8) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_year_node (academic_year_id, node_code),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS warning (
    warning_id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    stage VARCHAR(4) NOT NULL,
    time_rule_id BIGINT NOT NULL,
    node_code VARCHAR(32) NOT NULL,
    cycle_key VARCHAR(32) NOT NULL,
    due_date DATE NOT NULL,
    level VARCHAR(16) NOT NULL DEFAULT 'INFO',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    handled_by BIGINT NULL,
    handled_at DATETIME(3) NULL,
    handle_note VARCHAR(500) NULL,
    last_notified_level VARCHAR(16) NULL,
    message_sent TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (warning_id),
    UNIQUE KEY uk_person_node_cycle (person_id, node_code, cycle_key),
    KEY idx_level_status (level, status),
    KEY idx_due (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS message (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    biz_type VARCHAR(32) NULL,
    biz_id BIGINT NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (message_id),
    KEY idx_receiver_read (receiver_id, read_at),
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    username VARCHAR(64) NULL,
    action VARCHAR(64) NOT NULL,
    object_type VARCHAR(32) NULL,
    object_id BIGINT NULL,
    result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    reason VARCHAR(500) NULL,
    detail JSON NULL,
    ip VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (log_id),
    KEY idx_user_time (user_id, created_at),
    KEY idx_action_time (action, created_at),
    KEY idx_object (object_type, object_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS export_log (
    export_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    export_type VARCHAR(32) NOT NULL,
    filter_json JSON NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    watermark_text VARCHAR(200) NULL,
    row_count INT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (export_id),
    KEY idx_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS import_batch (
    batch_id BIGINT NOT NULL AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_sha256 CHAR(64) NOT NULL,
    template_version VARCHAR(32) NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PROCESSING',
    created_by BIGINT NOT NULL,
    finished_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (batch_id),
    KEY idx_creator_time (created_by, created_at),
    KEY idx_sha256 (file_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS import_error (
    error_id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    column_name VARCHAR(64) NULL,
    error_msg VARCHAR(500) NOT NULL,
    raw_data JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (error_id),
    KEY idx_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS compliance_score (
    score_id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_key VARCHAR(20) NOT NULL,
    material_score DECIMAL(5,2) NOT NULL,
    time_score DECIMAL(5,2) NOT NULL,
    content_score DECIMAL(5,2) NOT NULL,
    total_score DECIMAL(5,2) NOT NULL,
    risk_level VARCHAR(8) NOT NULL,
    detail JSON NULL,
    calculated_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (score_id),
    UNIQUE KEY uk_person_period (person_id, period_type, period_key),
    KEY idx_total (total_score),
    KEY idx_risk (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS archive_record (
    archive_id BIGINT NOT NULL AUTO_INCREMENT,
    person_id BIGINT NOT NULL,
    stage VARCHAR(4) NOT NULL,
    archive_no VARCHAR(64) NOT NULL,
    manifest JSON NULL,
    file_path VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'BUILDING',
    archived_by BIGINT NULL,
    archived_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (archive_id),
    UNIQUE KEY uk_archive_no (archive_no),
    UNIQUE KEY uk_person_stage (person_id, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
