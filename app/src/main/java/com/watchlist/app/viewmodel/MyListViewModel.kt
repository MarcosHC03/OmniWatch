package com.watchlist.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
//import com.google.gson.JsonParser
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.data.repository.MediaRepository
import com.watchlist.app.data.backup.AppBackup
import com.watchlist.app.BuildConfig
import com.watchlist.app.utils.AuthUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Intent
enum class ListMode { AUDIOVISUAL, PRINTED }

data class MyListUiState(
    val listMode: ListMode = ListMode.AUDIOVISUAL, // <-- *NUEVO* Controla el Toggle
    
    // Listas y selecciones AUDIOVISUALES
    val items: List<MediaItemEntity> = emptyList(),
    val selectedTab: MediaType = MediaType.SERIES,
    
    // Listas y selecciones IMPRESAS
    val printItems: List<PrintMediaEntity> = emptyList(),
    val selectedPrintTab: PrintType = PrintType.COMIC,
    
    val filterStatus: WatchStatus? = null,
    val searchQuery: String = "",

    // MAL
    val isImporting: Boolean = false,
    val importSuccessMessage: String? = null,
    val importErrorMessage: String? = null,

    // Backup
    val isBackupProcessing: Boolean = false,
    val backupSuccessMessage: String? = null,
    val backupErrorMessage: String? = null
)

