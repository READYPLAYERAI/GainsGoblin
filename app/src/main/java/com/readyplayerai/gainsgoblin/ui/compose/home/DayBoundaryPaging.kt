package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The Home day pager's TikTok-style boundary friction.
 *
 * Each day is a full-screen [androidx.compose.foundation.pager.VerticalPager] page whose own
 * content is a `verticalScroll` child. The pager runs with `userScrollEnabled = false` and a
 * no-op `pageNestedScrollConnection`, so the only thing that can move it is this per-page
 * [NestedScrollConnection]: it watches the child scroll's edges and, once the child has run out
 * of room, feeds the leftover finger travel into the pager. Slow drags at a boundary are
 * damped (the pager advances at [DAMP_FEED] of finger speed and snaps back if the page is not
 * committed); fast drags and flings page freely, one page per fling.
 *
 * Six empirically-verified gotchas (Compose foundation 1.12.1) drive this design:
 *
 * 1. **Keep-alive pre-scroll.** A child drag that reaches its edge and has nothing left to
 *    consume is cancelled. On a slow boundary drag we consume `(1 - DAMP_FEED)` of the delta in
 *    `onPreScroll` purely to keep the child's drag alive; the pager then receives exactly the
 *    remaining [DAMP_FEED] through `onPostScroll`.
 * 2. **SideEffect fling feeds.** A child fling emits frames with
 *    [NestedScrollSource.SideEffect]; these MUST be fed to the pager too, otherwise a fast fling
 *    that starts mid-content and reaches the edge dies there. `onPostScroll` therefore handles
 *    both `UserInput` and `SideEffect`.
 * 3. **No-op default connection.** The pager's default `pageNestedScrollConnection` double-feeds
 *    once a page drag is in progress, so we replace it with [NoOpNestedScrollConnection].
 * 4. **Tail spacer requirement.** A page whose content is shorter than the viewport (rest days)
 *    would have a scroll range of 0 and never claim the gesture, leaving the page dead. A 1dp
 *    tail spacer on every page keeps the child's range positive so it always owns the drag.
 * 5. **Fling feed cap.** A child fling reaching the edge feeds its decay frames through
 *    `onPostScroll`; without a cap a fast fling from mid-content glides unbounded pages, so
 *    fling-sourced feed is capped at one page size per fling (`flingFed`), keeping flings
 *    one-page (native/TikTok behavior).
 * 6. **Tiny-range release fallback.** Rest-day pages have a ~3px scrollable range, so the child
 *    drag ends on the first frame and a mid-gesture fling consumes the release velocity;
 *    `onPostFling` then sees `available.y == 0`, so fast releases are detected via the drag EMA
 *    (`lastDragEma`) plus the last feed direction (`lastFedDir`) instead of the release velocity.
 */
internal val NoOpNestedScrollConnection = object : NestedScrollConnection {}

/** Velocity (px/ms) below which a boundary drag is considered slow and gets damped. */
internal const val SLOW_VELOCITY_PX_PER_MS = 1.0f

/** Fraction of slow finger travel fed into the pager at a boundary (the rest is resistance). */
internal const val DAMP_FEED = 0.40f

/** Release velocity (px/s) above which a boundary drag becomes a free one-page fling. */
internal const val FLING_VELOCITY_PX_PER_S = 1200f

/** Release velocity (px/s) below which a release counts as "no velocity left" (see onPostFling). */
internal const val RELEASE_VELOCITY_FLOOR = 100f

/** Smallest leftover delta (px) worth feeding into the pager. */
internal const val FEED_MIN_DELTA = 0.1f

/**
 * Remembers the per-page boundary connection for [childScroll] driving [pagerState].
 *
 * @param pagerState the day pager being driven.
 * @param childScroll the page's inner vertical scroll state (its edge is the day boundary).
 * @param pageCount total number of pages, used to clamp fling targets.
 * @param scope scope used to settle the pager after a fling.
 * @param pageScrolls per-page inner scroll states (keyed by pager index) so the arrival
 *   day can be reset to its top before the pager settles.
 */
