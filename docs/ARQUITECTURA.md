# Arquitectura de JARVIS

## Objetivo

Construir un asistente Android modular, inspirado en el comportamiento general del JARVIS cinematográfico, pero limitado a capacidades reales, permisos explícitos y hardware disponible.

## Capas

1. **Interfaz** — panel visual, conversación, estado del sistema y controles.
2. **Interacción** — voz a texto, texto a voz, activación «Hey JARVIS» y gestión de turnos.
3. **Núcleo conversacional** — contexto de sesión, instrucciones de personalidad, selección del proveedor de IA y gestión de errores.
4. **Proveedor de IA** — contrato independiente del proveedor. La primera integración objetivo es Sign in with ChatGPT para una aplicación de código abierto, si se cumplen sus requisitos y se obtiene el registro/autorización correspondiente.
5. **Memoria** — preferencias y recuerdos estructurados locales, con búsqueda, exportación y controles para corregir o borrar.
6. **Herramientas Android** — acciones mediante APIs públicas de Android, siempre con permisos y confirmación para acciones sensibles.
7. **Integraciones externas** — módulos separados para ordenador, hogar, sensores y robots compatibles.
8. **Seguridad** — almacenamiento cifrado de credenciales, permisos mínimos, registro de acciones y consentimiento explícito.

## Flujo conversacional previsto

1. El usuario activa JARVIS o abre la conversación.
2. JARVIS recibe texto o transcribe voz.
3. El núcleo prepara el contexto autorizado y consulta el proveedor de IA conectado.
4. La respuesta se muestra y, si el usuario lo permite, se lee en voz alta.
5. Los recuerdos persistentes se guardan selectivamente según la configuración del usuario.

## Límites de diseño

- No se insertan claves API en el APK ni en el repositorio.
- No se extraen cookies, sesiones ni datos privados de la aplicación oficial de ChatGPT.
- No se accede a conversaciones o recuerdos de ChatGPT mediante Sign in with ChatGPT.
- El micrófono y las cámaras deben tener indicadores visibles y controles para detenerlos.
- Las acciones sensibles requieren confirmación.
- Las funciones en segundo plano respetan las restricciones de Android y el ahorro de batería.
- Cada módulo debe poder desactivarse sin inutilizar el resto del asistente.

## Fases

- **Fase 0:** estructura Android y compilación reproducible.
- **Fase 1:** interfaz conversacional y contrato del proveedor de IA.
- **Fase 2:** autenticación y conexión real con un proveedor autorizado.
- **Fase 3:** voz bidireccional y activación por palabra clave.
- **Fase 4:** memoria local, búsqueda y exportación.
- **Fase 5:** herramientas Android con permisos y confirmaciones.
- **Fase 6:** integraciones externas y panel de supervisión.

La fase de integración con ChatGPT no se considera terminada hasta completar el flujo oficial de autorización y verificar una solicitud real de extremo a extremo.
