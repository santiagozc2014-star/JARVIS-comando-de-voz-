# Memoria estructurada de JARVIS

## Alcance de esta fase

La memoria estructurada se separa del historial de conversación. Una conversación registra mensajes; una memoria registra un dato o nota que el usuario ha decidido conservar.

## Reglas de privacidad

- No se extraen ni guardan recuerdos automáticamente.
- La capa de interfaz debe solicitar aprobación explícita antes de invocar `MemoryRepository.add`.
- El usuario puede consultar, corregir y borrar cada entrada.
- La implementación de producción de `MemoryStore` debe cifrar los datos en reposo.
- No se sincroniza ni se transmite ninguna memoria a un servidor en esta fase.

## Operaciones disponibles

- Crear una memoria con título, contenido y etiquetas.
- Editar una memoria conservando su fecha de creación.
- Eliminar una entrada o borrar todas.
- Listar las entradas guardadas.
- Buscar por palabras clave ignorando mayúsculas y acentos. Todos los términos deben coincidir.

## Lo que todavía no hace

Esta versión no realiza búsqueda semántica, embeddings, extracción automática ni conexión con ChatGPT. La búsqueda actual es local y por palabras clave. La persistencia cifrada y la interfaz de gestión se integrarán en las siguientes fases.
