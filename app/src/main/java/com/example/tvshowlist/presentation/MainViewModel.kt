package com.example.tvshowlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvshowlist.data.entities.search.Result
import com.example.tvshowlist.data.remote.AppMapper
import com.example.tvshowlist.domain.model.TvShow
import com.example.tvshowlist.domain.model.TvShowSeasonEpisodes
import com.example.tvshowlist.domain.repositories.TvShowsRepository
import com.example.tvshowlist.presentation.ui.EpisodeCheckerScreen.EpisodeCheckerUIState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repository: TvShowsRepository
) : ViewModel() {

    private val _episodeCheckerUIState = MutableStateFlow(EpisodeCheckerUIState())
    val episodeCheckerUIState = _episodeCheckerUIState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching
        .onStart { getTvShows() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow("")
    val error = _error.asStateFlow()

    private val _tvShowList = MutableStateFlow(listOf<TvShow>())
    val tvShowList = searchText
        .debounce(1_000L)
        .onEach { _isSearching.update { true } }
        .combine(_tvShowList) { text, tvShow ->
            if (text.isBlank()) {
                tvShow
            } else {
                tvShow.filter { it.searchTvShow(text) }
            }
        }.onEach { _isSearching.update { false } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _tvShowList.value
        )

    private val _recentTvShowList = MutableStateFlow(listOf<TvShow>())
    val recentTvShowList = _recentTvShowList.asStateFlow()

    init {
        viewModelScope.launch {
            getRecentTvShows()
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        viewModelScope.launch { getTvShows(text) }
    }

    private suspend fun getTvShows(query: String = ""): List<TvShow> {
        val duplicateRemoverSet = mutableSetOf<Result>()
        try {
            val temp = repository.getTVShows(query).results
            temp.forEach {
                duplicateRemoverSet.add(it)
            }
        } catch (e: Exception) {
            _error.update { e.localizedMessage ?: "No error found" }
        }
        val tvShowList = AppMapper.mapGetTvShowsApiResultToTvShowList(duplicateRemoverSet.toList())
        _tvShowList.update { tvShowList }
        return tvShowList
    }

    suspend fun getRecentTvShows() {
        val duplicateRemoverSet = mutableSetOf<TvShow>()
        repository.getRecentTvShows().collect { tvShowList ->
            tvShowList.forEach {
                duplicateRemoverSet.add(it)
            }
        }
        _recentTvShowList.update { duplicateRemoverSet.toList() }
    }

    fun insertRecentTvShow(tvShow: TvShow) {
        viewModelScope.launch {
            repository.insertRecentTvShow(tvShow = tvShow)
        }
    }

    fun getTvShowById(tvShowId: Int) {
        viewModelScope.launch {
            try {
                _searchText.update { "" }
                val apiResult = repository.getTVShowById(tvShowId)
                val result = AppMapper.mapGetTvShowByIdApiResultToTvShow(apiResult)
                _episodeCheckerUIState.update { state -> state.copy(tvShow = result) }
            } catch (e: Exception) {
                _error.update { e.localizedMessage ?: "No error found" }
            }
        }
    }

    fun getTvShowSeasons(tvShowId: Int, seasonNumber: Int = 1) {
        viewModelScope.launch {
            _episodeCheckerUIState.update { state -> state.copy(isEpisodesLoaded = true) }
            try {
                val apiResult = repository.getTvShowSeason(tvShowId, seasonNumber)
                val result = AppMapper.mapGetTvShowSeasonsApiResultToTvShowSeason(apiResult)
                result.forEach {
                    it.isChecked = repository.getIsWatchedStatus(it.episodeId)
                    it.tvShowId = tvShowId
                    insertTvShowSeasonEpisodesToDB(it)
                }
                _episodeCheckerUIState.update { state -> state.copy(seasonEpisodes = result) }
            } catch (e: Exception) {
                _error.update { e.localizedMessage ?: "No error found" }
            }
            _episodeCheckerUIState.update { state -> state.copy(isEpisodesLoaded = false) }
        }
    }

    fun getTvShowSeasonsOffline(tvShowId: Int, seasonSelected: Int) {
        viewModelScope.launch {
            _episodeCheckerUIState.update { state -> state.copy(isEpisodesLoaded = true) }
            try {
                val databaseResult = repository.getTvShowSeasonOffline(tvShowId, seasonSelected)
                _episodeCheckerUIState.update { state -> state.copy(seasonEpisodes = databaseResult) }
            } catch (e: Exception) {
                _error.update { e.localizedMessage ?: "No error found" }
            }
            _episodeCheckerUIState.update { state -> state.copy(isEpisodesLoaded = false) }
        }
    }

    fun getTop10TvShowEpisodesById(tvShowId: Int){
        viewModelScope.launch {
            try {
                val dbResult = repository.getTop10TvShowEpisodesById(tvShowId = tvShowId)
                _episodeCheckerUIState.update { state -> state.copy(top10Episodes = dbResult) }
            } catch (e: Exception){
                _error.update { e.localizedMessage ?: "No error found" }
            }
        }
    }

    private fun insertTvShowSeasonEpisodesToDB(tvShowSeasonEpisodes: TvShowSeasonEpisodes) {
        viewModelScope.launch {
            repository.insertTvShow(tvShowSeasonEpisodes)
        }
    }

    fun updateIsWatchedState(episodeId: Int, isWatched: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateIsWatchedStatus(episodeId = episodeId, isWatchedStatus = isWatched)
                val currentTvShowId = episodeCheckerUIState.value.tvShow?.tvShowId
                val currentSeason =
                    _episodeCheckerUIState.value.seasonEpisodes.firstOrNull()?.seasonNumber

                if (currentTvShowId != null && currentSeason != null) {
                    getTvShowSeasons(currentTvShowId, currentSeason)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update episode status: ${e.message}"
            }
        }
    }
}