// Estado interno compacto para evitar el límite de 5 args en combine()
private data class InternalState(
    val mode: ListMode = ListMode.AUDIOVISUAL,
    val tab: MediaType = MediaType.SERIES,
    val printTab: PrintType = PrintType.COMIC,
    val status: WatchStatus? = null,
    val query: String = "",
    val isImporting: Boolean = false,
    val importSuccess: String? = null,
    val importError: String? = null,
    val isBackupProcessing: Boolean = false,
    val backupSuccess: String? = null,
    val backupError: String? = null
)

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val _state = MutableStateFlow(InternalState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MyListUiState> = _state
        .flatMapLatest { s ->
            if (s.mode == ListMode.AUDIOVISUAL) {
                val itemsFlow = if (s.query.isBlank()) {
                    if (s.status != null)
                        repository.getItemsByType(s.tab).map { list -> list.filter { it.watchStatus == s.status } }
                    else
                        repository.getItemsByType(s.tab)
                } else {
                    repository.searchItems(s.query).map { list -> list.filter { it.mediaType == s.tab } }
                }
                itemsFlow.map { items ->
                    MyListUiState(
                        listMode = s.mode,
                        items = items,
                        selectedTab = s.tab,
                        filterStatus = s.status,
                        searchQuery = s.query,
                        isImporting = s.isImporting,
                        importSuccessMessage = s.importSuccess,
                        importErrorMessage = s.importError,
                        isBackupProcessing = s.isBackupProcessing,
                        backupSuccessMessage = s.backupSuccess,
                        backupErrorMessage = s.backupError
                    )
                }
            } else {
                // --- NUEVA LÓGICA PARA CÓMICS Y MANGAS ---
                repository.getPrintItemsByType(s.printTab).map { items ->
                    
                    // 1. Filtramos por texto (Si el usuario escribió algo)
                    val searchedItems = if (s.query.isBlank()) {
                        items
                    } else {
                        items.filter { it.title.contains(s.query, ignoreCase = true) }
                    }

                    // 2. Filtramos por estado (Leyendo, Leído, etc.)
                    val mappedStatus = when (s.status) {
                        WatchStatus.WATCHING -> ReadStatus.READING
                        WatchStatus.COMPLETED -> ReadStatus.COMPLETED
                        WatchStatus.PLANNED -> ReadStatus.PLANNED
                        null -> null
                    }
                    
                    val filteredItems = if (mappedStatus != null) {
                        searchedItems.filter { it.status == mappedStatus }
                    } else {
                        searchedItems
                    }

                    MyListUiState(
                        listMode = s.mode,
                        printItems = filteredItems,
                        selectedPrintTab = s.printTab,
                        filterStatus = s.status,
                        searchQuery = s.query,
                        isImporting = s.isImporting,
                        importSuccessMessage = s.importSuccess,
                        importErrorMessage = s.importError,
                        isBackupProcessing = s.isBackupProcessing,
                        backupSuccessMessage = s.backupSuccess,
                        backupErrorMessage = s.backupError
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState())

    // ---- Tabs / filtros / búsqueda ----

    fun selectTab(type: MediaType) { _state.update { it.copy(tab = type) } }
    fun toggleListMode() { _state.update { it.copy(mode = if (it.mode == ListMode.AUDIOVISUAL) ListMode.PRINTED else ListMode.AUDIOVISUAL) } }
    fun selectPrintTab(type: PrintType) { _state.update { it.copy(printTab = type) } }
    fun setFilter(status: WatchStatus?) { _state.update { it.copy(status = status) } }
    fun setSearch(query: String) { _state.update { it.copy(query = query) } }

    fun deleteItem(item: MediaItemEntity) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun updateStatus(item: MediaItemEntity, status: WatchStatus) {
        viewModelScope.launch { repository.updateItem(item.copy(watchStatus = status)) }
    }

    // ---- Importación desde MyAnimeList ----
    // ---- Autenticación Oficial MAL (OAuth2) ----
    fun startMalLogin() {
        // 1. Generamos y guardamos la contraseña temporal
        val verifier = AuthUtils.generateAndSaveVerifier(context)
        val clientId = BuildConfig.MAL_CLIENT_ID

        // 2. Armamos la URL oficial que exige MyAnimeList
        val url = "https://myanimelist.net/v1/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=$clientId" +
                "&code_challenge=$verifier" +
                "&code_challenge_method=plain"

        // 3. Abrimos el navegador
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    init {
        // Nos quedamos escuchando el tubo secreto
        viewModelScope.launch {
            AuthUtils.authCodeFlow.collect { authCode ->
                processMalAuthCode(authCode)
            }
        }
    }

    private fun processMalAuthCode(authCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, importSuccess = null, importError = null) }
            
            val verifier = AuthUtils.getSavedVerifier(context)
            if (verifier == null) {
                _state.update { it.copy(isImporting = false, importError = "Error de seguridad local") }
                return@launch
            }

            try {
                val clientId = BuildConfig.MAL_CLIENT_ID
                // 1. Canjeamos el ticket por el Pase VIP
                val response = repository.exchangeMalCodeForToken(clientId, authCode, verifier)

                if (response != null) {
                    AuthUtils.saveAccessToken(context, response.accessToken)
                        
                    // 2. ¡NUEVO! Con el Pase VIP en mano, descargamos los animes
                    repository.syncOfficialMalList(response.accessToken)
                    repository.syncOfficialMalMangaList(response.accessToken)
                    
                    _state.update { it.copy(
                        isImporting = false, 
                        //tab = MediaType.ANIME, // Cambiamos a la pestaña Anime automáticamente
                        importSuccess = "¡Lista importada con éxito desde MyAnimeList!"
                    ) }
                } else {
                    _state.update { it.copy(isImporting = false, importError = "No se pudo validar el código con MAL") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isImporting = false, importError = "Error descargando la lista: ${e.message}") }
            }
        }
    }

    // ---- Backup: Exportar a JSON (v2) ----

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isBackupProcessing = true, backupSuccess = null, backupError = null) }
            try {
                // buildBackup() reemplaza a getAllItemsOnce(): ahora incluye impresos con sus tomos
                val backup = repository.buildBackup()

                if (backup.audiovisuales.isEmpty() && backup.impresos.isEmpty()) {
                    _state.update { it.copy(
                        isBackupProcessing = false,
                        backupError = "No hay elementos para exportar"
                    ) }
                    return@launch
                }

                val json = gson.toJson(backup)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw Exception("No se pudo abrir el archivo de destino")

                val totalAv = backup.audiovisuales.size
                val totalImp = backup.impresos.size
                val mensaje = buildString {
                    append("✓ Backup exportado: ")
                    if (totalAv > 0) append("$totalAv audiovisual${if (totalAv != 1) "es" else ""}")
                    if (totalAv > 0 && totalImp > 0) append(", ")
                    if (totalImp > 0) append("$totalImp impreso${if (totalImp != 1) "s" else ""}")
                }
                _state.update { it.copy(isBackupProcessing = false, backupSuccess = mensaje) }

            } catch (e: Exception) {
                _state.update { it.copy(
                    isBackupProcessing = false,
                    backupError = "Error al exportar: ${e.message ?: "error desconocido"}"
                ) }
            }
        }
    }

    // ---- Backup: Importar desde JSON (v2 + retrocompatibilidad v1) ----

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isBackupProcessing = true, backupSuccess = null, backupError = null) }
            try {
                // 1. Leemos el archivo en crudo
                val rawJson = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?.trim()
                    ?: throw Exception("No se pudo leer el archivo")

                if (rawJson.isBlank()) throw Exception("El archivo está vacío")

                var jsonToParse = rawJson

                // 2. LA MAGIA QUE DESCUBRISTE (Limpieza a lo bruto para v1.X)
                if (jsonToParse.startsWith("[") && jsonToParse.length > 2) {
                    // Borramos TODA llave, corchete o espacio que haya quedado colgando al final.
                    // Esto deja el texto cortado justo después del último dato (ej: [ {...}, {... )
                    val podado = jsonToParse.trimEnd(' ', '\n', '\r', '}', ']')
                    
                    // Y lo cerramos nosotros mismos a la fuerza de forma perfecta
                    jsonToParse = "$podado}]"
                }

                var backup: AppBackup? = null

                // 3. Ahora sí, se lo pasamos a Gson ya masticado y limpio
                try {
                    val element = com.google.gson.JsonParser.parseString(jsonToParse)
                    
                    if (element.isJsonObject) {
                        val parsed = gson.fromJson(element, AppBackup::class.java)
                        backup = AppBackup(
                            version = parsed.version,
                            audiovisuales = parsed.audiovisuales ?: emptyList(),
                            impresos = parsed.impresos ?: emptyList()
                        )
                    } else if (element.isJsonArray) {
                        val items = gson.fromJson(element, Array<MediaItemEntity>::class.java)
                        backup = AppBackup(version = 1, audiovisuales = items.toList(), impresos = emptyList())
                    }
                } catch (e: Exception) {
                    throw Exception("Sintaxis JSON rota: ${e.message}")
                }

                if (backup == null || (backup.audiovisuales.isEmpty() && backup.impresos.isEmpty())) {
                    throw Exception("El backup está vacío.")
                }

                // 4. Guardamos y festejamos
                repository.restoreBackup(backup)

                val msg = "✓ Importados: ${backup.audiovisuales.size} audiovisuales y ${backup.impresos.size} impresos"
                _state.update { it.copy(isBackupProcessing = false, backupSuccess = msg) }

            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isBackupProcessing = false, backupError = "Error: ${e.message}") }
            }
        }
    }

    fun clearBackupMessages() {
        _state.update { it.copy(backupSuccess = null, backupError = null) }
    }

    fun clearImportMessages() {
        _state.update { it.copy(importSuccess = null, importError = null) }
    }

    fun plusOneEpisode(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.updateItem(
                item.copy(watchedEpisodes = item.watchedEpisodes + 1)
            )
        }
    }

    fun deletePrintItem(item: PrintMediaEntity) {
        viewModelScope.launch { repository.deletePrintItem(item) }
    }

    fun plusOnePrintChapter(item: PrintMediaEntity) {
        viewModelScope.launch {
            val newChapter = item.currentChapter + 1

            repository.updatePrintItem(
                item.copy(currentChapter = newChapter)
            )
        }
    }
}