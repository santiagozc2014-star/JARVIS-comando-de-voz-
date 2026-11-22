# JARVIS Wake Word v2 — Escucha continua y apertura de ChatGPT

Aplicación experimental para Samsung Galaxy A30s (Android 11 / API 30) que reconoce localmente «Hey Jarvis».

## Qué hace esta versión

- Escucha continua mediante un servicio en primer plano y una notificación persistente.
- Detección local con el modelo `HEY_JARVIS` (openWakeWord + ONNX Runtime).
- Vibración corta al detectar la frase.
- Intenta invocar el asistente configurado en Android; si esa solicitud falla, prueba abrir ChatGPT y, como último recurso, Play Store.
- Siempre muestra una notificación de detección. El usuario ya ha confirmado en el Galaxy A30s que tocarla abre ChatGPT.
- Control de sensibilidad desde la pantalla.
- WakeLock parcial con límite de seguridad de 6 horas.
- Compilación del APK mediante GitHub Actions.

## Importante: solicitud no equivale a apertura

Android restringe la apertura de actividades desde segundo plano. Desde Android 10, un servicio en primer plano no obtiene por sí solo permiso para interrumpir al usuario abriendo otra aplicación. Además, el sistema puede bloquear una solicitud sin lanzar una excepción.

Por eso, JARVIS distingue ahora entre «Android aceptó la solicitud» y «ChatGPT se abrió». La aplicación no afirma que ChatGPT se haya abierto automáticamente cuando no puede confirmarlo.

La ruta de notificación sí está comprobada en el teléfono: al tocar el aviso de detección, se abre ChatGPT. En versiones compatibles de ChatGPT, la opción **Ajustes → Voz → Iniciar con voz** puede iniciar el modo de voz al abrir un chat nuevo o vacío. Su disponibilidad depende de la versión de la aplicación.

## Uso

1. Instala ChatGPT desde Play Store.
2. Abre JARVIS Wake Word y concede el permiso del micrófono.
3. Ajusta la sensibilidad según el entorno.
4. Pulsa **Iniciar escucha continua**.
5. Di «Hey Jarvis».
6. Si Android no abre ChatGPT automáticamente, toca la notificación de detección.

En Samsung, configura **Ajustes → Aplicaciones → JARVIS Wake Word → Batería → Sin restricciones** si el sistema detiene el servicio. La escucha continua consume batería y debe permanecer visible mediante su notificación.

## Compilar

En Android Studio o con:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

En GitHub: abre **Actions → Build JARVIS Android APK** y descarga el artefacto `JARVISWakeWord-debug-apk` cuando la ejecución termine correctamente.

## Archivos principales

- `MainActivity.kt` — interfaz, permisos, sensibilidad y estado.
- `WakeWordService.kt` — escucha, vibración, WakeLock y notificación.
- `WakeWordDetector.kt` — integración del motor openWakeWord.
- `ChatGPTLauncher.kt` — solicitudes de apertura.
- `LaunchRequestPolicy.kt` — orden de intentos y resultado comunicado.
- `NotificationHelper.kt` — notificaciones de escucha y detección.

## Licencias

- Código de la aplicación: uso personal.
- openwakeword-android: Apache 2.0.
- Modelo HEY_JARVIS: CC BY-NC-SA 4.0 (uso no comercial).

Proyecto experimental. La validación final de reconocimiento de voz y apertura debe realizarse en el Galaxy A30s real.
