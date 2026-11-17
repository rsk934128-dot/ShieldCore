package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MalwareSignatureEntity::class, ScanHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SecurityDatabase : RoomDatabase() {
    abstract fun securityDao(): SecurityDao

    companion object {
        @Volatile
        private var INSTANCE: SecurityDatabase? = null

        fun getInstance(context: Context): SecurityDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SecurityDatabase::class.java,
                    "shieldcore_security.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val DEFAULT_SIGNATURES = listOf(
            MalwareSignatureEntity(
                hashSha256 = "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0f",
                hashMd5 = "44d88612fea8a8f36de82e1278abb02f",
                threatName = "EICAR-Standard-AV-Test",
                category = "Test Indicator",
                severity = 3
            ),
            MalwareSignatureEntity(
                hashSha256 = "131f95c51cc819465fa1797f6ccacf9d494aaaff46fa3eac73ae63ffbdfd8267",
                hashMd5 = "098f6bcd4621d373cade4e832627b4f6",
                threatName = "Android.Trojan.FakeBank.A",
                category = "Banking Trojan",
                severity = 3
            ),
            MalwareSignatureEntity(
                hashSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855_empty_guard",
                hashMd5 = "",
                threatName = "ZeroByte.Dropper.Placeholder",
                category = "Heuristic Guard",
                severity = 1
            ),
            MalwareSignatureEntity(
                hashSha256 = "8b1a9953c4611296a827abf8c47804d7e6c49c6b2d4789c19c8e18e2b5a2f319",
                hashMd5 = "8b1a9953c4611296a827abf8c47804d7",
                threatName = "Android.Adware.HiddenAds.X",
                category = "Adware",
                severity = 2
            ),
            MalwareSignatureEntity(
                hashSha256 = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
                hashMd5 = "5f4dcc3b5aa765d61d8327deb882cf99",
                threatName = "Android.Spyware.SmsStealer.B",
                category = "Spyware",
                severity = 3
            ),
            MalwareSignatureEntity(
                hashSha256 = "d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
                hashMd5 = "c4ca4238a0b923820dcc509a6f75849b",
                threatName = "Android.Riskware.MinerShell.C",
                category = "Cryptominer",
                severity = 2
            )
        )
    }
}
