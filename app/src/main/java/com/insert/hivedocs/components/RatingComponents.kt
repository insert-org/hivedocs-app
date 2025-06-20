package com.insert.hivedocs.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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