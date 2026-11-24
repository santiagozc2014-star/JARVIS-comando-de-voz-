# Plan de rendimiento: Samsung Galaxy A30s (Android 11)

## Estado y alcance

Este documento define una línea base reproducible para medir consumo y estabilidad de JARVIS en el Samsung Galaxy A30s con Android 11 / API 30. No contiene resultados de medición física: no se ha ejecutado la aplicación en el dispositivo durante esta auditoría.

Rama inspeccionada: `jarvis/rebuild-android-project`  
Commit base inspeccionado: `5f15ab2b4fa6386b70217595f3330f70896b4863`  
Dependencia de detección: `com.github.msnilsen:openwakeword-android:b49f3ab14cf558ac0c2aef73a5e6c83da7a08558`

La primera entrega es deliberadamente documental. No se modifican umbrales, modelos, frecuencia de inferencia, captura de audio ni gestión del WakeLock antes de disponer de mediciones comparables. No se puede afirmar una mejora de batería o CPU sin una comparación A/B en el mismo teléfono.

## Hallazgos confirmados por código

### Pipeline ONNX

La versión exacta fijada por Gradle ejecuta tres sesiones ONNX: espectrograma Mel, embedding de voz y clasificador de palabra clave. La captura es mono a 16 kHz y procesa bloques de 1.280 muestras (80 ms), es decir, hasta 12,5 iteraciones de audio por segundo. Tras el calentamiento de las ventanas, cada iteración ejecuta las etapas de inferencia correspondientes.

Las sesiones se configuran con `setIntraOpNumThreads(1)` y `setInterOpNumThreads(1)`. El código mantiene las sesiones entre ciclos `stop/start` de la misma instancia y las cierra en `release()`. No se debe aumentar el paralelismo sin medirlo: más hilos pueden elevar contención y consumo en un dispositivo de recursos limitados.

### Memoria de buffers

Estimación derivada de las dimensiones del código de la dependencia, no de una lectura de RAM del proceso:

| Buffer | Capacidad | Memoria de datos aproximada |
| --- | ---: | ---: |
| Audio circular | 160.000 valores Float | 625 KiB |
| Mel buffer máximo | 970 × 32 valores Float | 121 KiB |
| Feature buffer máximo | 120 × 96 valores Float | 45 KiB |
| Ventana Mel inicial | 76 × 32 valores Float | 9,5 KiB |

Estos valores no incluyen cabeceras de objetos, listas, arrays temporales, tensores, memoria nativa de ONNX Runtime, pesos cargados ni AudioRecord. El tamaño de los archivos de modelo tampoco equivale a su consumo de RAM en ejecución.

### Captura y ciclo de vida

- `WakeWordService` declara un servicio en primer plano de tipo `microphone` y llama a `startForeground` con el tipo explícito en API 30.
- `WakeWordDetector` comprueba periódicamente el estado del hilo, AudioRecord y el progreso de predicciones mediante reflexión sobre campos privados de la dependencia.
- El diagnóstico calcula RMS/pico sobre hasta 16.000 muestras cada 10 segundos. Es telemetría de nivel, no almacenamiento de audio.
- El servicio adquiere un `PARTIAL_WAKE_LOCK` con timeout de seis horas y lo libera durante la parada/destrucción del servicio.
- La dependencia fijada espera hasta tres segundos en `processingThread.join(3000)` antes de liberar AudioRecord. Si el hilo no termina en ese plazo, la implementación continúa con la liberación; debe probarse si queda trabajo de inferencia en curso antes de cerrar las sesiones ONNX.
- El repositorio fija `targetSdk 30`, `minSdk 26`, Java/Kotlin 17 y no declara dependencias adicionales para telemetría de rendimiento.

## Riesgos que requieren verificación

1. **Parada de inferencia:** el `join(3000)` de la dependencia puede bloquear el hilo que invoca `stop()` y, si vence el timeout, la liberación posterior de sesiones podría coincidir con trabajo pendiente. Reproducir repetidos ciclos de inicio/parada y revisar Logcat, ANR y excepciones ONNX.
2. **Reflexión:** el detector depende de nombres de campos privados de la biblioteca. Una actualización del artefacto podría romper las comprobaciones aunque la captura siga activa.
3. **WakeLock:** seis horas es un límite temporal, no una estrategia de renovación. Verificar qué ocurre al superar el timeout y si el usuario entiende que la escucha puede dejar de ser fiable.
4. **Reinicio del proceso:** `START_STICKY` solicita al sistema que recree el servicio en determinados casos; no garantiza recuperación del micrófono. En Android 11, un servicio de micrófono iniciado desde segundo plano puede no tener acceso al micrófono. Probar reinicio del proceso con pantalla encendida/apagada y permiso concedido.
5. **Samsung One UI:** las políticas de ahorro de batería y suspensión de aplicaciones pueden cambiar el comportamiento. Registrar versión exacta de One UI y ajustes de batería durante cada prueba.
6. **Fugas:** confirmar que AudioRecord, los tres OrtSession, OrtEnvironment, el hilo de procesamiento y WakeLock se liberan al detener, fallar y recrear el servicio.

## Protocolo de medición A/B

Usar el mismo dispositivo, firmware, versión APK, nivel aproximado de batería, temperatura inicial, volumen y entorno acústico. Repetir cada escenario al menos tres veces. No cambiar umbral, modelo, frecuencia de audio ni backend entre A y B.

### Escenarios

