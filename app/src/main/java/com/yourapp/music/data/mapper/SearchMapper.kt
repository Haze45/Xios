package com.yourapp.music.data.mapper

import com.yourapp.music.data.local.entity.RecentSearchEntity
import com.yourapp.music.domain.model.RecentSearch

fun RecentSearchEntity.toDomain() = RecentSearch(query = query, searchedAt = searchedAt)
