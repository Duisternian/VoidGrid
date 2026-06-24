package com.duisternis.voidgrid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duisternis.voidgrid.data.local.dao.PinsDao
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity

@Database(
    entities = [FolderEntity::class, PinEntity::class],
    version = 3, // v2→v3: remove pins.tags, adiciona folders.categories
    exportSchema = false
)
abstract class VoidGridDatabase : RoomDatabase() {
    abstract fun pinsDao(): PinsDao

    companion object {
        fun create(context: Context): VoidGridDatabase =
            Room.databaseBuilder(context, VoidGridDatabase::class.java, "voidgrid.db")
                .fallbackToDestructiveMigration() // apaga e recria — ok em dev
                .build()
    }
}