package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseRecord
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker

/**
 * Renders the Records lens: a two-column grid of all-time-record tiles, filtered
 * by the search [query], the selected body-part [category] and [favouritesOnly].
 *
 * The incoming [records] order is preserved (the domain already sorts favourites
 * first) and the whole body is emitted as ordinary composables so no nested lazy
 * container is introduced.
 */
@Composable
fun RecordsLens(
    records: List<ExerciseRecord>,
    query: String,
    category: String?,
    favouritesOnly: Boolean,
    onOpenRecord: (String) -> Unit,
) {
    val filtered = records.filter { record ->
        record.name.contains(query, ignoreCase = true) &&
            (category == null || record.category == category) &&
            (!favouritesOnly || record.favorite)
    }

    if (filtered.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.records_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.records_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.records_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.records_count, filtered.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        filtered.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { record ->
                    RecordTile(
                        record = record,
                        onOpenRecord = onOpenRecord,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One all-time-record tile: category, name, best weight, meta line and sparkline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTile(
    record: ExerciseRecord,
    onOpenRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onOpenRecord(record.name) },
        modifier = modifier.heightIn(min = 190.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryMarker(category = record.category, size = 11.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = record.category.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                if (record.favorite) {
                    Text(
                        text = "★",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Text(
                text = record.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = formatWeight(record.maxWeight),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text = " kg",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    R.string.records_tile_meta,
                    formatWeight(record.maxWeightReps),
                    formatWeight(record.estOneRep),
                ),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Sparkline(
                values = record.sessions.takeLast(8).map { it.estOneRep.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                showArea = false,
                showLastDot = false,
                strokeWidth = 2.dp,
            )
        }
    }
}
