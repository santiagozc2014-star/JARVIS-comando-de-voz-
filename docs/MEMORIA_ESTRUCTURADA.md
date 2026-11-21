# Memoria estructurada de JARVIS

## Alcance implementado

La memoria estructurada está separada del historial de conversación. Una conversación registra mensajes; una memoria registra un dato o nota que el usuario ha decidido conservar.

La aplicación incluye una pantalla de gestión accesible desde la pantalla principal. Permite crear, listar, buscar, editar y borrar recuerdos. La búsqueda es local, por palabras clave, ignorando mayúsculas y acentos; todos los términos deben coincidir.

## Privacidad y almacenamiento

- No se extraen ni guardan recuerdos automáticamente.
- Cada recuerdo se crea únicamente después de que el usuario pulse «Guardar».
- El usuario puede consultar, corregir y borrar cada entrada.
- Los datos se cifran con AES-256-GCM.
- La clave se genera y permanece en Android Keystore; no se guarda junto al archivo cifrado.
- Las escrituras utilizan AtomicFile para reducir el riesgo de archivos parciales.
- El almacenamiento es privado de la aplicación y no se sincroniza ni transmite a un servidor.
- Las operaciones del repositorio están serializadas para evitar modificaciones concurrentes incoherentes.

## Límites actuales

Esta fase no realiza búsqueda semántica, embeddings, extracción automática, sincronización ni conexión con ChatGPT. La memoria está disponible para gestión local, pero todavía no se inyecta en el contexto de un proveedor de IA. Por tanto, guardar un recuerdo no significa que el asistente conversacional ya pueda consultarlo.

La aplicación debe probarse en un dispositivo Android real para verificar el comportamiento de Android Keystore y AtomicFile en el teléfono de destino.
