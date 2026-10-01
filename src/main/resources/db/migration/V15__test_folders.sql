CREATE TABLE test_folders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_test_folders_owner_parent (owner_user_id, parent_id),
    CONSTRAINT fk_test_folders_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_test_folders_parent FOREIGN KEY (parent_id) REFERENCES test_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE mcq_tests
    ADD COLUMN folder_id BIGINT NULL,
    ADD INDEX idx_mcq_tests_folder (folder_id),
    ADD CONSTRAINT fk_mcq_tests_folder FOREIGN KEY (folder_id) REFERENCES test_folders(id) ON DELETE SET NULL;