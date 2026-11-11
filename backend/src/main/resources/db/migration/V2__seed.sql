-- 演示种子数据（全部为虚构数据，禁止用于真实业务）
-- 默认密码：Party@2026（BCrypt）

INSERT INTO sys_branch (branch_id, branch_code, branch_name, college_name, status, sort_no)
VALUES
    (1, 'RJXY-01', '软件工程系学生党支部', '计算机与信息工程学院', 1, 1),
    (2, 'RJXY-02', '计算机科学与技术系学生党支部', '计算机与信息工程学院', 1, 2)
ON DUPLICATE KEY UPDATE branch_name = VALUES(branch_name);

INSERT INTO academic_year (year_id, year_code, start_date, end_date, status)
VALUES (1, '2026-2027', '2026-09-01', '2027-08-31', 1)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO person (person_id, student_no, name, gender, college, major, class_name, branch_id,
                    phone_mask, id_card_mask, application_date, current_stage, stage_start_date,
                    template_id, status, remark)
VALUES
    (1, '20260001', '张明', 'M', '计算机与信息工程学院', '软件工程', '软工2026-1', 1,
     '138****0001', '450***********0011', '2026-09-10', 'S0', '2026-09-10', 1, 'ACTIVE', '演示数据'),
    (2, '20260002', '李雨', 'F', '计算机与信息工程学院', '软件工程', '软工2026-1', 1,
     '138****0002', '450***********0022', '2026-03-05', 'S1', '2026-04-01', 1, 'ACTIVE', '演示数据'),
    (3, '20260003', '王浩', 'M', '计算机与信息工程学院', '计算机科学与技术', '计科2025-2', 1,
     '138****0003', '450***********0033', '2025-09-12', 'S2', '2026-09-15', 1, 'ACTIVE', '演示数据'),
    (4, '20260004', '赵雪', 'F', '计算机与信息工程学院', '软件工程', '软工2025-1', 1,
     '138****0004', '450***********0044', '2024-09-20', 'S3', '2026-06-01', 1, 'ACTIVE', '演示数据'),
    (5, '20260005', '陈晨', 'M', '计算机与信息工程学院', '计算机科学与技术', '计科2024-1', 1,
     '138****0005', '450***********0055', '2023-09-15', 'S4', '2026-05-20', 1, 'ACTIVE', '演示数据'),
    (6, '20260006', '刘洋', 'F', '计算机与信息工程学院', '软件工程', '软工2026-2', 2,
     '138****0006', '450***********0066', '2026-09-18', 'S0', '2026-09-18', 1, 'ACTIVE', '演示数据')
ON DUPLICATE KEY UPDATE name = VALUES(name), current_stage = VALUES(current_stage);

INSERT INTO sys_user (user_id, username, password_hash, real_name, person_id, branch_id, role_code,
                      status, must_change_password)
VALUES
    (1, 'admin', '$2b$10$tX64gGP3CrlRvI0URT4seuNxGwLIZ6.npOhhxL4f/IqlC2Om6I2ra', '系统管理员', NULL, 1, 'R1', 1, 0),
    (2, 'secretary', '$2b$10$tX64gGP3CrlRvI0URT4seuNxGwLIZ6.npOhhxL4f/IqlC2Om6I2ra', '支部书记', NULL, 1, 'R2', 1, 0),
    (3, 'assistant', '$2b$10$tX64gGP3CrlRvI0URT4seuNxGwLIZ6.npOhhxL4f/IqlC2Om6I2ra', '党务助理', NULL, 1, 'R3', 1, 0),
    (4, 'student', '$2b$10$tX64gGP3CrlRvI0URT4seuNxGwLIZ6.npOhhxL4f/IqlC2Om6I2ra', '张明', 1, 1, 'R4', 1, 0),
    (5, 'student2', '$2b$10$tX64gGP3CrlRvI0URT4seuNxGwLIZ6.npOhhxL4f/IqlC2Om6I2ra', '李雨', 2, 1, 'R4', 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), role_code = VALUES(role_code);

