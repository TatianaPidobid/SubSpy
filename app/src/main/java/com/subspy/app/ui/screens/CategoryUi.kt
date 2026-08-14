package com.subspy.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.subspy.app.R
import com.subspy.app.data.model.SubscriptionCategory
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.ui.theme.SuccessGreen
import com.subspy.app.ui.theme.WarningOrange

/** Localised label for a subscription category. */
@Composable
fun categoryLabel(category: SubscriptionCategory): String = stringResource(
    when (category) {
        SubscriptionCategory.ENTERTAINMENT -> R.string.category_entertainment
        SubscriptionCategory.WORK -> R.string.category_work
        SubscriptionCategory.HEALTH -> R.string.category_health
        SubscriptionCategory.MUSIC -> R.string.category_music
        SubscriptionCategory.OTHER -> R.string.category_other
    }
)

/** Accent colour used for a category in charts and chips. */
fun categoryColor(category: SubscriptionCategory): Color = when (category) {
    SubscriptionCategory.ENTERTAINMENT -> GreenAccent
    SubscriptionCategory.WORK -> Color(0xFF4FC3F7)
    SubscriptionCategory.HEALTH -> SuccessGreen
    SubscriptionCategory.MUSIC -> Color(0xFFBA68C8)
    SubscriptionCategory.OTHER -> WarningOrange
}
