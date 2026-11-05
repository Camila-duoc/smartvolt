# Icono de la app (logo de Figma) — Smart-Volt

> Cómo pasar tu logo de Figma al icono de Android, sin adivinar.

---

## 1. Qué es el "código" que te da Figma al compartir

Cuando en Figma usas **Share → Copy link** o **Share → Embed**, lo que copias es un **enlace o un `<iframe>`**, por ejemplo:

```html
<iframe style="border: 1px solid rgba(0,0,0,0.1);" width="800" height="450"
        src="https://embed.figma.com/design/XXXXXXXX/Tu-diseño?embed-host=share"
        allowfullscreen></iframe>
```

Eso **no es un archivo**: sirve para **insertar el diseño dentro de una página web** (un blog, un Notion, una wiki). Por eso no descarga nada.

Para el icono necesitas **exportar la imagen** (PNG o SVG), no el iframe.

---

## 2. Exportar el logo desde Figma (paso a paso)

1. Abre el archivo en **figma.com** (con tu cuenta, no el enlace de embed).
2. Selecciona **el frame o el grupo del logo** (haz clic sobre él en el lienzo).
3. En el panel derecho, baja hasta **Export**.
4. Pulsa **`+`** y elige el formato:
   - **PNG** → para un icono normal. Pon la escala en **4x** o el ancho en **1024 px**.
   - **SVG** → mejor si el logo es vectorial (se ve nítido en todos los tamaños).
5. Pulsa **Export** y guarda el archivo.

**Truco rápido (sin panel):**

- Clic derecho sobre el logo → **Copy/Paste as → Copy as PNG** y pégalo en Paint → Guardar como PNG.
- O **Copy as SVG** y pégalo en un archivo `.svg`.

### Recomendaciones para un buen icono

| Cosa | Valor ideal |
|---|---|
| Tamaño de exportación | **1024 × 1024 px** (cuadrado) |
| Fondo | **Transparente** (el fondo lo pone Android) |
| Margen | Deja ~10 % libre alrededor del logo (zona segura) |
| Texto pequeño | Evítalo: no se lee a 48 px |

---

## 3. Cómo está puesto el icono en este proyecto

Android usa **varias imágenes** según la densidad de pantalla y, desde Android 8, un **icono adaptativo**.

```text
app/src/main/res/
├── mipmap-mdpi/ic_launcher.png            48 × 48
├── mipmap-hdpi/ic_launcher.png            72 × 72
├── mipmap-xhdpi/ic_launcher.png           96 × 96
├── mipmap-xxhdpi/ic_launcher.png         144 × 144
├── mipmap-xxxhdpi/ic_launcher.png        192 × 192
├── mipmap-*/ic_launcher_foreground.png   (logo con zona segura)
├── mipmap-*/ic_launcher_monochrome.png   (iconos temáticos de Android 13+)
├── mipmap-anydpi-v26/ic_launcher.xml     (icono adaptativo)
└── values/colors.xml                     (color de fondo)
```

Y en `AndroidManifest.xml`:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ... >
```

> **Importante:** cambiar archivos en `res/` **no cambia el icono del APK ya instalado**.
> Hay que volver a compilar e instalar (`Run ▶` en Android Studio).

---

## 4. Regenerar el icono con Android Studio (sin comandos)

1. En el árbol del proyecto, clic derecho en **`res`** → **New → Image Asset**.
2. **Icon Type:** `Launcher Icons (Adaptive and Legacy)`.
3. **Name:** `ic_launcher`.
4. **Foreground Layer:**
   - **Asset Type:** `Image`
   - **Path:** el PNG/SVG exportado de Figma.
   - Ajusta **Scaling** y el círculo de **safe zone** hasta que se vea centrado.
5. **Background Layer:**
   - **Asset Type:** `Color` y elige el color de fondo (o `Image` si quieres un fondo con diseño).
6. Pestaña **Options:** deja `Generate Monochrome` activado si quieres soporte de iconos temáticos.
7. **Next → Finish**.

Android Studio genera automáticamente todas las densidades y el `ic_launcher.xml` adaptativo.

---

## 5. Verificar que quedó bien

1. Compila e instala:
   ```bat
   C:\Users\camil\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat :app:assembleDemoDebug
   adb -s ZY22HX8K49 install -r app\build\outputs\apk\demo\debug\app-demo-debug.apk
   ```
2. En el celular, abre el **cajón de apps** y busca **Smart-Volt**.
3. Si sigues viendo el icono viejo:
   - Desinstala y reinstala:
     ```bat
     adb -s ZY22HX8K49 uninstall cl.smartvolt.asistencia.demo
     ```
   - O reinicia el launcher (o el teléfono).

---

## 6. Iconos temáticos (Android 13+) y la capa monocroma

Si tu launcher tiene activado **Iconos temáticos** (Material You), Android **ignora los colores** del icono y usa solo la capa `ic_launcher_monochrome` con el color del tema. Por eso a veces el logo se ve como una **silueta plana**.

Para que se siga reconociendo:

- La capa monocroma debe ser una **silueta con los detalles calados** (transparentes), por ejemplo el rayo recortado dentro del pin.
- No basta con pintar el rayo de blanco: si queda del mismo color que el pin, el recorte desaparece.

En este proyecto la capa monocroma ya está generada así (pin con el rayo recortado) para todas las densidades.

Si prefieres ver el icono **a color** en la presentación, desactiva los iconos temáticos en el launcher:

- Moto / Hello UI: mantén pulsado el escritorio → **Configuración del escritorio** → **Iconos temáticos** (o Personalización) → desactivar.
- Pixel / Android puro: mantén pulsado el escritorio → **Estilo y fondos** → desactivar **Iconos temáticos**.

---

## 7. Estado actual

- El logo está en `C:\Users\camil\Downloads\smartvolt.svg` (pin azul con rayo amarillo).
- Ya fue importado con **Image Asset**: existen todas las densidades y el icono adaptativo.
- **Pendiente:** recompilar e instalar para que el celular lo muestre.
