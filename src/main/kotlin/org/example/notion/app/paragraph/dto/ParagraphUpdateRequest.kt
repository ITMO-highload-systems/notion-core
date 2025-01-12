package org.example.notion.app.paragraph.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.example.notion.app.paragraph.entity.ParagraphType

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ParagraphUpdateRequest(
    @field:Min(1)
    val id: Long,

    @field:Size(max = 255)
    val title: String,

    val text: String,

    val paragraphType: ParagraphType
)