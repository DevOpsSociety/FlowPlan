package com.hanmo.flowplan.task.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
  List<Task> findAllByProjectId(Long projectId);
}
