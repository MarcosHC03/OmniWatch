<p align="center">
  <img src="assets/omniwatch-logo.png" alt="Logo de OmniWatch" width="200"/>
</p>

# 👁️ OmniWatch - Track Your Media

OmniWatch es una aplicación nativa de Android diseñada para centralizar y organizar el seguimiento de Películas, Series, Animes, **Cómics y Mangas**. Desarrollada con un enfoque "Offline-First", permite a los usuarios gestionar sus listas personales, descubrir nuevos estrenos y llevar un calendario preciso de lanzamientos sin depender de una conexión constante a internet.

## ✨ Características Principales

<h2 align="center">📱 Vistazo a la App</h2>

<p align="center">
  <img src="assets/home.jpg" alt="Pantalla de Inicio" width="20%">
  &nbsp; &nbsp;
  <img src="assets/discovery.jpg" alt="Pantalla Descubrí" width="20%">
  &nbsp; &nbsp;
  <img src="assets/lista.jpg" alt="Pantalla Mi Lista" width="20%">
  &nbsp; &nbsp;
  <img src="assets/calendario.jpg" alt="Pantalla Calendario" width="20%">
</p>

- **Gestión de Medios:** Organiza tu contenido en listas de estado ("Por ver", "Viendo/Leyendo", "Visto/Leído").
- **Calendario Inteligente:** Visualiza los próximos episodios y estrenos ordenados por fecha y día de la semana.
- **Soporte Multi-Plataforma y Autor:** Etiqueta en qué servicio de streaming estás viendo cada título o asigna los autores y editoriales a tus medios físicos.
- **Modo Oscuro Nativo:** Interfaz diseñada con Material Design 3, optimizada para la fatiga visual y con soporte de íconos temáticos monocromáticos (Android 13+).

### 🚀 Novedades de la Versión 2.1 (Print Media and UI/UX Update)

- **Biblioteca de Impresos:** Soporte dedicado y separado para el seguimiento de Cómics, Mangas, Manhwas y Novelas Gráficas.
- **Alternador de Modos (Toggle):** Pestañas dinámicas que permiten alternar instantáneamente entre la colección Audiovisual y la Impresa para mantener una interfaz limpia.
- **Búsqueda Dual de APIs:** Descubrimiento automatizado utilizando la API de Jikan para obras orientales (Mangas) y la API de ComicVine para cómics occidentales.
- **Sincronización Total con MyAnimeList:** Importación automática tanto del progreso de Animes como de Mangas directamente desde tu cuenta de MAL, con enriquecimiento de datos On-Demand (Lazy Loading de autores).
- **Base de Datos Relacional:** Nueva arquitectura que separa el Progreso Global (la franquicia y capítulos totales) del Archivo Local (los tomos y páginas) para una escalabilidad perfecta hacia la lectura local.
- **Barra de Progreso Inteligente:** Calcula el avance automáticamente adaptándose a la lectura por capítulos o números (issues) en lugar de solo por tomos físicos.
- **Súper Backup JSON (Sistema Anidado):** Exportación e importación de la base de datos completa con soporte para la nueva estructura compleja (Franquicias y Tomos), manteniendo 100% de retrocompatibilidad con los backups de la v1.x.
- **Máquina de Estados Sincronizada:** El contador de episodios ahora controla el estado de reproducción de forma automatizada. Si una serie marcada como "Visto" recibe nuevos capítulos (ej. estrenos de temporada), la tarjeta detecta la desincronización y reactiva el botón `+` para devolverla automáticamente a "Viendo" al interactuar.
- **Inicio Rápido desde "Por Ver":** Al presionar el botón `+` en un elemento pendiente, se despliega un diálogo inteligente que permite inicializar la serie de forma express (`Temporada 1 - 1/?`) o saltar directamente a la pantalla de edición detallada para configurar los datos a mano.
- **El Sabueso Silencioso (Background Sync):** Implementación de un buscador asíncrono *Offline-First* conectado en segundo plano con TMDB y Jikan. Al sumar un episodio o iniciar una serie, el sistema valida de forma silenciosa el total oficial de capítulos por ID, actualizando el tope real en la base de datos sin interrumpir al usuario y blindado con escudos ante caídas de red.
- **Click Largo Avanzado (Control Gestual):** Manteniendo presionado el botón `+` se activa una micro-interacción premium que despliega un panel numérico enfocado. El usuario puede ingresar directamente el capítulo exacto en el que se quedó, ideal para registrar maratones de corrido y evitar llamadas repetitivas a las APIs.
- **Empty States Interactivos:** Rediseño total de las pantallas vacías tanto en el modo Audiovisual como en Impresos. Se eliminaron los callejones sin salida; ahora, según la pestaña activa (Viendo, Visto, Por Ver), el sistema presenta botones contextuales inteligentes en vertical para saltar a solapas pendientes, añadir contenido local o redirigir directamente al Centro de Descubrimiento.

