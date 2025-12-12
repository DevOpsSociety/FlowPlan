package com.hanmo.flowplan.task.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
  List<Task> findAllByProjectId(Long projectId);

  // ⭐️ 특정 부모를 가진 자식들 조회 (하향 전파, 상향 계산용)
  List<Task> findAllByParentId(Long parentId);
  boolean existsByParentId(Long parentId);
}
