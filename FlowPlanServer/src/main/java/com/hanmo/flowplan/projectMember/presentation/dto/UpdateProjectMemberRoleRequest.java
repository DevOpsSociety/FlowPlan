package com.hanmo.flowplan.projectMember.presentation.dto;

import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectMemberRoleRequest(
    @NotNull
    ProjectRole role // 변경할 권한 (EDITOR, VIEWER 등)
) {}