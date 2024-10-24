package com.example.tvshowlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvshowlist.data.entities.AppMapper
import com.example.tvshowlist.data.entities.search.Result
import com.example.tvshowlist.domain.model.TvShow
import com.example.tvshowlist.domain.model.TvShowExtended
import com.example.tvshowlist.domain.repositories.SearchTVShowsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repository: SearchTVShowsRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _selectedTvShow = MutableStateFlow<TvShowExtended?>(null)
    val selectedTvShow = _selectedTvShow.asStateFlow()

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
            viewModelScope, SharingStarted.WhileSubscribed(5000), _tvShowList.value
        )

    init {
        viewModelScope.launch { getTvShows() }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        viewModelScope.launch { getTvShows(text) }
    }

    private suspend fun getTvShows(query: String = ""): List<TvShow> {
        val duplicateRemoverSet = mutableSetOf<Result>()
        val temp = repository.getTVShows(query).results
        temp.forEach {
            duplicateRemoverSet.add(it)
        }
        val tvShowList = AppMapper.mapGetTvShowsApiResultToTvShowList(duplicateRemoverSet.toList())
        _tvShowList.update { tvShowList }
        return tvShowList
    }

    fun getTvShowById(tvShowId: Int) {
        viewModelScope.launch {
            val apiResult = repository.getTVShowById(tvShowId)
            val result = AppMapper.mapGetTvShowByIdApiResultToTvShow(apiResult)
            _selectedTvShow.update { result }
        }
    }
}