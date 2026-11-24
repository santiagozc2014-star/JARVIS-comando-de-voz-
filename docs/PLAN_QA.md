# Plan de pruebas QA de JARVIS

## Alcance y entorno de referencia

- Dispositivo objetivo: Samsung Galaxy A30s.
- Sistema de referencia: Android 11 / API 30 / One UI.
- Aplicación: `com.jarvis.wakeword`.
- Rama de referencia: `jarvis/rebuild-android-project`.
- Este plan distingue pruebas automatizadas, pruebas instrumentadas y pruebas físicas. Un resultado de compilación no sustituye a ninguna de las otras dos capas.

## Reglas de evidencia

Cada ejecución debe registrar versión de la app, commit, modelo del dispositivo, versión Android/One UI, fecha, precondiciones, pasos, resultado esperado, resultado observado y evidencia no sensible. No registrar audio, conversaciones privadas, tokens ni claves.

Estados permitidos: **PASS** (criterio cubierto con evidencia), **FAIL** (fallo reproducible), **BLOCKED** (falta entorno, permiso o servicio) y **NOT RUN**. No convertir BLOCKED o NOT RUN en PASS.

## Matriz de pruebas

| ID | Módulo | Escenario | Evidencia requerida | Criterio de aprobación |
|---|---|---|---|---|
| VOZ-01 | Wake word | 100 enunciados «Hey Jarvis» en condiciones controladas | Conteo de aciertos y fallos | Al menos 95 detecciones correctas |
| VOZ-02 | Wake word | Voz a distintas distancias y orientaciones | Resultados por distancia | Umbral acordado medido; registrar distancia y nivel de ruido |
| VOZ-03 | Wake word | Ruido ambiental moderado | Resultados por escenario | Al menos 85 % de detección en el conjunto definido |
| VOZ-04 | Wake word | 100 frases negativas y conversación ordinaria | Conteo de falsos positivos | Máximo 1 falso positivo por conjunto de 100 |
| VOZ-05 | Wake word | Reproducción de audio y voz de varias personas | Matriz de resultados | No activar de forma sistemática con frases parecidas |
| VOZ-06 | Audio | Detener escucha o revocar permiso | Estado del micrófono y del servicio | Captura detenida y recursos liberados |
| AND-01 | Asistente Android | Seleccionar y deseleccionar JARVIS como asistente | Selector del sistema y estado UI | Estado mostrado coincide con el rol efectivo |
| AND-02 | Asistente Android | Ciclo onCreate/onReady/onShutdown | Logcat sin datos privados | Sesión solo solicitada cuando el servicio está listo |
| AND-03 | Asistente Android | Activar desde wake word | Sesión visible y trazas | La solicitud llega a la sesión; no confundir solicitud con apertura |
| APP-01 | ChatGPT | App instalada y habilitada | Observación de pantalla | ChatGPT realmente visible |
| APP-02 | ChatGPT | App ausente o deshabilitada | Observación de pantalla | Fallback seguro y mensaje comprensible |
| APP-03 | ChatGPT | Restricción de inicio desde segundo plano | Resultado con pantalla encendida/apagada | Notificación recuperable; no afirmar apertura sin comprobarla |
| IA-01 | IA | Proveedor no configurado | Resultado del repositorio | Respuesta Unavailable; no se simula éxito ni se almacena un turno fallido |
| IA-02 | IA | Proveedor autorizado real | ID/estado de petición sin contenido privado | Respuesta real, autenticada y atribuible al proveedor |
| IA-03 | IA | Mensaje vacío, timeout, HTTP 4xx/5xx y sin red | Resultado y estado del historial | Error controlado, sin bloqueo ni turno falso |
| IA-04 | IA | Peticiones consecutivas y cancelación | Orden de mensajes y cancelación | Sin duplicados, corrupción ni respuestas tardías asociadas a otro turno |
| MEM-01 | Memoria | Crear, buscar, editar y borrar | Estado antes/después | Operaciones correctas y persistentes |
| MEM-02 | Memoria | Cerrar y reabrir app/dispositivo | Datos recuperados | Datos válidos recuperados sin duplicados |
| MEM-03 | Memoria | Archivo cifrado truncado o alterado | Excepción/estado controlado | No devolver datos corruptos como válidos |
| MEM-04 | Memoria | Pérdida/invalidez de clave Keystore | Resultado de recuperación | Fallo explícito y recuperación segura; nunca degradar a texto plano |
| MEM-05 | Memoria | Desactivar persistencia y borrar | Almacenamiento y UI | Copia persistida eliminada y preferencia coherente |
| TTS-01 | TTS | Inicializar motor español | Callback de inicialización | Motor listo o error recuperable |
| TTS-02 | TTS | 20 respuestas consecutivas | Finalización de utterance | 20/20 completadas, sin bloqueos ni duplicados |
| TTS-03 | TTS | Interrumpir, cambiar volumen y usar auriculares | Estado de audio | Interrupción y foco de audio gestionados correctamente |
| BG-01 | Segundo plano | Pantalla apagada 30 minutos | Notificación, servicio y consumo | Comportamiento estable; restricciones Samsung documentadas |
| BG-02 | Segundo plano | Doze/ahorro de batería | Estado del servicio | Recuperación o degradación comunicada, sin prometer escucha continua |
| NET-01 | Red | Desconexión, cambio Wi-Fi/datos y reconexión | Logs sin secretos y resultado | Error visible y nueva petición posible al recuperar red |
| SYS-01 | Reinicio | Reinicio de teléfono y proceso | Estado del servicio y preferencias | Sin servicios duplicados ni escucha no consentida |
| SYS-02 | Reinicio | Interrupción durante escritura | Datos tras reinicio | Archivo anterior recuperable o error explícito |
| PERM-01 | Permisos | Micrófono denegado, concedido y revocado | Diálogo y estado UI | No escuchar sin permiso; guía de recuperación |
| SEC-01 | Seguridad | Inspección de APK, manifest y logs | Revisión estática | Sin claves/tokens; componentes exportados mínimos |
| SEC-02 | Seguridad | Broadcast de activación | Prueba de emisor externo | Receptor no exportado o protegido; sin activación externa no autorizada |
| SEC-03 | Seguridad | Almacenamiento local | Inspección del sandbox | Datos sensibles cifrados y claves en Android Keystore |
| PERF-01 | Rendimiento | Inicio y latencia de activación | Tiempos repetidos | Inicio < 3 s y latencia wake-word a solicitud P95 < 1,5 s |
| PERF-02 | Rendimiento | Escucha de 30 minutos y 8 horas | CPU, RAM, batería, temperatura | Sin fuga creciente, bloqueo ni sobrecalentamiento; umbrales acordados con mediciones base |

