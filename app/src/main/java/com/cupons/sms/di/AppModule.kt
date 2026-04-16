package com.cupons.sms.di

import android.content.Context
import androidx.room.Room
import com.cupons.sms.data.db.AppDatabase
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.UsageLogDao
import com.cupons.sms.data.repository.CouponRepositoryImpl
import com.cupons.sms.domain.repository.CouponRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI Module — מגדיר כיצד ליצור כל dependency.
 *
 * @InstallIn(SingletonComponent) — הכל singleton ברמת האפליקציה.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideCouponDao(db: AppDatabase): CouponDao = db.couponDao()

    @Provides
    fun provideUsageLogDao(db: AppDatabase): UsageLogDao = db.usageLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * כורך את ה-impl לממשק — ViewModel יקבל CouponRepository (ממשק),
     * Hilt יזריק CouponRepositoryImpl (מימוש).
     */
    @Binds
    @Singleton
    abstract fun bindCouponRepository(
        impl: CouponRepositoryImpl
    ): CouponRepository
}
