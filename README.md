# JARVIS Wake Word v2 — Escucha continua + apertura automática de ChatGPT

Aplicación para Samsung Galaxy A30s (Android 11 / API 30) que escucha «Hey Jarvis» de forma continua y, al detectarlo, **intenta abrir ChatGPT automáticamente** (sin tocar nada).

## Qué hace esta versión

- Escucha continua en segundo plano con servicio en primer plano + notificación persistente.
- Detección local con el modelo `HEY_JARVIS` (openWakeWord + ONNX Runtime).
- **Apertura automática de ChatGPT** al detectar la frase (mejor esfuerzo).
- Si Android bloquea la apertura desde segundo plano → notificación de alta prioridad / full-screen para que solo tengas que tocarla.
- Vibración corta al detectar.
- Control de sensibilidad (threshold) desde la pantalla.
- Sincronización del estado del servicio con la UI.
- Botón «Probar abrir ChatGPT».
- WakeLock parcial para mantener la CPU activa mientras escucha (máx. 6 h de seguridad).
- GitHub Actions para compilar el APK sin ordenador.

## Limitaciones reales de Android 11

Android puede bloquear `startActivity` desde un servicio en segundo plano. Por eso:

1. Primero se intenta abrir ChatGPT solo.
2. Siempre se muestra una notificación de detección.
3. Si no se abrió solo, al tocar la notificación se abre con seguridad.

No se garantiza funcionamiento con la pantalla completamente bloqueada ni auto-inicio al reiniciar el teléfono. El ahorro de batería de Samsung puede matar el servicio; desactívalo para esta app en Ajustes → Batería.

## Compilar

### En el ordenador
Abre el proyecto en Android Studio o:

```bash
gradle :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Sin ordenador (GitHub Actions)
1. Sube el contenido de esta carpeta a un repositorio de GitHub (raíz = settings.gradle.kts + app/ + .github/).
2. Actions → **Build JARVIS Wake Word APK** → Run workflow.
3. Descarga el artefacto `JARVISWakeWord-debug-apk`.

## Uso recomendado

1. Instala ChatGPT desde Play Store.
2. Abre JARVIS Wake Word → concede micrófono.
3. Ajusta sensibilidad (0.40–0.55 suele ir bien).
4. Pulsa **Iniciar escucha continua**.
5. Di «Hey Jarvis» con claridad.
6. Deja la app en segundo plano; no hace falta mantener la pantalla abierta.

## Consejo importante en Samsung

Ve a **Ajustes → Aplicaciones → JARVIS Wake Word → Batería** y ponla en **Sin restricciones** / **No optimizar**. Si no, Samsung puede matar el servicio de escucha.

## Licencias

- Código de la app: uso personal.
- openwakeword-android: Apache 2.0.
- Modelos HEY_JARVIS: **CC BY-NC-SA 4.0** (solo no comercial / personal).

## Archivos principales

- `MainActivity.kt` — UI, permisos, umbral, sincronización.
- `WakeWordService.kt` — servicio continuo, vibración, WakeLock, intento de apertura automática.
- `WakeWordDetector.kt` — integración openWakeWord.
- `ChatGPTLauncher.kt` — apertura de ChatGPT.
- `NotificationHelper.kt` — notificaciones de escucha y detección.

---

Proyecto experimental. Prueba siempre en el A30s real.
