package com.hanmo.flowplan.project.domain.repository;

import com.hanmo.flowplan.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  // ⭐️ [핵심] "Touch" 메서드
  // Task가 변경될 때 이 메서드를 호출하여 프로젝트의 updatedAt을 현재 시간으로 갱신합니다.
  @Modifying(clearAutomatically = true)
  @Query("UPDATE Project p SET p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :projectId")
  void updateLastModifiedDate(@Param("projectId") Long projectId);

}
