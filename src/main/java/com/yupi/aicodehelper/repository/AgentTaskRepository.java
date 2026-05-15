package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.AgentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {

    Optional<AgentTask> findByUserIdAndTaskId(Long userId, String taskId);

    Optional<AgentTask> findByTaskId(String taskId);
}
