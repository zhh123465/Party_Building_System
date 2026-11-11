-- M5 复核闭环：区分党务助理初审与支部书记终审
ALTER TABLE review_log
    ADD COLUMN review_level VARCHAR(16) NOT NULL DEFAULT 'FINAL' AFTER reviewer_id;
