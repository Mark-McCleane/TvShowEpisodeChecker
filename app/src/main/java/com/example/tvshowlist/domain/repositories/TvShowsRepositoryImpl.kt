package com.example.tvshowlist.domain.repositories

import com.example.tvshowlist.data.db.TvShowCheckerDao
import com.example.tvshowlist.data.entities.getTvShow.GetTvShowApiResponse
import com.example.tvshowlist.data.entities.getTvShowSeason.GetSeasonApiResponse
import com.example.tvshowlist.data.entities.search.SearchApiResponse
import com.example.tvshowlist.data.remote.RetrofitInstance
import com.example.tvshowlist.domain.model.TvShow
import com.example.tvshowlist.domain.model.TvShowSeasonEpisodes


class TvShowsRepositoryImpl(private val dao: TvShowCheckerDao): TvShowsRepository {
    override suspend fun getTVShows(query: String): SearchApiResponse =
        RetrofitInstance.api.searchTvShowsByName(query = query)

    override suspend fun getTVShowById(tvShowId: Int): GetTvShowApiResponse =
        RetrofitInstance.api.getTvShowById(id = tvShowId)

    override suspend fun getTvShowSeason(tvShowId: Int, seasonNumber: Int): GetSeasonApiResponse =
        RetrofitInstance.api.getSeasonById(tvId = tvShowId, seasonNumber = seasonNumber)

    override suspend fun insertTvShow(tvShow: TvShowSeasonEpisodes) =
        dao.upsertTvShowChecker(tvShowChecker = tvShow)

    override suspend fun insertRecentTvShow(tvShow: TvShow) = dao.insertRecentTvShow(tvShow = tvShow)
    override suspend fun getRecentTvShows(): List<TvShow> = dao.getRecentTvShows()
    override suspend fun updateIsWatchedStatus(episodeId: Int, isWatchedStatus: Boolean) =
        dao.updateIsWatchedStatus(episodeId = episodeId, isWatchedStatus = isWatchedStatus)

    override suspend fun getIsWatchedStatus(episodeId: Int): Boolean =
        dao.getIsWatchedStatus(episodeId = episodeId)

    override suspend fun getTvShowSeasonOffline(
        tvShowId: Int,
        seasonSelected: Int
    ): List<TvShowSeasonEpisodes> =
        dao.getTvShowSeasonOffline(tvShowId = tvShowId, seasonSelected = seasonSelected)
}
