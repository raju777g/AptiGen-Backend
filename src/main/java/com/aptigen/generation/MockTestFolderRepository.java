package com.aptigen.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MockTestFolderRepository extends JpaRepository<MockTestFolder, Long> {
    List<MockTestFolder> findAllByOrderByCreatedAtAsc();
}
