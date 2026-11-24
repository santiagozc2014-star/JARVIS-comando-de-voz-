# JARVIS Wake Word v2 — Escucha continua y apertura de ChatGPT

Aplicación experimental para Samsung Galaxy A30s (Android 11 / API 30) que reconoce localmente «Hey Jarvis».

Esta configuración mantiene `targetSdk 30` para el dispositivo Android 11 probado y la distribución directa del APK. No está preparada para publicarse en Google Play: antes habría que migrar a un nivel API vigente y adaptar permisos y servicio de micrófono, y repetir las pruebas en Android recientes.

## Qué hace esta versión

- Escucha continua mediante un servicio en primer plano y una notificación persistente.
- Detección local con el modelo `HEY_JARVIS` (openWakeWord + ONNX Runtime).
- Vibración corta al detectar la frase.
- La vibración la genera una sola vez la aplicación; el canal de notificación de detección no añade una segunda vibración.
- Intenta invocar el asistente configurado en Android; si esa solicitud falla, prueba abrir ChatGPT y, como último recurso, Play Store.
- Siempre muestra una notificación de detección. El usuario ya ha confirmado en el Galaxy A30s que tocarla abre ChatGPT.
- Control de sensibilidad desde la pantalla.
- WakeLock parcial con límite de seguridad de 6 horas.
- Compilación del APK mediante GitHub Actions.
- Verificación criptográfica del APK con `apksigner`, huella del certificado y suma SHA-256 antes de publicar el artefacto.

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
La canalización de CI solo publica el APK si el análisis Lint, las pruebas unitarias, la compilación y la verificación de firma terminan correctamente.

La canalización también puede generar un APK **release firmado de forma estable** cuando estén configurados los cuatro secretos de GitHub siguientes: `JARVIS_KEYSTORE_BASE64`, `JARVIS_KEYSTORE_PASSWORD`, `JARVIS_KEY_ALIAS` y `JARVIS_KEY_PASSWORD`. La clave privada se descodifica solo en el entorno temporal del runner y nunca se incluye en el repositorio ni en los artefactos.

La creación y custodia del keystore debe realizarla un adulto responsable. Debe conservarse una copia de seguridad segura fuera de GitHub; perder esa clave impediría publicar futuras actualizaciones compatibles. No compartas contraseñas ni archivos de clave en el chat. Hasta que los cuatro secretos estén configurados, el flujo seguirá produciendo únicamente el APK debug temporal.


**Aviso de instalación:** el APK de depuración de GitHub Actions se firma con una clave temporal del entorno de compilación. No debe tratarse como una actualización instalable garantizada sobre una versión anterior. Para actualizaciones sin desinstalar ni perder datos habrá que configurar una clave de firma estable, custodiada por un adulto responsable, mediante secretos de GitHub. No se debe subir la clave privada al repositorio.

En la revisión del 22-11-2026 se confirmó que el certificado del APK de la ejecución #87 es distinto al del APK de la ejecución #72. Android rechazará el nuevo APK como actualización de aquella instalación. **No desinstales la versión que ya funciona ni instales este APK encima.** Desinstalar puede borrar los datos locales. La clave privada temporal de la ejecución antigua no está disponible; antes de migrar habrá que acordar una instalación segura y conservar los datos que se quieran mantener.

La pantalla de conversación nativa sigue usando `NotConfiguredAiProvider`: es una interfaz local, no un asistente conectado a ChatGPT. No envía mensajes ni audio a un modelo. La integración con IA requiere una arquitectura y credenciales autorizadas separadas.

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

## Wake-word service compatibility and known limitations

The foreground service now supplies Android's microphone service type starting at API 30 (Android 11). Android 8–10 continue to use the legacy service type value.

The pinned `openwakeword-android` dependency still has an upstream capture-thread limitation: negative `AudioRecord.read()` results are not classified separately from short reads, and failures inside its worker thread are logged but are not exposed through the application's error callback. A persistent capture failure can therefore leave the wrapper's requested-listening state out of sync with actual audio processing. This application change does not claim to repair that dependency behavior. A follow-up dependency fork or an app-owned capture pipeline is required before claiming automatic recovery from microphone errors.

The unit tests for the foreground-service type are JVM tests. They do not validate microphone capture, ONNX inference, screen-off operation, Samsung One UI battery management, CPU use, or wake-word accuracy. Those require Android instrumentation and physical testing on the Galaxy A30s.
