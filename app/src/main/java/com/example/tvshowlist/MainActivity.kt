package com.example.tvshowlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.tvshowlist.data.db.TvShowCheckerDao
import com.example.tvshowlist.data.db.TvShowCheckerDatabase
import com.example.tvshowlist.domain.model.TvShow
import com.example.tvshowlist.domain.model.ParcelableType
import com.example.tvshowlist.domain.repositories.SearchTVShowsRepository
import com.example.tvshowlist.ui.SearchField
import com.example.tvshowlist.ui.TvShowEpisodeChecker
import com.example.tvshowlist.ui.theme.TvShowListTheme
import com.example.tvshowlist.utils.ViewModelProviderFactory
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TvShowListTheme {
                val tvShowCheckerDao = TvShowCheckerDatabase.getDatabase(applicationContext).tvShowCheckerDao()
                val viewModelProviderFactory = ViewModelProviderFactory(SearchTVShowsRepository(tvShowCheckerDao))
                val viewModel =
                    ViewModelProvider(this, viewModelProviderFactory)[MainViewModel::class.java]
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = SearchScreenRoute){
                    composable<SearchScreenRoute>{
                        SearchField(
                            viewModel = viewModel,
                            navigateTo = {
                                it.addedToRecentDate = System.currentTimeMillis()
                                viewModel.insertRecentTvShow(it)
                                navController.navigate(TvShowCheckerScreenRoute(it.id, it.title))
                            }
                        )
                    }

                    composable<TvShowCheckerScreenRoute>(
                        typeMap = mapOf(typeOf<TvShow>() to ParcelableType<TvShow>())
                    ){ backStackEntry ->
                        val args = backStackEntry.toRoute<TvShowCheckerScreenRoute>()
                        TvShowEpisodeChecker(
                            tvShowId = args.tvShowId,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

@Serializable
object SearchScreenRoute

@Serializable
data class TvShowCheckerScreenRoute(val tvShowId: Int, val name: String)