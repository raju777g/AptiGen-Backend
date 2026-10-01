CREATE TABLE mock_test_folders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_mock_test_folders_parent (parent_id),
    CONSTRAINT fk_mock_test_folders_parent FOREIGN KEY (parent_id) REFERENCES mock_test_folders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE mcq_tests
    ADD COLUMN is_mock_test BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mock_folder_id BIGINT NULL,
    ADD INDEX idx_mcq_tests_mock (is_mock_test, mock_folder_id),
    ADD CONSTRAINT fk_mcq_tests_mock_folder FOREIGN KEY (mock_folder_id) REFERENCES mock_test_folders(id) ON DELETE SET NULL;
