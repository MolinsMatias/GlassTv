# Arquitectura

GlassTV es una aplicación Home nativa de Android TV 9. No modifica el sistema operativo, la selección de Home automáticamente ni los servicios de casting. Identificador estable: `tv.glass.launcher`.

## Capas

* `MainActivity`: composición con FrameLayout, LinearLayout, GridLayout, ImageView y TextView; navegación espacial del framework y foco explícito entre dock y centro de control. Las superficies modales bloquean el foco del Home; Atrás lo devuelve.
* `HomeModel`: estado de aplicaciones y clima con StateFlow, coordinación de repositorios y preferencias. Es un modelo propiedad de la Activity; no necesita un ViewModel retenido para este Home pequeño. Al recrearse, recarga datos persistidos.
* `AppsRepository`: descubre actividades exportadas LEANBACK_LAUNCHER, conserva identificadores de componente, abre intents explícitos y trata desapariciones como errores recuperables. Consulta en IO; caché de hasta 48 iconos, sin almacenar todos los bitmaps de las aplicaciones.
* `Preferences`: SharedPreferences privado, compatible con API 28, con Flow para apariencia. Favoritos como arreglo JSON ordenado; `null` significa primera ejecución y lista vacía significa decisión del usuario. Los componentes temporalmente no disponibles no se borran del orden guardado.
* `WeatherRepository`: HTTPS a Open-Meteo, búsqueda manual de ciudad, timeout de 7 segundos por petición y caché de 15 minutos. Sin GPS, cuentas ni claves. La localidad realmente resuelta aparece en pantalla. Campo vacío desactiva las consultas.
* `MediaRepository`: MediaSessionManager, escucha sólo mientras Home está visible, previa autorización de acceso a notificaciones en el panel oficial. Registra y libera callbacks. La sesión seleccionada prioriza reproducción activa; no se garantiza acceso a ninguna app concreta. Sólo dibuja acciones anunciadas por la sesión. No lee contenido de notificaciones.
* `GlassApp`: anota cierres inesperados y delega al manejador estándar. Tres cierres en cinco minutos activan una pantalla simple en la próxima apertura. No inicia servicios de vigilancia ni cambia defaults. El usuario conserva acceso a Ajustes y al Home original.

## Ciclo de vida y recursos

Scope SupervisorJob en Main; consulta del catálogo y clima en Dispatchers.IO. Se cancela al destruir Activity. Reloj y callbacks multimedia se detienen al pausar; reloj a frecuencia de un minuto, alineado al cambio de minuto. No keep-screen-on, wake locks, servicios de boot ni polling en segundo plano. Android sólo vincula el NotificationListener opcional si el usuario lo habilita.

Broadcast de cambio de paquetes sólo mientras existe Activity. Las imágenes personalizadas se importan en IO con muestreo hasta 2048 px y reemplazo por archivo temporal; no se requiere permiso de almacenamiento. El fondo incluido es local. El launcher funciona sin red salvo el clima.

## Compatibilidad

minSdk y targetSdk 28 para este dispositivo dedicado, compileSdk 35 para herramientas actuales. Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9, bytecode JVM 17 y Coroutines Android 1.9.0; el plugin Android transforma bytecode para Android. CI usa JDK 17; desarrollo también admite JDK 21 con Gradle 8.9.

No se usan RenderEffect (API 31), RuntimeShader (API 33), Settings.Panel (API 29) ni RoleManager (API 29). Los ajustes de Wi-Fi/Bluetooth se abren con intents disponibles en API 28. El estado Bluetooth posterior a Android 11 puede requerir permiso adicional y se muestra como "Consultar en Ajustes"; el destino soportado es API 28. No se pide ubicación para mostrar SSID.

El target deliberadamente antiguo es para instalación directa en la Mi Box, no una declaración de cumplimiento de Google Play. No se ofrece compatibilidad general con televisores nuevos.
