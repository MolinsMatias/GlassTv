# Diseño GlassTV

Referencia principal: imagen aportada por el usuario con paisaje, reloj superior izquierdo, dock inferior y panel de control a la derecha. La implementación adapta proporciones a dp en pantallas TV 16:9 y mantiene márgenes de seguridad para overscan.

Fondo panorámico local con velo gradual de contraste, reloj sans-serif ligero de 62 sp, fecha de 17 sp en español. Dock de 112 dp con radio 28 dp, tarjetas de 96 × 90 dp, iconos reales de hasta 62 × 54 dp y etiquetas de una línea. Desplazamiento horizontal si hay más favoritos de los que caben.

Materiales: gradientes translúcidos azul grisáceo, contorno claro de 1 dp, foco de 2 dp, elevación estática y escalado al 107% durante 150 ms. No hay desenfoque del contenido detrás de cada tarjeta. El brillo es una combinación de borde y tinte, no una pasada de blur. Esta diferencia deliberada respecto a Liquid Glass protege la fluidez de la Mali-450.

El centro de control empieza cerrado. El botón superior lo abre; el panel contiene un ScrollView para evitar recortes. No hay aplicaciones recientes. Todas las acciones son enfocables y accesibles con D-pad; mantener OK o pulsar Menú sobre una app abre su editor. Atrás cierra el modal y restaura foco.

Modo claro cambia superficies y texto de los paneles; reloj y fecha permanecen claros sobre el paisaje por legibilidad. Modo reducir movimiento elimina la duración de transiciones. No se muestran clima, canción, carátula ni estados de red ficticios. Si no hay sesión multimedia o título disponible, se oculta el reproductor.

Los controles de brillo de televisor y encendido directo de radios de la referencia se sustituyen por Ajustes oficiales. Se informa del volumen fijo si el firmware delega el sonido al televisor. El fondo incluido es una fotografía de montaña y lago, no la imagen exacta de costa de la referencia; se puede importar una imagen propia.

Pendiente de calibración física: legibilidad a distancia, overscan real, calidad del foco, memoria de iconos, fps de navegación 1080p y comportamiento del mando Xiaomi.
