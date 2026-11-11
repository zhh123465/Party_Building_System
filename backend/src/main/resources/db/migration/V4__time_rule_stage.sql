-- M7 时限预警：规则增加适用阶段、节点类型与依赖关系
ALTER TABLE time_rule
    ADD COLUMN stage VARCHAR(4) NULL AFTER node_code,
    ADD COLUMN node_type VARCHAR(16) NOT NULL DEFAULT 'FIXED' AFTER start_event;

UPDATE time_rule SET stage = 'S0' WHERE node_code IN ('T-01', 'T-02');
UPDATE time_rule SET stage = NULL WHERE node_code = 'T-03';
UPDATE time_rule SET stage = 'S1' WHERE node_code = 'T-04';
UPDATE time_rule SET stage = 'S2' WHERE node_code IN ('T-05', 'T-06', 'T-07');
UPDATE time_rule SET stage = 'S3' WHERE node_code IN ('T-08', 'T-09', 'T-10');
UPDATE time_rule SET stage = 'S4' WHERE node_code = 'T-11';

UPDATE time_rule SET node_type = 'QUARTER' WHERE node_code = 'T-03';
UPDATE time_rule SET node_type = 'EVENT' WHERE node_code IN ('T-06', 'T-09');
UPDATE time_rule SET node_type = 'PREV' WHERE node_code = 'T-07';

UPDATE time_rule SET depends_on_node = 'T-02' WHERE node_code = 'T-04';
UPDATE time_rule SET depends_on_node = 'T-04' WHERE node_code IN ('T-05', 'T-06');
UPDATE time_rule SET depends_on_node = 'T-05,T-06' WHERE node_code = 'T-07';
UPDATE time_rule SET depends_on_node = 'T-07' WHERE node_code = 'T-08';
UPDATE time_rule SET depends_on_node = 'T-08' WHERE node_code = 'T-09';
UPDATE time_rule SET depends_on_node = 'T-09' WHERE node_code = 'T-10';
UPDATE time_rule SET depends_on_node = 'T-10' WHERE node_code = 'T-11';
