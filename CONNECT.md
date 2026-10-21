# GlassTV Connect · evaluación experimental

Estado: investigación y separación arquitectónica. **No se incluye ni anuncia un receptor AirPlay funcional**. No se publica ningún servicio mDNS ficticio. El launcher no necesita este módulo para funcionar.

## UxPlay

Fuente examinada: FDH2/UxPlay, commit `dbb8a8f451597aa5f2e56c830ce41c4a655a8f98`, README y CMakeLists. Es la rama experimental 1.74. Incluye AirPlay Mirror y AirPlay Audio (ALAC); su documentación describe HLS opcional y ausencia de audio multiroom AirPlay 2. No equivale a un Apple TV certificado.

El build crea un ejecutable C/C++, usa OpenSSL, GStreamer y plugins de audio/vídeo. El CMake contempla Unix/macOS/Windows, detección opcional X11/DBus y descubrimiento DNS-SD externo o implementación mDNS mínima interna. No encontramos una integración Android lista para instalar. Compatibilidad ARM Linux no implica compatibilidad Android/bionic.

Para Android 9 haría falta una aplicación receptora independiente con JNI/NDK, ABI `armeabi-v7a` y las ABI realmente reportadas por la Mi Box (consultables en GlassTV), OpenSSL compatible, GStreamer para Android o reemplazo por MediaCodec y AudioTrack, gestión de Surface, sincronización A/V y red. No se debe asumir que un SoC ARM de 64 bits use un espacio de usuario arm64: hay que consultar `ro.product.cpu.abilist` antes de compilar.

## Descubrimiento y ciclo de vida

mDNS/Bonjour debe anunciar los servicios AirPlay/RAOP reales con capacidades y pairing correctos. Android 9 dispone de NsdManager; integración con la implementación de UxPlay y multicast del Wi-Fi requiere pruebas. Misma subred, aislamiento de clientes y filtrado multicast pueden impedir descubrimiento. No se presupone soporte AWDL ni descubrimiento Bluetooth de Apple.

Un receptor permanente debe vivir en un APK/módulo distinto del launcher. En Android 9 una escucha sostenida requiere un foreground service con notificación visible y activación voluntaria, nunca depender de que Home esté en primer plano. El servicio debe liberar multicast locks, códecs, sockets y audio al apagarlo; respetar audio focus y evitar conflictos con Cast.

## Licencias

UxPlay declara GPLv3; su código y componentes derivados no se pueden distribuir como MIT sin cumplir GPL. Una distribución integrada debe incluir licencia, avisos, fuente correspondiente y scripts de construcción de la versión distribuida; revisar cada dependencia, plugins de GStreamer y OpenSSL. El README advierte incompatibilidad con OpenSSL anterior a 3 sin una excepción aplicable. Separar APK facilita mantenimiento, pero no elimina obligaciones si se distribuye código derivado. No se ha copiado ni enlazado código de UxPlay en GlassTV.

Shairport Sync (https://github.com/mikebrady/shairport-sync) es otra línea de investigación para audio, no fotos, vídeo ni duplicación. Antes de reutilizar hay que fijar versión y revisar licencias de cada componente; no se lo implementa ni se afirma que compile para este dispositivo.

## Medios, rendimiento y DRM

La documentación de UxPlay describe vídeo H.264 y audio AAC en duplicación; ALAC para audio separado. Necesita verificar decodificación acelerada y no caer en decodificación software. Plan: comenzar con duplicación 720p30 sin DRM, medir uso de CPU/RAM, temperatura, fotogramas perdidos y sincronización, luego evaluar 1080p. Medir latencia extremo a extremo con cámara y cronómetro; **no hay medidas de latencia ni rendimiento obtenidas en la Mi Box**.

Fotos se pueden ver a través de duplicación cuando ésta funcione; no se garantiza un modo de envío nativo de fotos. Vídeo directo/HLS depende de la app emisora, códecs y red. UxPlay advierte que Apple video DRM no se reproduce en Mirror. No se elude FairPlay ni Widevine; contenido protegido puede dar pantalla negra o limitarse al dispositivo oficial. Una afirmación de soporte requiere pruebas por versión de iOS y app emisora.

## Decisión y alternativa utilizable

No es viable entregar responsablemente aquí un port Android probado sin NDK multimedia integrado y la Mi Box física. Se difiere la implementación del receptor; no se crea una carcasa de servicio que finja compatibilidad. El punto de integración es abrir una app receptora independiente instalada, que ya aparece en Todas las aplicaciones y puede añadirse al dock.

Alternativa: instalar un receptor AirPlay Android TV mantenido, por ejemplo AirScreen, si su versión actual admite Android 9; revisar permisos, precio y compatibilidad en su ficha antes de instalar. Probar primero contenido personal sin DRM. GlassTV no garantiza prestaciones de terceros. Para apps iPhone con icono Cast, se puede seguir usando Google Cast integrado sin instalar GlassTV Connect. Si se necesita duplicación AirPlay fiable de contenido compatible, un Apple TV por HDMI es una alternativa independiente de la Mi Box.

## Puerta de aceptación del futuro módulo

Compilar APK para las ABI reales, descubrimiento desde iPhone en la misma LAN, pairing, audio/fotos/vídeo personales, duplicación, reconexión, pausa/reanudación, cambio Wi-Fi, suspensión, 60 minutos de estabilidad, apagado completo del receptor y convivencia con Cast. Documentar versiones iOS, firmware, licencia, memoria y latencia. Hasta completar esas pruebas no se debe ofrecer "AirPlay compatible".
