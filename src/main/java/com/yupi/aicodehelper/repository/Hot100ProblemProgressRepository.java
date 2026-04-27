package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.Hot100ProblemProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Hot100ProblemProgressRepository extends JpaRepository<Hot100ProblemProgress, Long> {

    Optional<Hot100ProblemProgress> findByProblemSlug(String problemSlug);

    List<Hot100ProblemProgress> findAllByOrderByUpdatedAtDesc();

    List<Hot100ProblemProgress> findByStatusOrderByUpdatedAtDesc(String status);

    Optional<Hot100ProblemProgress> findByUserIdAndProblemSlug(Long userId, String problemSlug);

    List<Hot100ProblemProgress> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Hot100ProblemProgress> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);

    List<Hot100ProblemProgress> findByUserId(Long userId);
}
