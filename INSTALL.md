# Instalar GlassTV en Xiaomi Mi Box S (1.ª generación, Android TV 9)

## Instalación sin escribir código

1. En [GitHub Actions](https://github.com/MolinsMatias/GlassTv/actions/workflows/android.yml), abrí una ejecución verde de **Android APK**. Descargá **GlassTV-debug** en Artifacts y extraé `app-debug.apk`. GitHub puede pedir iniciar sesión para descargar artefactos.
2. Copiá la APK a la Mi Box con una memoria USB o una aplicación de transferencia de archivos. Abrila con un gestor de archivos y autorizá "Instalar aplicaciones desconocidas" para ese gestor si Android lo solicita.
3. Instalá y abrí **GlassTV** desde la lista de aplicaciones. Primero comprobá navegación, apertura de tus apps y acceso a Ajustes.
4. En GlassTV, ↑ → Centro de control → **Seleccionar aplicación de inicio**. Si el firmware ofrece selector, elegí GlassTV y, cuando aparezca, **Siempre**. Si no ofrece esa opción, GlassTV sigue funcionando como aplicación y el launcher de fábrica permanece disponible.

No se desactiva el Home original. Chromecast, Bluetooth, Wi-Fi, DRM y OTA siguen a cargo de Android. Una actualización de firmware puede restaurar su propio Home: volvé a seleccionar GlassTV si el sistema lo permite.

La APK es de depuración, firmada para pruebas e instalación directa; no es una publicación en Google Play. Las claves debug de máquinas/ejecuciones diferentes pueden cambiar. Si Android rechaza una actualización por firma, conservá la APK anterior o desinstalá GlassTV antes de instalar la nueva (desinstalar borra sus preferencias). Una distribución estable futura necesita una clave de firma conservada y protegida.

## Manejo con el mando Xiaomi

| Botón | Acción |
|---|---|
| Izquierda / derecha | Recorrer el dock; ajustar volumen en su deslizador |
| Arriba desde el dock | Ir al botón del centro de control |
| OK | Abrir app o activar control |
| Mantener OK sobre app | Añadir/quitar favoritos o moverlos |
| Menú sobre app, si el mando lo incluye | Abrir el mismo editor |
| Apps | Abrir todas las aplicaciones de TV |
| Atrás | Cerrar panel o diálogo y volver al Home |
| Home | Lo gestiona Android según el launcher predeterminado |

Los botones Netflix, micrófono y encendido conservan el comportamiento del firmware. No hay interceptación global ni servicio de accesibilidad. Volver a Home tras una app conserva el orden de favoritos.

## Personalización y permisos opcionales

Centro de control permite modo claro/oscuro, reloj de 12/24 h, reducir movimiento, fondo incluido/nocturno e importar una imagen local si hay selector de archivos instalado.

El clima está oculto al principio. Indicá tu ciudad y comprobá que el lugar resuelto sea correcto. No se pide ubicación. Requiere Internet y consulta Open-Meteo; ciudad vacía lo oculta. Algunos nombres ambiguos necesitan una búsqueda más específica.

Para música, elegí **Autorizar controles multimedia**, leé la explicación y habilitá GlassTV en el panel oficial de acceso a notificaciones si tu firmware lo ofrece. No es obligatorio. No todas las apps publican sesiones; sin sesión accesible el widget se oculta. El permiso puede revocarse desde ese mismo panel.

## Recuperación

En Centro de control → **Abrir launcher original** se abre uno de los Home instalados sin cambiar la preferencia. Para volver permanentemente, elegí el Home original en Ajustes de aplicaciones de inicio o borrá los valores predeterminados de GlassTV desde la ficha de la aplicación. También podés desinstalar GlassTV desde Ajustes.

Si se producen tres cierres en cinco minutos, la siguiente apertura muestra una pantalla segura con Ajustes, Home original y Reintentar. No es un watchdog capaz de corregir un fallo del proceso de Android. Si ni esa pantalla se abre, usá los Ajustes del firmware o desinstalación por ADB.

## Alternativa ADB para firmware sin selector

No hace falta para la instalación con gestor de archivos. Estos son comandos manuales opcionales; GlassTV no los ejecuta. Requieren herramientas platform-tools en una computadora de la misma red, depuración activada por el propietario y autorización en pantalla:

```sh
adb connect IP_DE_LA_MIBOX:5555
adb install -r app-debug.apk
adb shell cmd package set-home-activity tv.glass.launcher/.MainActivity
```

La referencia específica del firmware PI.3933 documenta soporte de ese comando con componente completo, pero hay que verificar la respuesta en cada firmware. Para recuperar, abrí Ajustes y elegí el Home original. Antes de cambiar el default, se puede registrar su componente con:

```sh
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Para volver por ADB, usá `cmd package set-home-activity` con ese componente registrado. No se incluye un nombre de Activity de fábrica supuesto. Desinstalar GlassTV es otra salida reversible:

```sh
adb uninstall tv.glass.launcher
```

Al terminar apagá depuración. La referencia de Mi Box S PI.3933 advierte que depuración USB puede cortar alimentación al puerto USB; evitá depender de un disco o parlante USB durante ese paso. No se necesita root ni desactivar paquetes.

## Compilar para desarrollo

JDK 17, Android SDK plataforma 35 y build-tools 35.0.0. Desde la raíz:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Salida: `app/build/outputs/apk/debug/app-debug.apk`. Las pruebas reales D-pad requieren un emulador Android TV 9 o dispositivo: `./gradlew connectedDebugAndroidTest`. CI instala herramientas y ejecuta estas tareas automáticamente; el usuario final sólo descarga la APK.
