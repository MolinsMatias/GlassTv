# Referencias técnicas

Revisión: 21 de octubre de 2026. Son referencias de diseño técnico, no dependencias ni código incorporado. No se ejecutaron sus instaladores, prompts ni scripts. Las afirmaciones sobre equipos probados por terceros no constituyen pruebas de GlassTV.

## CleanYourTV

Repositorio: https://github.com/Gathin23/cleanyourtv

Snapshot revisado: `622789f07fdc1b4b9914f900905a9065cee8c244`.

Archivos examinados: `README.md`, `LICENSE`, `launcher/app/build.gradle`, `launcher/app/src/main/AndroidManifest.xml` y `examples/xiaomi-mi-box-s/debloat.txt`.

* Licencia MIT. La fuente Outfit tiene licencia OFL separada. No copiamos código, fuentes, recursos ni listas de desactivación.
* El launcher tiene minSdk 26, compile/target 34, Java 17, RecyclerView 1.3.2 y DynamicAnimation 1.0.0. Su mínimo permite API 28; eso no prueba todas sus funciones en el firmware objetivo.
* El ejemplo específico describe MIBOX4 / MDZ-22-AB / `oneday`, Android TV 9 API 28, firmware PI.3933, 2 GB RAM, interfaz 1080p. Es una unidad india reportada por terceros; otros firmwares pueden comportarse distinto.
* Aprovechamos los patrones de categorías HOME y LEANBACK_LAUNCHER, acceso al launcher original, descubrimiento con PackageManager y una interfaz pequeña orientada a D-pad. La implementación de GlassTV es propia, en Kotlin y vistas nativas.
* El ejemplo documenta selección con `cmd package set-home-activity` usando el componente completo en Android 9. GlassTV ofrece primero ACTION_HOME_SETTINGS; INSTALL.md deja la selección por ADB como alternativa manual reversible. La aplicación no incorpora un cliente ADB.
* El ejemplo advierte que activar depuración USB puede quitar alimentación al puerto USB en ese firmware. INSTALL.md recomienda apagarla al terminar y permite instalación con gestor de archivos.
* Descartamos la lista de debloat: incluye Cast, control remoto por teléfono, actualizaciones, asistente y launcher original. Contradice los requisitos de GlassTV. Tampoco adoptamos cambios de salvapantallas, remapeo global de botones, permisos de ubicación ni servicios de accesibilidad.
* La Mi Box es una caja sin entrada HDMI ni sintonizador: no inventamos controles de entradas ni de brillo del televisor.

## Free My TV

Repositorio: https://github.com/simbasjungles/free-my-tv

Snapshot revisado: `62e883b3179c8cafb7a2b09960eabfe7eea6ef1c`.

Archivos examinados: `README.md`, `LICENSE`, `build.sh`, `app/AndroidManifest.xml`, `HomeMode.java`, `HomeApps.java`.

* Licencia MIT; sus tipografías Inter y símbolos tienen licencias separadas OFL y Apache 2.0. No se incorporaron sus recursos ni su código.
* `build.sh` fija minSdk 28 y target 33. El README describe shaders de Android 13 con superficies simples para Android 9–12. Compatible a nivel de mínimo declarado; no encontramos en estos archivos evidencia de una prueba física específica en la Mi Box S de primera generación.
* Aprovechamos conceptualmente: favoritos persistentes, consulta de apps de TV, actualización del catálogo, vuelta al Home original, clima con Open-Meteo y recuperación tras cierres repetidos.
* GlassTV usa Gradle Kotlin DSL, Coroutines/Flow y vistas enfocables reales. No adopta el pipeline manual Java, Canvas monolítico, shaders RuntimeShader, WebOS/Tizen ni interfaces WebView.
* Rechazamos `Adb.java`, `HomeMode`/`ModeJob`, desactivación de paquetes, cambios de ajustes seguros, vigilancia en segundo plano y servicios de accesibilidad. Su watchdog restaura defaults mediante ADB; el nuestro sólo muestra una pantalla de recuperación local con acceso al launcher original.
* La lista PROTECTED sirve como evidencia para identificar familias de componentes esenciales; GlassTV adopta una regla más amplia: no modifica ningún paquete del sistema.

## Servicios que deben permanecer intactos

Los nombres pueden variar entre versiones. No son una lista para ejecutar comandos:

| Función | Componentes identificados en las referencias |
|---|---|
| Google Cast integrado | `com.google.android.apps.mediashell`, Google Play Services `com.google.android.gms` y Google Services Framework `com.google.android.gsf` |
| Control remoto y emparejamiento | `com.xiaomi.android.tvsetup.partnercustomizer`, `com.google.android.tungsten.setupwraith`, `mitv.service`, familias `com.droidlogic*`; `com.google.android.tv.remote.service` para teléfono |
| Bluetooth y Wi-Fi | pila Android, servicios Amlogic `com.droidlogic*`, Settings, SystemUI, proveedores y servicios del firmware; no existe un único APK universal de Wi-Fi |
| Ajustes y recuperación | `com.android.tv.settings`, `com.android.systemui`, `android`, `com.google.android.tvlauncher` |
| Streaming y DRM | MediaCodec, MediaDrm/Widevine, bibliotecas y servicios del fabricante, apps de streaming; no se tocan |
| Actualizaciones | `com.xiaomi.mitv.updateservice`, `com.mitv.download.service`, Play Store `com.android.vending` y componentes OTA del firmware |
| MiLink y reproducción asociada | `com.mitv.milinkservice`, `com.mitv.videoplayer` |

No copiamos listas de paquetes de otros televisores para la Mi Box. No basta con proteger sólo los nombres anteriores: GlassTV no tiene ningún mecanismo de desactivación, borrado ni cambio privilegiado.

## UxPlay y otras referencias AirPlay

Se revisaron README.md y CMakeLists.txt de https://github.com/FDH2/UxPlay en `dbb8a8f451597aa5f2e56c830ce41c4a655a8f98` (rama experimental 1.74). GPLv3, GStreamer y dependencias nativas; no es un APK para Android. Detalle, límites y decisión en [CONNECT.md](CONNECT.md).

Fuentes adicionales a evaluar antes de portar: https://github.com/antimof/UxPlay (origen del proyecto) y https://github.com/mikebrady/shairport-sync (receptor de audio, no duplicación de pantalla). No se incorpora código de esos proyectos.
