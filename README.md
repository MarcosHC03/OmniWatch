<p align="center">
  <img src="assets/omniwatch-logo.png" alt="Logo de OmniWatch" width="200"/>
</p>

# OmniWatch 🎬 (v1.2)

App Android nativa en Kotlin + Jetpack Compose para gestionar tu lista de películas, series y anime de forma privada y sin depender de servicios de terceros.

## ✨ Características Principales

<h2 align="center">📱 Vistazo a la App</h2>

<p align="center">
  <img src="assets/home.jpg" alt="Pantalla de Inicio" width="30%">
  &nbsp; &nbsp;
  <img src="assets/lista.jpg" alt="Pantalla Mi Lista" width="30%">
  &nbsp; &nbsp;
  <img src="assets/calendario.jpg" alt="Pantalla Calendario" width="30%">
</p>

- **Inicio (Muro de Noticias)**: Feed híbrido de noticias de entretenimiento y cultura geek (**Cine PREMIERE, SensaCine, SomosKudasai**) con soporte de imágenes y caché offline.
- **Mi Lista**: Biblioteca personal optimizada.
  - **Títulos en Latino**: Integración con TMDB configurada para nombres de Latinoamérica (ej: *Un Show Más*).
  - **Botón +1**: Incremento rápido de episodios desde la tarjeta principal con límite de seguridad por temporada.
  - **Sincronización Inteligente**: Importación desde **MyAnimeList** con lógica anti-duplicados y detección automática de estados de emisión.
  - **Backup Local**: Exportación e importación completa en formato JSON.
- **Calendario Inteligente**:
  - *Pestaña "Viendo"*: Agenda semanal automática basada en fechas de estreno reales.
  - *Pestaña "Por Ver"*: Línea de tiempo cronológica para próximos lanzamientos y títulos ya estrenados.

## 🛠 Stack Técnico
- **UI**: Kotlin + Jetpack Compose (Material Design 3)
- **Arquitectura**: MVVM + Repository Pattern (Single Source of Truth)
- **Base de Datos**: Room (SQLite con migraciones de datos)
- **Red**: Retrofit + OkHttp (REST APIs y XML/RSS)
- **Imágenes**: Coil (Carga asíncrona de pósters)
- **Inyección de Dependencias**: Dagger Hilt
- **Navegación**: Navigation Compose

---

## 📦 Descargar e Instalar (APK)

Podés probar la aplicación directamente en tu dispositivo Android (Requiere API 26+).
1. Ve a la sección de **[Releases](../../releases)** de este repositorio.
2. Descarga el archivo `OmniWatch-v1.1.apk`.
3. Instálalo en tu dispositivo (asegúrate de tener habilitada la instalación desde orígenes desconocidos).

---

## ⚙️ Configuración para Desarrolladores (Clonar y Compilar)

### 1. Configurar TMDB API Key (Pósters y Autocompletado)
Para proteger tus credenciales, la app lee las claves desde un archivo local que no se sube a GitHub.
1. Registrate en https://www.themoviedb.org/settings/api (es gratis).
2. Copiá tu **API Read Access Token** (el JWT largo).
3. En la raíz del proyecto, abrí (o creá) el archivo `local.properties`.
4. Agregá esta línea con tu token (sin comillas ni espacios raros):
```properties
TMDB_API_KEY=tu_token_jwt_largo_aqui
```
*(Nota: La API de Jikan para anime es pública y no requiere clave).*

### 2. Abrir el proyecto
1. Cloná este repositorio.
2. Abrí Android Studio y seleccioná **File → Open** (buscá la carpeta del proyecto).
3. Esperá que Gradle sincronice las dependencias.
4. Ejecutá en el emulador o dispositivo físico.

---

## 📁 Estructura del Proyecto
```text
app/src/main/java/com/watchlist/app/
├── data/
│   ├── local/        # Dao, Entities (Room) y WatchListDatabase
│   ├── remote/       # Servicios de TMDB, Jikan y RssApiService
│   └── repository/   # MediaRepository (Single Source of Truth)
├── di/               # AppModule (Configuración de Hilt)
├── navigation/       # WatchListNavHost y Bottom Navigation
├── ui/
│   ├── home/         # Pantalla de Inicio (Noticias RSS)
│   ├── mylist/       # Pantalla de la biblioteca personal
│   ├── calendar/     # Calendario Inteligente (Viendo / Por ver)
│   ├── addmedia/     # Buscador TMDB/MAL y formulario de edición
│   ├── theme/        # Colores, Tipografía y Tema Compose
│   └── CommonComponents.kt
├── viewmodel/        # Lógica de presentación (StateFlow)
├── utils/            # Utilidades (Import/Export JSON, parseo de fechas)
├── MainActivity.kt
└── WatchListApplication.kt
```
