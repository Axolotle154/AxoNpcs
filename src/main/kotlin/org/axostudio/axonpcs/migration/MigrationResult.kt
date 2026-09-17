package org.axostudio.axonpcs.migration

data class MigrationResult(
    val source: String,
    val successCount: Int,
    val failedCount: Int,
    val errors: List<String> = emptyList()
)
