package com.hanmo.flowplan.projectMember.domain;

import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.user WHERE pm.project.id = :projectId")
  List<ProjectMember> findAllByProjectId(@Param("projectId") Long projectId);
  List<ProjectMember> findAllByUser(User user);
  Optional<ProjectMember> findByUserAndProject(User user, Project project);
  boolean existsByUserAndProject(User user, Project project);
}
