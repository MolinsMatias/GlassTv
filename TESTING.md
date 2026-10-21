# Verificación

## Comprobaciones automatizadas

`testDebugUnitTest` ejecuta pruebas JUnit/Robolectric con Android API 28:

* Alta, baja, límites y reordenamiento de favoritos, lista vacía y deduplicación.
* Persistencia con nuevas instancias del repositorio y recuperación ante JSON inválido.
* Persistencia de apariencia y ciudad.
* Centro de control cerrado por defecto y cierre de superficies mediante el estado de navegación.
* Clima oculto o sin red, sin inventar datos ni realizar peticiones en esos estados.
* Aplicaciones inexistentes, actividades de TV duplicadas, listado y envío del intent de apertura.
* Creación, pausa, reanudación, recreación y destrucción de Activity sin permisos opcionales.

`connectedDebugAndroidTest` usa UiAutomator para enviar D-pad, OK y Atrás en un emulador Android TV API 28. Comprueba apertura/cierre del centro de control y de Todas las aplicaciones. El trabajo `remote-tests` en CI lo ejecuta de forma independiente; sus resultados deben revisarse aparte del artefacto de compilación.

`lintDebug` verifica uso de APIs y recursos; `assembleDebug` genera una APK firmada para instalar. Los resultados reales de esta entrega se registrarán aquí al terminar la ejecución. Una prueba escrita o configurada no equivale a una prueba superada.

## Pruebas físicas pendientes

No hay una Xiaomi Mi Box S conectada a este entorno. No se ha instalado ni probado físicamente GlassTV en el equipo del usuario. Antes de considerarlo listo para uso diario, verificar:

- [ ] Instalar, abrir desde Home original y comprobar pantalla 1080p sin recortes.
- [ ] D-pad por todo el dock, apps, scroll, panel, diálogos, teclado y selección de imágenes; OK largo con mando original.
- [ ] Añadir, quitar y mover favoritos; volver desde streaming, forzar cierre y reiniciar el dispositivo; confirmar persistencia.
- [ ] Selector Home del firmware y selección reversible; abrir original y desinstalar sin perder acceso a Ajustes.
- [ ] Wi-Fi apagado, conectado sin Internet y funcionando; Bluetooth sin desconectar el mando.
- [ ] Volumen fijo/variable y HDMI-CEC reales del equipo.
- [ ] Clima con ciudad válida, ambigua, inexistente, servidor caído y red ausente. Confirmar localidad, hora de medición y atribución.
- [ ] Música sin permiso, permiso habilitado/revocado, sesión pausada/activa, metadatos vacíos y app que no publica sesión.
- [ ] Cast desde iPhone/Android, Netflix/Prime y contenido Widevine; reproducción mientras GlassTV está pausado.
- [ ] Emparejamiento Bluetooth, micrófono, botones dedicados, suspensión y actualizaciones habituales.
- [ ] Recuperación tras tres cierres; preferencias conservadas y acceso al Home original.
- [ ] Medir frames y memoria con `adb shell dumpsys gfxinfo tv.glass.launcher` y `adb shell dumpsys meminfo tv.glass.launcher`, tras 30 minutos de navegación. No se afirma todavía 60 fps ni un consumo de RAM medido.

El emulador no reproduce la GPU Mali, DRM, Cast integrado, firmware, mando Bluetooth ni administración de energía de Xiaomi. GlassTV Connect no tiene pruebas de interoperabilidad y no se anuncia compatible con AirPlay.