@Composable
internal fun rememberDayBoundaryPagingConnection(
    pagerState: PagerState,
    childScroll: ScrollState,
    pageCount: Int,
    scope: CoroutineScope,
    pageScrolls: Map<Int, ScrollState>,
): NestedScrollConnection = remember(childScroll, pagerState, pageCount, pageScrolls) {
    object : NestedScrollConnection {
        var ema = 0f
        var lastT = 0L
        var inBoundary = false
        var feeding = false
        var flingFed = 0f
        var lastDragEma = 0f   // drag velocity EMA captured at release
        var lastFedDir = 0     // +1 forward (older), -1 backward; direction of the last boundary feed

        fun boundary(dy: Float): Boolean =
            (dy > 0f && childScroll.value <= 0) ||
                (dy < 0f && childScroll.value >= childScroll.maxValue)

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            val dy = available.y
            if (!boundary(dy)) {
                inBoundary = false
                feeding = false
                ema = 0f
                return Offset.Zero
            }
            inBoundary = true
            feeding = true
            val now = System.nanoTime()
            val dt = if (lastT == 0L) {
                1f
            } else {
                ((now - lastT) / 1_000_000f).coerceIn(1f, 64f)
            }
            lastT = now
            ema = 0.55f * ema + 0.45f * (abs(dy) / dt)
            lastFedDir = if (dy < 0f) 1 else -1
            return if (ema < SLOW_VELOCITY_PX_PER_MS) {
                // Consume most of the delta just to keep the child's drag alive; the pager
                // gets the remaining DAMP_FEED via onPostScroll.
                Offset(0f, dy * (1f - DAMP_FEED))
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val dy = available.y
            if (abs(dy) < FEED_MIN_DELTA) return Offset.Zero
            if (!boundary(dy)) return Offset.Zero
            feeding = true
            if (source != NestedScrollSource.UserInput &&
                abs(flingFed) >= pagerState.layoutInfo.pageSize
            ) {
                // Fling feed capped: one page per fling, TikTok-style.
                return Offset.Zero
            }
            val fed = pagerState.dispatchRawDelta(-dy)
            if (source != NestedScrollSource.UserInput) {
                flingFed += fed
            }
            return Offset(0f, fed)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            lastDragEma = ema
            ema = 0f
            flingFed = 0f
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val wasFeeding = feeding
            inBoundary = false
            feeding = false
            if (!wasFeeding) return Velocity.Zero
            val v = available.y
            val frac = pagerState.currentPageOffsetFraction
            // finger up (v < 0) pages forward (older); v > 0 pages backward.
            // On tiny-range pages (rest days) the child drag ends after ~3px and a mid-gesture
            // fling eats the release velocity, so v == 0 at release: fall back to the tracked
            // drag velocity (EMA) and the last feed direction to keep fast releases paging.
            val dir = if (v != 0f) -sign(v).toInt() else lastFedDir
            val fastRelease = abs(v) > FLING_VELOCITY_PX_PER_S ||
                (abs(v) < RELEASE_VELOCITY_FLOOR &&
                    lastDragEma > SLOW_VELOCITY_PX_PER_MS &&
                    dir != 0)
            val target = if (fastRelease) {
                pagerState.currentPage + dir
            } else if (abs(frac) > 0.5f) {
                pagerState.currentPage + sign(frac).toInt()
            } else {
                pagerState.currentPage
            }
            scope.launch {
                val clampedTarget = target.coerceIn(0, pageCount - 1)
                // A day change lands expanded: reset the arrival day to its top before the
                // settle animation (a release back onto the same day keeps its place).
                if (clampedTarget != pagerState.settledPage) {
                    pageScrolls[clampedTarget]?.scrollTo(0)
                }
                pagerState.animateScrollToPage(clampedTarget)
            }
            return available
        }
    }
}
