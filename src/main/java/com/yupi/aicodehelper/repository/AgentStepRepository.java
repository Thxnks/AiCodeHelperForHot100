package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.AgentStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentStepRepository extends JpaRepository<AgentStep, Long> {

    List<AgentStep> findByTaskIdOrderByStepOrderAsc(String taskId);
}
