package cl.smartvolt.asistencia.di

import android.content.Context
import androidx.room.Room
import cl.smartvolt.asistencia.BuildConfig
import cl.smartvolt.asistencia.viewmodel.AppDatabase
import cl.smartvolt.asistencia.data.remote.ApiService
import cl.smartvolt.asistencia.data.remote.FakeApiService
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.data.repository.RoomAttendanceRepository
import cl.smartvolt.asistencia.domain.antifraude.RuleEngine
import cl.smartvolt.asistencia.sensors.*
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): AppDatabase = AppDatabase.getInstance(c) // TODO: SQLCipher (RNF-08)

    @Provides fun configDao(db: AppDatabase) = db.configDao()
    @Provides fun marcaDao(db: AppDatabase) = db.marcaDao()

    @Provides @Singleton
    fun attendanceRepository(impl: RoomAttendanceRepository): AttendanceRepository = impl

    @Provides @Singleton
    fun api(): ApiService {
        if (BuildConfig.USE_FAKE_API) return FakeApiService() // demo: sin backend
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ApiService::class.java)
    }

    @Provides @Singleton fun ruleEngine(): RuleEngine = RuleEngine.porDefecto()

    // Sensores: simulados en el flavor "demo", reales en "campo".
    @Provides @Singleton
    fun location(@ApplicationContext c: Context): LocationProvider =
        if (BuildConfig.USE_FAKE_SENSORS) FakeLocationProvider() else FusedLocationProviderImpl(c)

    @Provides @Singleton
    fun ble(@ApplicationContext c: Context): BleScanner =
        if (BuildConfig.USE_FAKE_SENSORS) FakeBleScanner() else BleScannerImpl(c)

    @Provides @Singleton
    fun system(@ApplicationContext c: Context): SystemStateProvider =
        if (BuildConfig.USE_FAKE_SENSORS) FakeSystemStateProvider() else SystemStateProviderImpl(c)
}
