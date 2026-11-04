# Conectar el emulador o un celular — Smart-Volt

> Guía rápida para que la app se vea en el emulador y para presentar desde un teléfono físico.
> Probada en este equipo con Windows, Android SDK en `C:\Users\camil\AppData\Local\Android\Sdk`.

---

## 1. Diagnóstico en 10 segundos

Abre una terminal y ejecuta:

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l
```

| Resultado | Significado | Solución |
|---|---|---|
| `device` | Todo bien | Puedes instalar/abrir la app |
| `offline` | El emulador/teléfono no responde | Reiniciar adb o arrancar en frío |
| `unauthorized` | El teléfono no autorizó la PC | Aceptar el aviso “¿Permitir depuración USB?” |
| Lista vacía | No hay dispositivo conectado | Abrir el emulador o revisar el cable |

> Si `adb` no se reconoce, usa la ruta completa de arriba o agrega
> `C:\Users\camil\AppData\Local\Android\Sdk\platform-tools` al `PATH`.

### 1.1 Si sale `"adb" no se reconoce como un comando...`

Ese mensaje **no significa que adb esté roto**: significa que Windows no sabe dónde está.
En este equipo ya quedó configurado el PATH de usuario con:

```text
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools
C:\Users\camil\AppData\Local\Android\Sdk\emulator
```

**Importante:** los cambios de PATH no afectan terminales ya abiertas.

1. Cierra **todas** las terminales (cmd, PowerShell, Windows Terminal).
2. Si usas la terminal de Android Studio, **cierra Android Studio completo**.
3. Abre una terminal nueva y prueba:
   ```bat
   adb devices -l
   ```

Si por alguna razón vuelve a fallar, revisa el registro con:

```bat
reg query HKCU\Environment /v Path
```

Y si no está, agrégalo (sin comillas alrededor del valor):

```bat
reg add HKCU\Environment /v Path /t REG_EXPAND_SZ /d C:\Users\camil\AppData\Local\Android\Sdk\platform-tools;C:\Users\camil\AppData\Local\Android\Sdk\emulator /f
```

También puedes usar la ruta completa sin tocar el PATH:

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l
```

---

## 2. Si el emulador no se ve o sale `offline`

### 2.1 Reiniciar el puente adb

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe kill-server
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l
```

### 2.2 Arrancar el emulador en frío y visible

Cierra el emulador y ejecuta:

```bat
cd C:\Users\camil\AppData\Local\Android\Sdk\emulator
emulator.exe -avd Pixel_10_Pro -no-snapshot-load -gpu auto
```

- `-no-snapshot-load` evita arrancar desde una instantánea dañada.
- **No** uses `-qt-hide-window`: eso oculta la ventana y parece que “no pasa nada”.

Si sigue fallando, prueba con:

```bat
emulator.exe -avd Pixel_10_Pro -no-snapshot-load -wipe-data
```

`-wipe-data` borra los datos del emulador (la app se reinstala y la semilla se recarga).

### 2.3 Comprobar que terminó de arrancar

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe shell getprop sys.boot_completed
```

Debe devolver `1`. Si devuelve vacío, todavía está arrancando.

### 2.4 Si sale “System UI isn’t responding”

Es un aviso del **emulador lento**, no de la app. Toca **Wait** y espera.
Para reducirlo:

- Cierra Chrome/Android Studio si están consumiendo mucha RAM.
- En el AVD, baja la resolución o usa `-gpu swiftshader_indirect` si el GPU da problemas.
- La primera cold boot siempre es la más lenta; las siguientes son más rápidas.

---

## 3. Conectar un celular Android (recomendado para presentar)

### 3.1 Preparar el teléfono

1. **Ajustes → Acerca del teléfono**.
2. Toca **Número de compilación** 7 veces hasta que diga “Ya eres desarrollador”.
3. Vuelve a **Ajustes → Sistema → Opciones de desarrollador**.
4. Activa **Depuración USB**.
5. Conecta el teléfono por USB a la PC.
6. En el teléfono, elige **Transferencia de archivos (MTP)** si aparece la opción.
7. Acepta el aviso **“¿Permitir depuración USB?”** y marca **“Confiar siempre en esta computadora”**.

