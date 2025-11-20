package com.hanmo.flowplan.projectMember.domain;

import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
  Optional<ProjectMember> findByUserAndProject(User user, Project project);
  boolean existsByUserAndProject(User user, Project project);
}