INSERT INTO material_type (type_id, type_code, type_name, stage_scope, file_formats, default_required, sort_no)
VALUES
    (1, 'APPLICATION', '入党申请书', 'S0', 'pdf,jpg,png,docx', 1, 10),
    (2, 'TALK_RECORD', '同入党申请人的谈话记录', 'S0', 'pdf,jpg,png,docx', 1, 20),
    (3, 'FULL_RECORD', '发展党员工作全程纪实表', 'S0', 'pdf,jpg,png,docx', 1, 30),
    (4, 'ACTIVIST_FORM', '推荐入党积极分子人选登记表', 'S1', 'pdf,jpg,png,docx', 1, 10),
    (5, 'ACTIVIST_MEETING', '支部委员会确定入党积极分子会议记录', 'S1', 'pdf,jpg,png,docx', 1, 20),
    (6, 'THOUGHT_REPORT', '思想汇报', 'S1,S2,S3', 'pdf,jpg,png,docx', 1, 30),
    (7, 'TRAINING_FORM', '培养考察登记表', 'S1', 'pdf,jpg,png,docx', 1, 40),
    (8, 'TRAINING_CERT', '积极分子培训班结业证书', 'S1', 'pdf,jpg,png', 1, 50),
    (9, 'MASS_OPINION', '党员群众意见会议记录', 'S2', 'pdf,jpg,png,docx', 1, 10),
    (10, 'DEV_MEETING', '支部委员会讨论发展对象人选会议记录', 'S2', 'pdf,jpg,png,docx', 1, 20),
    (11, 'PUBLICITY_FORM', '公示情况登记表', 'S2', 'pdf,jpg,png,docx', 1, 30),
    (12, 'AUTOBIOGRAPHY', '自传', 'S2', 'pdf,jpg,png,docx', 1, 40),
    (13, 'POLITICAL_REVIEW', '政治审查材料', 'S2', 'pdf,jpg,png', 1, 50),
    (14, 'CRIMINAL_RECORD', '无犯罪记录证明', 'S2', 'pdf,jpg,png', 1, 60),
    (15, 'RELIGION_STATEMENT', '宗教信仰说明', 'S2', 'pdf,jpg,png,docx', 1, 70),
    (16, 'POLITICAL_REPORT', '政治审查报告', 'S2', 'pdf,jpg,png,docx', 1, 80),
    (17, 'SHORT_TRAINING', '短期集中培训证明', 'S2', 'pdf,jpg,png', 1, 90),
    (18, 'TRANSCRIPT', '成绩单', 'S2', 'pdf,jpg,png', 1, 100),
    (19, 'APPLICATION_BOOK', '入党志愿书', 'S3', 'pdf,jpg,png', 1, 10),
    (20, 'CONVERSION_APPLICATION', '转正申请书', 'S4', 'pdf,jpg,png,docx', 1, 10)
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

INSERT INTO checklist_template (template_id, template_code, template_name, academic_year_id, version_no,
                                status, effective_from, effective_to, created_by)
VALUES (1, 'DEFAULT', '党员发展材料清单（2026-2027）', 1, 1, 'PUBLISHED', '2026-09-01', '2027-08-31', 1)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO checklist_item (template_id, stage, material_type_id, required, min_count, min_pages, verify_points, sort_no)
VALUES
    (1, 'S0', 1, 1, 1, 1, JSON_ARRAY('本人签名', '申请日期', '入党动机'), 10),
    (1, 'S0', 2, 1, 1, 1, JSON_ARRAY('谈话时间', '谈话人', '谈话内容'), 20),
    (1, 'S0', 3, 1, 1, 1, JSON_ARRAY('阶段记录完整'), 30),
    (1, 'S1', 4, 1, 1, 1, JSON_ARRAY('推荐人意见', '支部意见'), 10),
    (1, 'S1', 5, 1, 1, 1, JSON_ARRAY('会议时间', '参会人员', '决议'), 20),
    (1, 'S1', 6, 1, 4, 1, JSON_ARRAY('每季度不少于1次', '本人署名', '思想变化'), 30),
    (1, 'S1', 7, 1, 1, 1, JSON_ARRAY('培养联系人意见', '考察记录'), 40),
    (1, 'S1', 8, 1, 1, 1, JSON_ARRAY('结业时间', '主办单位'), 50),
    (1, 'S2', 9, 1, 1, 1, JSON_ARRAY('会议时间', '群众意见'), 10),
    (1, 'S2', 10, 1, 1, 1, JSON_ARRAY('会议时间', '决议'), 20),
    (1, 'S2', 11, 1, 1, 1, JSON_ARRAY('公示时间', '公示结果'), 30),
    (1, 'S2', 12, 1, 1, 1, JSON_ARRAY('本人签名', '个人经历'), 40),
    (1, 'S2', 13, 1, 1, 1, JSON_ARRAY('家庭成员', '政审结论', '签章'), 50),
    (1, 'S2', 14, 1, 1, 1, JSON_ARRAY('开具单位', '有效日期'), 60),
    (1, 'S2', 15, 1, 1, 1, JSON_ARRAY('本人签名', '说明内容'), 70),
    (1, 'S2', 16, 1, 1, 1, JSON_ARRAY('审查结论', '审查人', '日期'), 80),
    (1, 'S2', 17, 1, 1, 1, JSON_ARRAY('培训时间', '主办单位'), 90),
    (1, 'S2', 18, 1, 1, 1, JSON_ARRAY('成绩', '学校盖章'), 100),
    (1, 'S3', 19, 1, 1, 1, JSON_ARRAY('栏目完整', '介绍人意见', '支部决议'), 10),
    (1, 'S4', 20, 1, 1, 1, JSON_ARRAY('本人签名', '转正日期', '预备期总结'), 10)
