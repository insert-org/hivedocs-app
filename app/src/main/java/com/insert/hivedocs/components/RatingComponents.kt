package com.insert.hivedocs.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.insert.hivedocs.R
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun StarRatingDisplay(
    rating: Float,
    maxRating: Int = 5,
    starColor: Color = Color(0xFFFFC107),
    starSize: Dp = 24.dp
) {
    Row {
        val fullStars = floor(rating).toInt()
        val halfStar = ceil(rating) - floor(rating) >= 0.5f
        val emptyStars = maxRating - fullStars - if (halfStar) 1 else 0

        repeat(fullStars) {
            Icon(
                painter = painterResource(id = R.drawable.star_solid),
                contentDescription = null,
                tint = starColor,
                modifier = Modifier.size(starSize)
            )
        }
        if (halfStar && (fullStars + emptyStars) < maxRating) {
            Icon(
                painter = painterResource(id = R.drawable.star_half_solid),
                contentDescription = null,
                tint = starColor,
                modifier = Modifier.size(starSize)
            )
        }
        repeat(emptyStars) {
            Icon(
                painter = painterResource(id = R.drawable.star_regular),
                contentDescription = null,
                tint = starColor,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun StarRatingInput(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    maxRating: Int = 5,
    starColor: Color = MaterialTheme.colorScheme.primary,
    starSize: Dp = 36.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..maxRating) {
            Box(
                modifier = Modifier.size(starSize)
            ) {
                val icon = when {
                    i <= floor(rating) -> painterResource(id = R.drawable.star_solid)
                    i > floor(rating) && i.toFloat() == ceil(rating) && (rating - floor(rating)) >= 0.5f -> painterResource(id = R.drawable.star_half_solid)
                    else -> painterResource(id = R.drawable.star_regular)
                }
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = starColor,
                    modifier = Modifier.fillMaxSize()
                )

                Row(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable { onRatingChange(i - 0.5f) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable { onRatingChange(i.toFloat()) }
                    )
                }
            }
        }
    }
}