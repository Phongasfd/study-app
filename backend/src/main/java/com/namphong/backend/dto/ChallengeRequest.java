package com.namphong.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dates are inclusive. */
public record ChallengeRequest(@NotBlank @Size(max = 200) String title, @Size(max = 1000) String description, @NotNull @DecimalMin("0.5") @DecimalMax("1000") Double targetHours, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
