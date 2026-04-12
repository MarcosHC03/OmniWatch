package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Modelos de estado
// ---------------------------------------------------------------------------

/**
 * Un día de la semana con los títulos en emisión activa asignados a ese día.
 * Solo se incluye en la lista si tiene al menos un ítem.
 */
data class WeekDay(
    val dayOfWeek: DayOfWeek,
    val label: String,           // "Lunes", "Martes", … en español
    val items: List<MediaItemEntity>
)

data class CalendarState(

    // ── Pestaña "Viendo" ────────────────────────────────────────────────────
    /** En emisión activa (isAiring == true), agrupados por día de la semana. */
    val airingSchedule: List<WeekDay> = emptyList(),

    /** Finalizados / sin estado de emisión (isAiring == false), orden alfabético. */
    val finishedWatching: List<MediaItemEntity> = emptyList(),

    // ── Pestaña "Por ver" ───────────────────────────────────────────────────
    /** Fecha futura o sin fecha — todavía no se estrenó. */
    val upcomingReleases: List<MediaItemEntity> = emptyList(),

    /** Fecha pasada — ya se estrenó pero el usuario aún no lo vio. */
    val releasedItems: List<MediaItemEntity> = emptyList(),

    // ── Control ─────────────────────────────────────────────────────────────
    val isLoading: Boolean = true,
    val error: String? = null
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState(isLoading = true))
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    // Formato en que guardamos la fecha en la entidad
    private val displayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    init {
        observeData()
    }

    // -------------------------------------------------------------------------
    // Observación reactiva
    // -------------------------------------------------------------------------

    private fun observeData() {
        viewModelScope.launch {
            repository.getAllItems()
                .catch { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Error desconocido")
                    }
                }
                .collect { allItems ->
                    val watching = allItems.filter { it.watchStatus == WatchStatus.WATCHING }
                    val planned  = allItems.filter { it.watchStatus == WatchStatus.PLANNED }
                    val today    = LocalDate.now()

                    _state.update {
                        it.copy(
                            airingSchedule   = buildAiringSchedule(watching),
                            finishedWatching = buildFinishedWatching(watching),
                            upcomingReleases = buildUpcomingReleases(planned, today),
                            releasedItems    = buildReleasedItems(planned, today),
                            isLoading        = false,
                            error            = null
                        )
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Lógica — pestaña "Viendo"
    // -------------------------------------------------------------------------

    /**
     * Agrupa los ítems con `isAiring == true` por `airDayOfWeek` (1–7).
     * Los que tienen `airDayOfWeek == 0` van a un bucket "Sin asignar" al final.
     * Solo se incluyen los días que tienen al menos un ítem.
     */
    private fun buildAiringSchedule(watching: List<MediaItemEntity>): List<WeekDay> {
        val airing  = watching.filter { it.isAiring }
        val grouped = airing.groupBy { it.airDayOfWeek }

        val ordered = DayOfWeek.values().mapNotNull { dow ->
            val items = grouped[dow.value] ?: return@mapNotNull null
            WeekDay(
                dayOfWeek = dow,
                label     = dow.getDisplayName(TextStyle.FULL, Locale("es", "AR"))
                                .replaceFirstChar { it.uppercase() },
                items     = items
            )
        }

        val unassigned = grouped[0] ?: emptyList()
        return if (unassigned.isEmpty()) {
            ordered
        } else {
            ordered + WeekDay(
                dayOfWeek = DayOfWeek.SUNDAY, // placeholder estructural, no se usa para display
                label     = "Sin asignar",
                items     = unassigned
            )
        }
    }

    /**
     * Lista simple de ítems con `isAiring == false`, orden alfabético.
     */
    private fun buildFinishedWatching(watching: List<MediaItemEntity>): List<MediaItemEntity> =
        watching
            .filter { !it.isAiring }
            .sortedBy { it.title.lowercase() }

    // -------------------------------------------------------------------------
    // Lógica — pestaña "Por ver"
    // -------------------------------------------------------------------------

    /**
     * Parsea `releaseDate` (dd/MM/yyyy) y devuelve `null` si está vacía o no parsea.
     */
    private fun MediaItemEntity.parsedReleaseDate(): LocalDate? {
        if (releaseDate.isBlank()) return null
        return try {
            LocalDate.parse(releaseDate.trim(), displayFormat)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /**
     * "Próximos": fecha futura o sin fecha, ordenados:
     *   1. Con fecha → los más cercanos primero.
     *   2. Sin fecha → al final, alfabéticamente.
     */
    private fun buildUpcomingReleases(
        planned: List<MediaItemEntity>,
        today: LocalDate
    ): List<MediaItemEntity> {
        val (withDate, withoutDate) = planned.partition { it.parsedReleaseDate() != null }

        val futureWithDate = withDate
            .filter { it.parsedReleaseDate()!! >= today }   // hoy inclusive = todavía no visto
            .sortedBy { it.parsedReleaseDate() }

        val noDate = withoutDate.sortedBy { it.title.lowercase() }

        return futureWithDate + noDate
    }

    /**
     * "Ya estrenados": fecha estrictamente anterior a hoy, orden del más reciente al más viejo.
     */
    private fun buildReleasedItems(
        planned: List<MediaItemEntity>,
        today: LocalDate
    ): List<MediaItemEntity> =
        planned
            .filter { date -> date.parsedReleaseDate()?.let { it < today } == true }
            .sortedByDescending { it.parsedReleaseDate() }

    // -------------------------------------------------------------------------
    // Acciones públicas
    // -------------------------------------------------------------------------

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        observeData()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
