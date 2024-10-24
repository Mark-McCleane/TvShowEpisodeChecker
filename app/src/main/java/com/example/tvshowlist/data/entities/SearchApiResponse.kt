package com.example.tvshowlist.data.entities

data class SearchApiResponse(
    val page: Int,
    val results: List<Result>,
    val total_pages: Int,
    val total_results: Int
)