package com.duisternis.voidgrid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Lista de categorias salva como CSV — ex: "rpg,art,scenario"
    // Usada pelo ForYou para enriquecer a query: "mork borg rpg art scenario"
    val categories: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Converte o CSV em lista limpa para uso no ViewModel
    fun categoryList(): List<String> =
        categories.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
}