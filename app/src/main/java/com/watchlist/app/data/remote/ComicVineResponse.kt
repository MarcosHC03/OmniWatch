package com.watchlist.app.data.remote

// El envoltorio principal de ComicVine
data class ComicVineResponse<T>(
    val error: String,
    val limit: Int,
    val offset: Int,
    val number_of_page_results: Int,
    val number_of_total_results: Int,
    val results: T
)

// Un "Volumen" (Franquicia/Colección de cómics)
data class ComicVineVolume(
    val id: Int,
    val name: String?,
    val count_of_issues: Int?, // Total de capítulos/números
    val description: String?, // La sinopsis (Ojo: a veces viene en HTML)
    val image: ComicVineImage?,
    val publisher: ComicVinePublisher?,
    val start_year: String?
)

data class ComicVineImage(
    val medium_url: String?,
    val original_url: String?
)

data class ComicVinePublisher(
    val name: String?
)