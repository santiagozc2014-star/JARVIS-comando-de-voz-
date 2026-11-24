# Auditoría visual y criterios UX de JARVIS

## Alcance
Esta iteración reorganiza las tres pantallas XML existentes: inicio, conversación y memoria. Conserva los identificadores que consumen las Activities y no modifica lógica de voz, permisos, proveedor de IA ni persistencia.

## Dirección visual
- Fondo azul petróleo oscuro, superficies elevadas discretas y acento cian reservado para identidad y acciones principales.
- Jerarquía tipográfica por secciones, etiquetas compactas y textos de ayuda secundarios.
- Paneles con bordes finos y radios consistentes; sin fondos animados ni desenfoque.
- Componentes nativos Android/Material para reducir complejidad y coste de renderizado.

## Pantalla principal
- El estado de escucha ocupa el primer panel y explica el uso del micrófono.
- Iniciar/detener escucha es el control más visible.
- Conversación y memoria se presentan como accesos directos.
- Sensibilidad, estado del asistente, batería Samsung y prueba de apertura quedan agrupados en configuración.

## Conversación
- Cabecera compacta con identidad y estado del proveedor.
- Historial cifrado y borrado quedan agrupados sin competir con el transcript.
- El transcript conserva el espacio flexible; el compositor queda anclado al borde inferior.

## Memoria
- Se explica que los recuerdos son locales y explícitos.
- Búsqueda, alta y lista mantienen un orden predecible.
- Las tarjetas generadas por MemoryActivity usan el drawable compartido de panel.

## Accesibilidad y adaptación
- Controles principales con objetivos táctiles de al menos 48dp.
- Los estados variables usan accessibilityLiveRegion=polite.
- El estado no se comunica únicamente mediante color.
- Textos en sp, dimensiones en dp y ScrollView para pantallas compactas o fuentes ampliadas.
- Sin animaciones continuas, partículas, blur ni dependencias nuevas.

## Límites conocidos
- Los estados de escucha, conversación y micrófono dependen de la lógica existente. Esta iteración mejora su presentación, pero no implementa una visualización animada ni un nuevo ciclo de voz.
- Configuración sigue integrada en la pantalla principal.
- Verificar en emuladores y Samsung Galaxy A30s: escalado de fuente, teclado abierto, orientación, contraste y tema del sistema.