Los porcentajes y tiempos son objetivos iniciales de aceptación, no resultados ya medidos. Para ensayos acústicos se debe fijar previamente el protocolo, volumen, distancia, idioma y ruido; publicar también el tamaño de muestra.

## Niveles de ejecución

### 1. Unitarias (CI)

Comando principal:

```bash
gradle :app:testDebugUnitTest
```

Cubrir políticas puras, estados del runtime, repositorios, normalización, errores y contratos de proveedores. La CI debe guardar los informes JUnit y fallar ante cualquier test fallido.

### 2. Estáticas y compilación (CI)

```bash
gradle :app:lintDebug
gradle :app:assembleDebug
```

La firma del APK y su checksum se verifican por separado. Un APK debug firmado temporalmente no es una actualización compatible garantizada.

### 3. Instrumentadas (emulador/dispositivo)

Añadir pruebas Android instrumentadas para permisos, ciclo de vida de servicios, notificaciones, almacenamiento Android Keystore y componentes del sistema. Ejecutar API 30 como referencia y otras API solo si se amplía oficialmente el soporte.

### 4. Físicas (Galaxy A30s)

La detección acústica, el rol de asistente, la pantalla apagada, las restricciones One UI, batería y apertura real de ChatGPT requieren el teléfono físico. Registrar manualmente los resultados; no inferirlos desde CI.

## Criterios de bloqueo

Rechazar la versión si se presenta cualquiera de estas condiciones:

- Crash, ANR, pérdida/corrupción de memoria o escucha sin permiso.
- Exposición de credenciales, tokens, audio o datos privados.
- Falsa afirmación de que ChatGPT/IA está conectado o que una app se abrió sin comprobarlo.
- Fallo de cualquier prueba crítica de permisos, cifrado, activación o recuperación.
- Prueba crítica marcada BLOCKED o NOT RUN en una versión que se declara lista para uso.
- Diferencia de firma no resuelta cuando se pretende actualizar una instalación existente.

## Estado inicial conocido

- CI ha ejecutado Lint, pruebas unitarias, compilación debug y verificación de firma en una ejecución verde.
- La firma release se omite si no están configurados los secretos de firma.
- El proveedor de la pantalla de chat es `NotConfiguredAiProvider`; no realiza una petición real de IA.
- La integración física de VoiceInteractionService y wake word sigue requiriendo validación en el Galaxy A30s.
- No hay evidencia automatizada suficiente para TTS, pruebas acústicas, Doze, reinicios físicos ni mediciones de consumo.

Actualizar esta sección solo con resultados recientes y trazables.
