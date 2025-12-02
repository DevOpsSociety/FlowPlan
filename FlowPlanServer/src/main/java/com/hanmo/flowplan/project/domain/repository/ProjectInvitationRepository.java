package com.hanmo.flowplan.project.domain.repository;

import com.hanmo.flowplan.project.domain.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {
  Optional<ProjectInvitation> findByToken(String token);
}
