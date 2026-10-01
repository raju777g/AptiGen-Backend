package com.aptigen.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestFolderRepository extends JpaRepository<TestFolder, Long> {
    List<TestFolder> findByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);
}