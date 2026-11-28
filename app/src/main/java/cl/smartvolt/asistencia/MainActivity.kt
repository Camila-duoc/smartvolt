package cl.smartvolt.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cl.smartvolt.asistencia.ui.comun.navigation.AppNav
import cl.smartvolt.asistencia.ui.comun.theme.SmartVoltTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartVoltTheme { AppNav() } }
    }
}
