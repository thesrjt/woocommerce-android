package com.woocommerce.android.config

import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.woocommerce.android.experiment.RESTAPILoginExperiment.RESTAPILoginVariant
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val crashLogging: Provider<CrashLogging>
) : RemoteConfigRepository {
    companion object {
        private const val PERFORMANCE_MONITORING_SAMPLE_RATE_KEY = "wc_android_performance_monitoring_sample_rate"
        private const val REST_API_LOGIN_VARIANT_KEY = "wcandroid_rest_api_login_variant"
        private const val DEBUG_INTERVAL = 10L
        private const val RELEASE_INTERVAL = 31200L
    }

    private val minimumFetchIntervalInSeconds =
        if (PackageUtils.isDebugBuild())
            DEBUG_INTERVAL // 10 seconds
        else
            RELEASE_INTERVAL // 12 hours

    private val changesTrigger = MutableSharedFlow<Unit>(replay = 1)

    private val _fetchStatus = MutableStateFlow(RemoteConfigFetchStatus.Pending)
    override val fetchStatus: Flow<RemoteConfigFetchStatus> = _fetchStatus.asStateFlow()

    private val defaultValues by lazy {
        @Suppress("RemoveExplicitTypeArguments")
        mapOf<String, String>(
            REST_API_LOGIN_VARIANT_KEY to RESTAPILoginVariant.CONTROL.name
        )
    }

    init {
        remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = this@FirebaseRemoteConfigRepository.minimumFetchIntervalInSeconds
                }
            )
            setDefaultsAsync(defaultValues)
                .addOnSuccessListener {
                    changesTrigger.tryEmit(Unit)
                }
        }
    }

    override fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { hasChanges ->
                WooLog.d(T.UTILS, "Remote config fetched successfully, hasChanges: $hasChanges")
                _fetchStatus.value = RemoteConfigFetchStatus.Success
                if (hasChanges) changesTrigger.tryEmit(Unit)
            }
            .addOnFailureListener {
                _fetchStatus.value = RemoteConfigFetchStatus.Failure
                WooLog.e(T.UTILS, it)
            }
    }

    override fun getPerformanceMonitoringSampleRate(): Double =
        remoteConfig.getDouble(PERFORMANCE_MONITORING_SAMPLE_RATE_KEY)

    override fun getRestAPILoginVariant(): RESTAPILoginVariant {
        return try {
            RESTAPILoginVariant.valueOf(remoteConfig.getString(REST_API_LOGIN_VARIANT_KEY).uppercase())
        } catch (e: IllegalArgumentException) {
            crashLogging.get().recordException(e)
            RESTAPILoginVariant.valueOf(defaultValues[REST_API_LOGIN_VARIANT_KEY]!!)
        }
    }

    @VisibleForTesting
    fun observeStringRemoteValue(key: String) = changesTrigger
        .map { remoteConfig.getString(key) }
}
