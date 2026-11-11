-- 补全 T-02 与 T-01 的依赖链
UPDATE time_rule SET depends_on_node = 'T-01' WHERE node_code = 'T-02';
