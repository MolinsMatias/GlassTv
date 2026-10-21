# GlassTV

Launcher nativo para **Xiaomi Mi Box S de primera generación · Android TV 9 / API 28**. Fondo panorámico, dock flotante con iconos reales, navegación D-pad, favoritos persistentes y centro de control. Inspirado en tvOS y materiales Liquid Glass, con efectos ligeros para la GPU del dispositivo.

[Descargar compilaciones de GitHub Actions](https://github.com/MolinsMatias/GlassTv/actions/workflows/android.yml) · [Instalar](INSTALL.md) · [Pruebas y estado](TESTING.md)

## Funciones

* Descubre aplicaciones instaladas para Android TV y las abre con el mando.
* Añade, quita y reordena favoritos manteniendo OK sobre una app. Conserva el orden al reiniciar.
* Panel de control colapsado al iniciar, Wi-Fi y Bluetooth con acceso a Ajustes oficiales, volumen multimedia cuando el dispositivo lo permite e información del equipo.
* Reloj local, fecha en español, 12/24 horas, modo claro/oscuro, movimiento reducido y fondo personalizable.
* Clima opcional por ciudad manual, con datos Open-Meteo y estados de carga, sin red y error. Sin ubicación obligatoria.
* Widget de sesiones multimedia accesibles, con autorización opcional en Android. Se oculta si no existe una sesión disponible.
* Registro como Home, salida al launcher original y pantalla de recuperación tras cierres repetidos.

Sin WebView, backend, cuentas, anuncios ni aplicaciones recientes. No modifica paquetes del sistema: Cast, mando, Bluetooth, Wi-Fi, DRM y actualizaciones permanecen bajo control del firmware. La selección como Home depende de lo que permita ese firmware.

## Documentación

| Documento | Contenido |
|---|---|
| [INSTALL.md](INSTALL.md) | Instalación mínima, mando y recuperación |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Capas, dependencias y APIs de Android 9 |
| [DESIGN.md](DESIGN.md) | Sistema visual y decisiones de rendimiento |
| [REFERENCES.md](REFERENCES.md) | Análisis de CleanYourTV y Free My TV, licencias y servicios esenciales |
| [CONNECT.md](CONNECT.md) | Investigación AirPlay/UxPlay y alternativa independiente |
| [TESTING.md](TESTING.md) | Validaciones realizadas y pendientes en hardware |
| [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) | Recursos, atribuciones y dependencias |

**GlassTV Connect no está implementado como receptor AirPlay.** La investigación no encontró un port de UxPlay listo para este equipo. El launcher funciona de forma independiente; el documento explica el trabajo nativo necesario y alternativas.

## Desarrollo

Kotlin 2.0.21 · Gradle Kotlin DSL 8.9 · AGP 8.7.3 · SDK 35 · min/target 28 · Coroutines/Flow · vistas Android nativas · SharedPreferences.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. CI publica `GlassTV-debug` y reportes de verificación. Ver TESTING.md antes de interpretar una compilación como una prueba física.
