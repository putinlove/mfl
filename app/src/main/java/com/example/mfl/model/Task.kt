package com.example.mfl.model

import java.util.UUID


data class Task(
    val id: String = UUID.randomUUID().toString(), // Уникальный идентификатор
    var title: String = "",
    var assignedId: String = "",
    var executorId: String = "",
    var description: String = "",
    var dueTime: String = ""
)