### 3.2 Verificar que la PC lo detecta

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l
```

Debe aparecer algo como:

```text
R58XXXXXXXX     device product:... model:...
```

Si aparece `unauthorized`, revisa la pantalla del teléfono y acepta el aviso.
Si no aparece nada:

- Prueba otro cable USB (muchos cables solo cargan).
- Prueba otro puerto USB.
- Instala los drivers USB del fabricante (Samsung, Xiaomi, etc.) si Windows no lo reconoce.

### 3.3 Ejecutar la app desde Android Studio

1. Abre el proyecto en Android Studio.
2. Arriba, en el selector de variante, elige **`demoDebug`**.
3. En el selector de dispositivo, elige tu teléfono.
4. Presiona **Run ▶**.

La variante **`demo`** usa sensores simulados, así que **no necesitas GPS ni beacons reales** para la presentación.

---

## 4. ¿Emulador o celular?

| Criterio | Emulador | Celular físico |
|---|---|---|
| Velocidad | Depende del PC; puede ser lento | Normalmente más rápido |
| Configuración | Cero cables | Requiere depuración USB y drivers |
| Sensores reales | No | Sí (solo flavor `campo`) |
| Riesgo en presentación | Fallos de arranque/GPU | Batería, cable, permisos |
| Recomendación | Para desarrollar | **Para presentar** |

**Consejo:** presenta con el celular y el flavor `demoDebug`. Deja el emulador como respaldo ya abierto y con la app instalada.

---

## 5. Si usas el flavor `campo` en un celular

`campo` usa Retrofit contra `API_BASE_URL`. En `app/build.gradle.kts` el valor por defecto es:

```kotlin
debug { buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"") }
```

`10.0.2.2` **solo funciona dentro del emulador** (es la PC vista desde el emulador).
En un celular físico tienes dos opciones:

### Opción A — Misma red Wi-Fi

Cambia la URL por la IP local de tu PC, por ejemplo:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.50:8080/\"")
```

El celular y la PC deben estar en la misma red, y el firewall debe permitir el puerto 8080.

### Opción B — `adb reverse` (más simple, sin tocar la IP)

Con el celular conectado por USB:

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe reverse tcp:8080 tcp:8080
```

Y usa `http://127.0.0.1:8080/` como `API_BASE_URL`.

> Para la presentación no necesitas backend: usa `demoDebug` y `FakeApiService`.

---

## 6. Instalar y abrir sin Android Studio (plan B)

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\demo\debug\app-demo-debug.apk
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n cl.smartvolt.asistencia.demo/cl.smartvolt.asistencia.MainActivity
```

Si sale `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, desinstala primero:

```bat
C:\Users\camil\AppData\Local\Android\Sdk\platform-tools\adb.exe uninstall cl.smartvolt.asistencia.demo
```

---

## 7. Checklist antes de la presentación

- [ ] Celular cargado o conectado a corriente.
- [ ] `adb devices` muestra `device`.
- [ ] Variante `demoDebug` compilada.
- [ ] App instalada y abierta al menos una vez.
- [ ] Permiso de notificaciones aceptado.
- [ ] Login probado con `OP-0001` / `1234`.
- [ ] Una marca de prueba realizada (para que se vea el resultado).
- [ ] Emulador de respaldo abierto (opcional).
- [ ] Datos del emulador/celular reiniciables: Ajustes → Apps → Smart-Volt → Borrar datos.

---

## 8. Credenciales de la demo

| Rol | Código | PIN |
|---|---|---|
| Operario | `OP-0001` | `1234` |
| Capataz | `CAP-0001` o `CAP-0002` | `1234` |
| Administración | `ADM-0001` | `1234` |
