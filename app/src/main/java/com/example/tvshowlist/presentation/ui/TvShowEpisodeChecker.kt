package com.example.tvshowlist.presentation.ui

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tvshowlist.domain.model.TvShowSeasonEpisodes
import com.example.tvshowlist.presentation.MainViewModel
import com.example.tvshowlist.utils.ApplicationOnlineChecker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowEpisodeChecker(
    tvShowId: Int,
    tvShowName: String,
    viewModel: MainViewModel
) {
    val tvShow = viewModel.selectedTvShow.collectAsState()
    val seasonEpisodes = viewModel.selectedSeason.collectAsState()
    val isEpisodesLoaded by viewModel.isEpisodesLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var isSeasonsDropDownExpanded by remember {
        mutableStateOf(false)
    }
    var seasonSelected by remember {
        mutableIntStateOf(1)
    }

    LaunchedEffect(key1 = tvShow) {
        if (ApplicationOnlineChecker.isOnline(context)) {
            viewModel.getTvShowById(tvShowId)
        }
    }

    LaunchedEffect(key1 = seasonSelected) {
        if (ApplicationOnlineChecker.isOnline(context)) {
            viewModel.getTvShowSeasons(tvShowId, seasonSelected)
        } else {
            viewModel.getTvShowSeasonsOffline(tvShowId, seasonSelected)
        }
    }

    LaunchedEffect(key1 = error) {
        if (error.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                if (!ApplicationOnlineChecker.isOnline(context)) "No Internet Connection" else error,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )
        }
    }

    val seasonList = (1..(tvShow.value?.seasonCount ?: 1)).toList()

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(
                text = tvShow.value?.title ?: tvShowName
            )
        })
    },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .clickable { isSeasonsDropDownExpanded = true }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Seasons",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown Arrow",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f)
                    )
                }

                DropdownMenu(
                    expanded = isSeasonsDropDownExpanded,
                    onDismissRequest = { isSeasonsDropDownExpanded = false },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                ) {
                    seasonList.forEachIndexed { index, seasonNumber ->
                        DropdownMenuItem(
                            onClick = {
                                seasonSelected = seasonNumber
                                isSeasonsDropDownExpanded = false
                            },
                            text = {
                                Text(
                                    text = "Season $seasonNumber",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }
            if (isEpisodesLoaded) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 0.dp, top = 0.dp, bottom = 16.dp, end = 0.dp)
                ) {
                    itemsIndexed(seasonEpisodes.value.filter { it.seasonNumber == seasonSelected }) { index, seasonEpisode ->
                        var isExpanded by remember { mutableStateOf(false) }
                        var isWatched by rememberSaveable {
                            mutableStateOf(seasonEpisode.isChecked ?: false)
                        }

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "${seasonEpisode.episodeNumber}. ${seasonEpisode.episodeName}",
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                                RatingSection(tvShowSeasonEpisodes = seasonEpisode)
                            },
                            supportingContent = {
                                Text(
                                    text = seasonEpisode.overview,
                                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 3
                                )
                            },
                            overlineContent = {
                            },

                            leadingContent = {
                                AsyncImage(
                                    model = seasonEpisode.episodeImage,
                                    error = painterResource(id = android.R.drawable.stat_notify_error),
                                    contentDescription = "${seasonEpisode.episodeName} Episode Image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(100.dp)
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isWatched,
                                    onCheckedChange = {
                                        isWatched = changeWatchedStatus(
                                            isWatched = isWatched,
                                            viewModel = viewModel,
                                            episodeId = seasonEpisode.episodeId
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            },
                            modifier = Modifier.clickable {
                                isWatched = changeWatchedStatus(
                                    isWatched = isWatched,
                                    viewModel = viewModel,
                                    episodeId = seasonEpisode.episodeId
                                )
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

private fun changeWatchedStatus(
    isWatched: Boolean,
    viewModel: MainViewModel,
    episodeId: Int
): Boolean {
    val isWatchedChanged = !isWatched
    viewModel.updateIsWatchedState(isWatched = isWatchedChanged, episodeId = episodeId)
    return isWatchedChanged
}

@Composable
private fun RatingSection(tvShowSeasonEpisodes: TvShowSeasonEpisodes) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Star Icon",
            tint = androidx.compose.ui.graphics.Color.Yellow,
            modifier = Modifier.weight(0.20f)
        )
        val rating =
            String.format(Locale.getDefault(), "%.1f", tvShowSeasonEpisodes.voteAverage)

        Text(
            text = rating,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(0.25f)
        )
        val airDate = formatDate(tvShowSeasonEpisodes.episodeAirDate)
        Text(
            text = airDate,
            maxLines = 1,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
        )
    }
}

private fun formatDate(episodeAirDate: String): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return episodeAirDate

    val inputFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd",
        Locale.getDefault()
    )
    val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    return try {
        val date = LocalDate.parse(episodeAirDate, inputFormatter)
        date.format(outputFormatter)
    } catch (e: DateTimeParseException) {
        episodeAirDate
    }
}