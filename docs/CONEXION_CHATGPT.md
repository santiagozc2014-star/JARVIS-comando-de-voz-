# Conexión de JARVIS con ChatGPT

## Ruta elegida

Evaluar **Sign in with ChatGPT para aplicaciones de código abierto (OSS)**. Esta opción permite que una aplicación compatible solicite, con consentimiento, el uso de ChatGPT plan usage para solicitudes elegibles de Responses API.

Documentación oficial:
- https://developers.openai.com/siwc/
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source
- https://openai.com/policies/sign-in-with-chatgpt-terms/

## Qué permite y qué no

- Puede permitir que una aplicación OSS elegible envíe solicitudes Responses API usando el uso incluido en un plan ChatGPT compatible, después de que el usuario autorice expresamente el permiso correspondiente.
- La guía oficial indica que el uso del plan está disponible para usuarios elegibles de ChatGPT Plus y Pro; no se debe asumir que una cuenta Free puede usar esta modalidad. Hay que comprobar los requisitos vigentes antes de diseñar la conexión final.
- La disponibilidad, elegibilidad, límites y registro de la aplicación dependen de los requisitos oficiales vigentes.
- Las solicitudes deben ejecutarse en el entorno local del usuario (o un entorno remoto bajo su control), corresponder a su propia actividad y no convertirse en un servicio API general para terceros.
- Antes de cualquier uso en segundo plano se necesita consentimiento expreso; el usuario debe poder desconectar la cuenta y controlar el uso.
- No concede acceso al historial de conversaciones, memoria, archivos ni datos privados de ChatGPT.
- La autorización de identidad y la autorización para usar el plan son permisos distintos.
- No proporciona una clave API reutilizable para otros servicios.

## Requisitos previos pendientes

1. Revisar los términos y la documentación vigentes con un adulto responsable antes de registrar o autorizar una aplicación.
2. Confirmar que el proyecto cumple los requisitos de aplicación OSS y solicitar/configurar el identificador de cliente oficial que corresponda.
3. Definir el callback OAuth compatible con Android.
4. Implementar Authorization Code + PKCE, state y validación de la respuesta.
5. Solicitar solo los scopes necesarios y comprobar que el permiso de uso del plan fue concedido.
6. Guardar tokens de forma segura en el dispositivo; nunca registrarlos en logs ni subirlos a GitHub.
7. Implementar renovación, revocación, cierre de sesión y errores de cuota/consentimiento.
8. Realizar una solicitud Responses API de prueba y verificar la respuesta real.

## Alternativa API convencional

La API de OpenAI requiere un proyecto y credenciales API independientes. No debe llamarse directamente desde el APK con una clave incrustada. Si algún día se elige esa vía, debe existir un backend controlado que proteja la credencial y se debe revisar antes cualquier coste y límite de uso.

## Estado

**No conectado todavía.** El proyecto Android compila, pero no se ha registrado un cliente OAuth, no se ha autorizado ninguna cuenta y no se ha realizado ninguna solicitud real de IA. No introducir credenciales hasta completar los requisitos anteriores.
