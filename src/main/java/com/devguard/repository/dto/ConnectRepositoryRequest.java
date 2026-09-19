package com.devguard.repository.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConnectRepositoryRequest(
        @NotBlank(message = "el proveedor es obligatorio")
        @Pattern(regexp = "GITHUB|GITLAB", message = "el proveedor debe ser GITHUB o GITLAB")
        String provider,

        @NotBlank(message = "el nombre completo es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]+/[A-Za-z0-9._-]+$",
                 message = "el formato esperado es owner/repo")
        String fullName,

        @NotBlank(message = "la URL de clonado es obligatorio")
        String cloneUrl,

        String defaultBranch,

        @Pattern(regexp = "PUBLIC|PRIVATE|INTERNAL",
                 message = "la visibilidad debe ser PUBLIC, PRIVATE O INTERNAL")
        String visibility,

        @AssertTrue(message = "debe confirmar que esta autorizado a escanear este repositorio")
        Boolean authorizationConfirmed) {
}
