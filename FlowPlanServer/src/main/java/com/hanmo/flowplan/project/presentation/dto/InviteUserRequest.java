package com.hanmo.flowplan.project.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InviteUserRequest(
    String email
) {}