package com.devguard.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(min = 3, max = 100, message = "el nombre debe tener entre 3 y 100 caracteres")
        String name,

        @Size(max = 500, message = "la descripcion no puede exceder 500 caracteres")
        String description) {

}
