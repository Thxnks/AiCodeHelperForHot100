package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    List<AgentMemory> findByUserIdOrderByImportanceDescUpdatedAtDesc(Long userId);

    List<AgentMemory> findTop20ByUserIdOrderByImportanceDescUpdatedAtDesc(Long userId);
}
