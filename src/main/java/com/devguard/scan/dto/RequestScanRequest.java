package com.devguard.scan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RequestScanRequest(
        @NotNull(message = "el repositorio es obligatorio")
        UUID repositoryId,
        @Size(max = 200, message = "el nombre de rama es demasiado largo")
        String branch) {
}