- **S0 — reposo:** JARVIS instalado, servicio detenido, 15 minutos.
- **S1 — pantalla encendida:** escucha continua durante 15 minutos, sin detecciones.
- **S2 — pantalla apagada:** escucha continua durante 30 minutos, sin detecciones; comprobar también que el servicio sigue mostrando estado coherente.
- **S3 — detecciones:** 20 frases objetivo pronunciadas desde distancias y niveles habituales; anotar detecciones correctas y falsas activaciones en un conjunto separado de audio negativo.
- **S4 — estabilidad:** 2 horas de escucha, con comprobaciones de estado cada 15 minutos.
- **S5 — ciclo de vida:** 20 ciclos de iniciar/detener, después revocar y volver a conceder el permiso, y comprobar el comportamiento ante una interrupción del servicio.
- **S6 — recuperación:** terminar el proceso desde herramientas de desarrollo y observar el comportamiento de START_STICKY; probar por separado reinicio del teléfono. No confundir recuperación del proceso con inicio automático tras reinicio.

### Métricas

| Métrica | Método | Qué registrar |
| --- | --- | --- |
| RAM | Android Studio Profiler o `adb shell dumpsys meminfo com.jarvis.wakeword` | PSS, Private Dirty, Native Heap y evolución temporal |
| CPU | Android Studio Profiler / Perfetto; `adb shell top -b -n 1` | CPU media y picos, duración de inferencia si está instrumentada |
| Batería | `adb shell dumpsys batterystats` antes/después, más duración y porcentaje | consumo atribuido a JARVIS y duración; no inferir mAh exactos del porcentaje |
| WakeLock | `adb shell dumpsys power` | nombre, estado y duración del bloqueo |
| Audio | Logcat de `WakeWordDetector` y `WakeWordService` | errores, interrupciones, tiempo hasta capturar y continuidad de muestras |
| ONNX | trazas de tiempo alrededor de cada etapa, si se añade instrumentación | latencia p50/p95 y errores por etapa |
| Estabilidad | Logcat, ANR y ciclos S4/S5 | crashes, hilos supervivientes, errores de sesión y reinicios |
| Precisión | conjunto de pruebas etiquetado | tasa de detección, falsos positivos y falsos negativos |

Ejemplos de comandos (con el dispositivo conectado y depuración USB autorizada):

```bash
adb shell dumpsys meminfo com.jarvis.wakeword
adb shell top -b -n 1
adb shell dumpsys power
adb shell dumpsys batterystats
adb logcat -d -s WakeWordDetector WakeWordService OpenWakeWord
```

Para medir batería, guardar una captura inicial y otra final con la misma duración y condiciones. `dumpsys batterystats` aporta estadísticas del sistema; el porcentaje visible de batería es demasiado grueso para demostrar por sí solo pequeñas mejoras.

## Criterios para aceptar una optimización

Una modificación se acepta solo si:

- compila y supera las pruebas automatizadas;
- no empeora la tasa de detección ni incrementa falsos positivos en el conjunto de prueba;
- mejora una métrica objetivo de forma repetible frente a la línea base;
- no introduce reinicios, ANR, fugas ni interrupciones de micrófono;
- se prueba con pantalla encendida y apagada;
- se documentan firmware, APK, duración, repeticiones y datos originales.

No reducir la frecuencia de inferencia, no cambiar el umbral y no activar NNAPI/XNNPACK por intuición. ONNX Runtime indica que los proveedores de ejecución y sus resultados son específicos del modelo y dispositivo; cualquier cambio de backend exige medición y validación de precisión en el A30s.

## Mejoras candidatas, aún no demostradas

1. Instrumentar tiempos y asignaciones por etapa ONNX, sin registrar audio.
2. Evaluar buffers circulares para sustituir `ArrayList.removeAt(0)` en las ventanas de Mel/features de la dependencia, evitando desplazamientos repetidos. Requiere mantener exactamente el orden y contenido de ventanas y comparar scores.
3. Evaluar reutilización de arrays/tensores temporales solo si la API ONNX permite un ciclo de vida seguro; no reutilizar un tensor mientras una inferencia lo esté consumiendo.
4. Evitar búsquedas repetidas de campos reflectivos si se mantiene el diagnóstico basado en reflexión; preferir una API pública de estado en una futura versión de la dependencia.
5. Revisar la secuencia de parada de la dependencia para garantizar que AudioRecord se desbloquea y el hilo termina antes de cerrar OrtSession/OrtEnvironment. La solución preferida es corregir o sustituir esa dependencia con pruebas, no ocultar el timeout.
6. Mantener el WakeLock únicamente mientras la escucha activa lo necesite y verificar su liberación en todas las rutas de salida. No eliminarlo sin prueba de pantalla apagada.

## Referencias técnicas

- Android 11, servicios en primer plano y acceso al micrófono: https://developer.android.com/about/versions/11/privacy/foreground-services
- Android, bloqueos parciales excesivos: https://developer.android.com/topic/performance/issues/excessive-wakelock
- Android, bloqueos parciales atascados: https://developer.android.com/topic/performance/issues/stuck-wakelock
- ONNX Runtime, gestión de hilos: https://onnxruntime.ai/docs/performance/tune-performance/threading.html
- ONNX Runtime, despliegue móvil y selección de proveedores: https://onnxruntime.ai/docs/tutorials/mobile/
- Fuente exacta de la dependencia fijada: https://github.com/msnilsen/openwakeword-android/blob/b49f3ab14cf558ac0c2aef73a5e6c83da7a08558/openwakeword/src/main/java/com/openwakeword/OpenWakeWord.kt

## Estado de esta entrega

- Documentación de auditoría y protocolo añadida en una rama independiente.
- No se afirman mejoras medidas de CPU, RAM o batería.
- No se modificó código de ejecución porque faltan datos del A30s y cambiar el pipeline antes de disponer de una línea base podría perjudicar la detección.
- La validación física y cualquier cambio de inferencia quedan pendientes.
