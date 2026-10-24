package cl.smartvolt.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cl.smartvolt.asistencia.ui.navigation.AppNav
import cl.smartvolt.asistencia.ui.theme.SmartVoltTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartVoltTheme { AppNav() } }
    }
}
