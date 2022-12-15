package com.woocommerce.android.di

import android.os.Build
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.module.ApplicationPasswordClientId
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsListener(
        notifier: ApplicationPasswordsNotifier
    ): ApplicationPasswordsListener

    companion object {
        @Provides
        @ApplicationPasswordClientId
        fun providesApplicationPasswordClientId() = "${BuildConfig.APPLICATION_ID}.app-client.${Build.DEVICE}"
    }
}