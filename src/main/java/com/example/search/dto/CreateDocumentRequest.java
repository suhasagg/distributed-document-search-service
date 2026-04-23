package com.example.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 100000) String content
) {}