ON DUPLICATE KEY UPDATE verify_points = VALUES(verify_points);

INSERT INTO verify_rule (rule_code, rule_name, stage, material_type_id, rule_type, params, severity, enabled, version_no)
VALUES
    ('R-S0-001', '申请书必须上传', 'S0', 1, 'FILE_PRESENT', JSON_OBJECT(), 'HARD', 1, 1),
    ('R-S0-002', '申请书需有本人签名', 'S0', 1, 'KEYWORD_PRESENT', JSON_OBJECT('keywords', JSON_ARRAY('本人签名', '签字', '申请人')), 'HARD', 1, 1),
    ('R-S0-003', '申请书需有日期', 'S0', 1, 'DATE_PRESENT', JSON_OBJECT(), 'HARD', 1, 1),
    ('R-S0-004', '申请书正文不少于200字', 'S0', 1, 'MIN_LENGTH', JSON_OBJECT('minLength', 200), 'SOFT', 1, 1),
    ('R-S1-001', '思想汇报需有本人署名', 'S1', 6, 'KEYWORD_PRESENT', JSON_OBJECT('keywords', JSON_ARRAY('汇报人', '本人', '签名')), 'HARD', 1, 1),
    ('R-S1-002', '思想汇报需有日期', 'S1', 6, 'DATE_PRESENT', JSON_OBJECT(), 'HARD', 1, 1),
    ('R-S1-003', '思想汇报不少于500字', 'S1', 6, 'MIN_LENGTH', JSON_OBJECT('minLength', 500), 'SOFT', 1, 1)
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);

INSERT INTO prompt_template (prompt_code, version_no, content, variables, status, created_by)
VALUES
    ('SEMANTIC_DEFAULT', 1,
     '你是高校党务材料核验助手。请逐条判断材料是否覆盖给定要点。只依据原文，不要臆测；证据必须摘录原文；无法判断输出 UNKNOWN。只输出 JSON，不要 Markdown。输出格式：{"findings":[{"point":"要点","conclusion":"PASS|DOUBT|UNKNOWN","evidence":"原文片段","suggestion":"修改建议","confidence":0.0}]}。\n核验要点：{{check_points}}\n材料文本：{{material_text}}',
     JSON_ARRAY('check_points', 'material_text'), 'PUBLISHED', 1)
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO time_rule (rule_code, academic_year_id, node_code, node_name, start_event, offset_value, offset_unit, warn_offsets, responsible_role, enabled)
VALUES
    ('T-01', 1, 'T-01', '首次谈话', 'APPLICATION_DATE', 30, 'DAY', JSON_ARRAY(30, 15, 7, 3), 'R2', 1),
    ('T-02', 1, 'T-02', '确定积极分子', 'APPLICATION_DATE', 60, 'DAY', JSON_ARRAY(30, 15, 7, 3), 'R2', 1),
    ('T-03', 1, 'T-03', '思想汇报提交', 'QUARTER_START', 0, 'DAY', JSON_ARRAY(15, 7, 3), 'R4', 1),
    ('T-04', 1, 'T-04', '培养考察期满', 'STAGE_START', 12, 'MONTH', JSON_ARRAY(30, 15, 7, 3), 'R2', 1),
    ('T-05', 1, 'T-05', '政治审查完成', 'STAGE_START', 60, 'DAY', JSON_ARRAY(30, 15, 7, 3), 'R3', 1),
    ('T-06', 1, 'T-06', '集中培训完成', 'STAGE_START', 0, 'DAY', JSON_ARRAY(30, 15, 7, 3), 'R3', 1),
    ('T-07', 1, 'T-07', '支部大会讨论接收', 'PREV_NODE', 30, 'DAY', JSON_ARRAY(15, 7, 3), 'R2', 1),
    ('T-08', 1, 'T-08', '上级审批', 'APPROVAL_DATE', 90, 'DAY', JSON_ARRAY(30, 15, 7, 3), 'R3', 1),
    ('T-09', 1, 'T-09', '预备期起算与宣誓', 'APPROVAL_DATE', 0, 'DAY', JSON_ARRAY(), 'R2', 1),
    ('T-10', 1, 'T-10', '预备期满', 'STAGE_START', 12, 'MONTH', JSON_ARRAY(30, 15, 7, 3), 'R4', 1),
    ('T-11', 1, 'T-11', '转正审批', 'STAGE_START', 90, 'DAY', JSON_ARRAY(30, 15, 7, 3), 'R3', 1)
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name);
