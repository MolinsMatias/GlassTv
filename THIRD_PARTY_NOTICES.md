# Recursos y dependencias

GlassTV contiene código propio. Las referencias CleanYourTV y Free My TV (MIT) se estudiaron sin copiar código ni recursos; ver REFERENCES.md. No se incluye UxPlay.

* Kotlin y Kotlin Coroutines: Apache License 2.0, https://github.com/JetBrains/kotlin y https://github.com/Kotlin/kotlinx.coroutines . Dependencias empaquetadas por Gradle según sus metadatos.
* Android SDK/framework y herramientas: licencias de Android/Google aplicables; no se empaqueta el SDK en la app. AndroidX, JUnit y Robolectric se usan para pruebas según sus licencias, no como recursos visuales del producto.
* Fotografía incluida `app/src/main/res/drawable-nodpi/landscape.jpg`: imagen publicada en Unsplash, archivo fuente `https://images.unsplash.com/photo-1470770841072-f978cf4d019e`. Uso bajo la licencia Unsplash: https://unsplash.com/license . Se incluye como fondo de una app, no como servicio de redistribución de fotografías. Crédito a la comunidad/autor de Unsplash; no se atribuye un nombre sin verificar. El usuario puede sustituirla por una imagen propia.
* Clima y geocodificación: Open-Meteo, https://open-meteo.com/ . Datos bajo CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ . Atribución visible en el widget. El servicio público tiene límites y condiciones de uso; el uso comercial debe revisar su plan vigente. No hay clave ni conexión obligatoria.
* Iconos y nombres de aplicaciones: se leen de las aplicaciones instaladas; pertenecen a sus respectivos titulares y no se incluyen logos de streaming en el repositorio.
* Tipografía: sans-serif del sistema Android; no se distribuyen tipografías de Apple, Outfit ni Inter.

No existe afiliación con Apple, Google, Xiaomi, Open-Meteo ni proveedores de streaming.
