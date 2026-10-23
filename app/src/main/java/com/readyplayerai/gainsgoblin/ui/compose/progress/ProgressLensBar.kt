package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.readyplayerai.gainsgoblin.R

/** The three content lenses shown in the Progress screen's segmented bar. */
enum class ProgressLens { JOURNAL, RECORDS, INSIGHTS }

/**
 * Official Material 3 Expressive segmented lens bar for switching between the
 * Journal, Records and Insights lenses. Built on [SingleChoiceSegmentedButtonRow],
 * so it uses the official default colours, shapes and motion, and shows the
 * official check-icon crossfade in the icon slot for the selected lens.
 */
@Composable
fun ProgressLensBar(
    selected: ProgressLens,
    onSelect: (ProgressLens) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        ProgressLens.entries.forEachIndexed { index, lens ->
            SegmentedButton(
                selected = selected == lens,
                onClick = { onSelect(lens) },
                shape = SegmentedButtonDefaults.itemShape(index, ProgressLens.entries.size),
                icon = {
                    SegmentedButtonDefaults.Icon(
                        active = selected == lens,
                        inactiveContent = {
                            Icon(
                                painter = painterResource(lens.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        },
                    )
                },
                label = { Text(stringResource(lens.labelRes), maxLines = 1) },
            )
        }
    }
}

/** The label string shown on a [ProgressLens] segment. */
private val ProgressLens.labelRes: Int
    get() = when (this) {
        ProgressLens.JOURNAL -> R.string.progress_lens_journal
        ProgressLens.RECORDS -> R.string.progress_lens_records
        ProgressLens.INSIGHTS -> R.string.progress_lens_insights
    }

/** The icon shown on a [ProgressLens] segment while it is not selected. */
private val ProgressLens.iconRes: Int
    get() = when (this) {
        ProgressLens.JOURNAL -> R.drawable.ic_menu_24px
        ProgressLens.RECORDS -> R.drawable.ic_emoji_events_24px
        ProgressLens.INSIGHTS -> R.drawable.ic_leaderboard_24px
    }
