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
    version = 2, // incrementado por causa do campo tags
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