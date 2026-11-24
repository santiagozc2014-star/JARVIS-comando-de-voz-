# Validación de la integración con VoiceInteractionService

## Estado

La integración compila y supera las comprobaciones automatizadas de CI. **Todavía no se ha validado en un dispositivo físico**. Esta guía define la prueba pendiente para el Samsung Galaxy A30s (Android 11 / API 30).

No instalar un APK de depuración como actualización de la instalación existente: los APK de GitHub Actions usan una firma temporal. Antes de sustituir una instalación, debe existir una firma estable y un plan de migración de datos. No desinstalar la versión que funciona para hacer esta prueba.

## Preparación

- [ ] Generar un APK release firmado con el keystore estable del proyecto.
- [ ] Comprobar que el certificado coincide con el de la instalación que se pretende actualizar.
- [ ] Si no coincide, detener la prueba de actualización y preparar una instalación paralela con otro applicationId, sin borrar la aplicación actual.
- [ ] Confirmar que ChatGPT está instalado y abre manualmente.
- [ ] Conceder a JARVIS el permiso de micrófono.
- [ ] Abrir JARVIS y comprobar que la pantalla informa del estado real del rol de asistente.

## Prueba del rol de asistente

1. En JARVIS, pulsar **Establecer JARVIS como asistente**.
2. Revisar el selector de Android y elegir JARVIS solo si el usuario desea que sea su asistente predeterminado.
3. Volver a JARVIS y comprobar que el estado indica que JARVIS es el asistente activo.
4. Si Android u One UI no ofrece el rol, registrar el resultado y probar únicamente la ruta de apertura anterior. No asumir que el rol está disponible en todos los firmwares.

## Prueba funcional

Realizar cada escenario varias veces y anotar el resultado:

| Escenario | Resultado esperado |
| --- | --- |
| Pantalla encendida, JARVIS seleccionado | La detección solicita una sesión de asistente Android; la sesión intenta abrir ChatGPT |
| Pantalla apagada, JARVIS seleccionado | Android puede diferir o bloquear la actividad; la notificación debe seguir siendo el mecanismo de recuperación |
| JARVIS no seleccionado | Se utiliza la ruta de apertura anterior |
| ChatGPT no instalado | Se abre la pantalla de JARVIS como destino alternativo |
| JARVIS detenido antes de procesar una detección pendiente | La apertura pendiente se cancela |
| Permiso de micrófono denegado | La escucha no debe iniciarse |

Para cada detección, verificar por separado:
- detección de «Hey Jarvis»;
- vibración;
- aparición de la notificación;
- resultado de la solicitud de sesión;
- apertura real de ChatGPT;
- si ChatGPT entra o no en modo voz.

Una solicitud aceptada por Android **no demuestra** que la actividad se haya mostrado. Abrir ChatGPT tampoco demuestra que se haya iniciado una conversación de voz.

## Diagnóstico

Filtrar Logcat por estas etiquetas:
- `JarvisVoiceInteraction`
- `JarvisVoiceSession`
- `VoiceInteractionBridge`
- `WakeWordService`

Mensajes importantes:
- `VoiceInteractionService listo`: el servicio de asistente recibió `onReady`.
- `Detección enviada a la sesión de asistente del sistema`: el puente envió el broadcast interno.
- `Solicitud de sesión de voz enviada al sistema`: se llamó a `showSession`; no confirma que la sesión haya abierto la aplicación.
- `Solicitud de apertura enviada desde la sesión de asistente`: se llamó a `startAssistantActivity`; no confirma que ChatGPT se mostrara.
- `JARVIS no es el asistente predeterminado`: se tomó la ruta anterior.

No desactivar las restricciones de seguridad de Android ni ocultar el indicador de micrófono para conseguir una apertura automática. Si el sistema bloquea la apertura en segundo plano, conservar la notificación como alternativa controlada por el usuario.

## Criterios de aceptación

La integración se considerará validada en el Galaxy A30s solo cuando:
1. Android permita seleccionar JARVIS como asistente predeterminado.
2. El servicio reciba `onReady` y permanezca disponible durante las pruebas.
3. Una detección real llegue al receptor y provoque la creación de la sesión.
4. La sesión solicite la apertura de ChatGPT mediante la API de asistente.
5. El resultado se compruebe visualmente en el dispositivo, con pantalla encendida y apagada por separado.
6. El fallback de notificación siga funcionando cuando la apertura automática no ocurra.

Registrar versión de Android, versión de One UI, versión de JARVIS, huella del certificado, fecha y resultados. No registrar audio, mensajes privados ni tokens.
