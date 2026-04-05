# WatchList App 🎬

App Android en Kotlin + Jetpack Compose para gestionar tu lista de películas, series y anime.

## Características
- **Home**: Noticias de entretenimiento + tendencias de la semana (vía NewsAPI y TMDB)
- **Mi lista**: Tus títulos organizados en Series / Películas / Anime
  - Estado: Viendo / Visto / Por ver
  - Puntaje de 1 a 5 estrellas
  - Seguimiento de episodios y temporada (series y anime)
  - Plataforma de streaming
  - Buscador y filtros
- **Estrenos**: Próximos lanzamientos agrupados por mes

## Stack técnico
- Kotlin + Jetpack Compose (UI declarativa)
- Room (base de datos local)
- Hilt (inyección de dependencias)
- Retrofit + OkHttp (networking)
- Coil (carga de imágenes)
- Navigation Compose
- MVVM + Repository pattern

---

## ⚙️ Configuración antes de compilar

### 1. TMDB API Key (para pósters, búsqueda y estrenos)
1. Registrate en https://www.themoviedb.org/settings/api (gratis)
2. Copiá tu **API Read Access Token** (el JWT largo)
3. En `app/build.gradle.kts`, reemplazá:
```kotlin
buildConfigField("String", "TMDB_API_KEY", "\"TU_TMDB_API_KEY_AQUI\"")
```
con tu token.

### 2. NewsAPI Key (para noticias en el Home)
1. Registrate en https://newsapi.org/ (gratis, hasta 100 req/día)
2. Copiá tu API key
3. En `app/build.gradle.kts`, reemplazá:
```kotlin
buildConfigField("String", "NEWS_API_KEY", "\"TU_NEWS_API_KEY_AQUI\"")
```

> **Nota**: La app funciona sin las API keys, pero el Home no mostrará noticias
> ni tendencias, y la búsqueda TMDB no devolverá resultados.
> La lista personal funciona completamente offline con Room.

---

## 🚀 Cómo abrir el proyecto
1. Abrí Android Studio
2. File → Open → seleccioná la carpeta `WatchListApp`
3. Esperá que Gradle sincronice (~2-3 min la primera vez)
4. Configurá las API keys (ver arriba)
5. Ejecutá en emulador o dispositivo (API 26+)

---

## 📁 Estructura del proyecto
```
app/src/main/java/com/watchlist/app/
├── data/
│   ├── local/
│   │   ├── dao/          # MediaItemDao
│   │   ├── entities/     # MediaItemEntity, enums
│   │   └── WatchListDatabase.kt
│   ├── remote/           # ApiModels, TmdbApiService, NewsApiService
│   └── repository/       # MediaRepository
├── di/                   # AppModule (Hilt)
├── navigation/           # Navigation.kt, rutas y bottom nav
├── ui/
│   ├── home/             # HomeScreen.kt
│   ├── mylist/           # MyListScreen.kt
│   ├── releases/         # ReleasesScreen.kt
│   ├── addmedia/         # AddMediaScreen.kt
│   ├── theme/            # Theme.kt, Typography.kt
│   └── CommonComponents.kt
├── viewmodel/            # HomeVM, MyListVM, ReleasesVM, AddMediaVM
├── MainActivity.kt
└── WatchListApplication.kt
```
