package org.example.notion.app.paragraph.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.example.notion.app.paragraph.entity.ParagraphType
import org.springframework.web.multipart.MultipartFile

data class ParagraphCreateRequest(
    @Min(1)
    val noteId: Long,

    @Size(max = 255)
    val title: String,

    val nextParagraphId: Long?,

    val text: String,

    @NotBlank
    val paragraphType: ParagraphType,

    val images: List<MultipartFile> = emptyList()
)