---

## 🛠️ Stack Tecnológico y Arquitectura

El proyecto sigue los lineamientos recomendados por Google (Modern Android Development):

- **UI:** Jetpack Compose (100% declarativo) + Material Design 3.
- **Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture principles.
- **Concurrencia y Estado:** Kotlin Coroutines & StateFlow.
- **Base de Datos Local:** Room Database (Offline-first approach con relaciones 1 a muchos).
- **Red:** Retrofit2 + OkHttp (Consumo de APIs REST: TMDB, Jikan, MyAnimeList Oficial, ComicVine).
- **Inyección de Dependencias:** Dagger Hilt.
- **Imágenes:** Coil (Carga asíncrona y caché de imágenes).

---

## 📂 Estructura del Proyecto

La aplicación está modularizada por capas (features) dentro del paquete `com.watchlist.app` para asegurar una clara separación de responsabilidades.

```text
📦 app/src/main/java/com/watchlist/app
 ┣ 📂 backup          # Lógica y modelos para exportar/importar JSON anidado (AppBackup)
 ┣ 📂 data
 ┃ ┣ 📂 local         # Entidades de Room (Audiovisuales, Impresos, Cachés), DAOs y Database
 ┃ ┣ 📂 remote        # Servicios de Retrofit (TMDB, Jikan, ComicVine, MAL Oficial) y DTOs
 ┃ ┗ 📂 repository    # Única fuente de verdad (Single Source of Truth)
 ┣ 📂 di              # Módulos de Inyección de Dependencias (Hilt)
 ┣ 📂 navigation      # NavHost, Rutas y Configuración de Jetpack Navigation
 ┣ 📂 ui              # Pantallas (Screens) y componentes compartidos de UI
 ┃ ┣ 📂 addmedia      # Pantalla para agregar Películas, Series y Anime
 ┃ ┣ 📂 addprintmedia # Pantalla dedicada para agregar Cómics y Mangas
 ┃ ┣ 📂 calendar      # Calendario de estrenos y seguimiento semanal
 ┃ ┣ 📂 discovery     # Descubrimiento de tendencias (Audiovisual e Impresos)
 ┃ ┣ 📂 home          # Pantalla principal (Inicio)
 ┃ ┣ 📂 mylist        # Biblioteca unificada con toggle (Audiovisual/Impresos)
 ┃ ┣ 📂 printdetails  # Perfil de franquicia impresa y gestión individual de tomos
 ┃ ┗ 📂 theme         # Colores, Tipografías y Formas (Material 3)
 ┣ 📂 utils           # Clases de ayuda y formateadores (Ej: Fechas)
 ┗ 📂 viewmodel       # Lógica de presentación centralizada (StateFlow)
```

---

## 📦 Descargar e Instalar (APK)

Podés probar la aplicación directamente en tu dispositivo Android (Requiere API 26+).

1. Ve a la sección de **Releases** de este repositorio.
2. Descarga el archivo `OmniWatch-v2.0.apk`.
3. Instálalo en tu dispositivo (asegúrate de tener habilitada la instalación desde orígenes desconocidos).

---

## ⚙️ Configuración para Desarrolladores (Clonar y Compilar)

### 1. Configurar TMDB API Key (Pósters y Autocompletado)

Para proteger tus credenciales y evitar baneos, la app lee las claves y tu correo de contacto desde un archivo local que no se sube a GitHub.

1. Registrate en [TMDB](https://www.themoviedb.org/settings/api) para obtener la clave de películas/series y en [ComicVine](https://comicvine.gamespot.com/api/) para la clave de cómics occidentales.
2. En la raíz del proyecto, abrí (o creá) el archivo `local.properties`.
3. Agregá estas líneas con tus tokens y tu correo (sin comillas ni espacios raros):

```properties
TMDB_API_KEY=tu_token_jwt_largo_aqui
CV_API_KEY=tu_clave_comicvine_aqui
CV_EMAIL=tu_correo_de_contacto_aqui
```

_(Nota: Las APIs de Jikan para anime/manga son públicas y no requieren clave)._

### 2. Abrir el proyecto

1. Cloná este repositorio.
2. Abrí Android Studio y seleccioná **File → Open** (buscá la carpeta del proyecto).
3. Esperá que Gradle sincronice las dependencias.
4. Ejecutá en el emulador o dispositivo físico.
