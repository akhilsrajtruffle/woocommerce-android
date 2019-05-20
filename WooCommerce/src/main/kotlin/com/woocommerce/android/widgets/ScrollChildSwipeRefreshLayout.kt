package com.woocommerce.android.widgets

import android.content.Context
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.util.AttributeSet
import android.view.View

/**
 * Extends [SwipeRefreshLayout] to support non-direct descendant scrolling views.
 *
 * [SwipeRefreshLayout] works as expected when a scroll view is a direct child: it triggers
 * the refresh only when the view is on top. This class adds a way (@link #setScrollUpChild} to
 * define which view controls this behavior.
 *
 * see https://github.com/googlesamples/android-architecture
 */
class ScrollChildSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
)
    : androidx.swiperefreshlayout.widget.SwipeRefreshLayout(context, attrs) {
    var scrollUpChild: View? = null

    override fun canChildScrollUp() =
            scrollUpChild?.canScrollVertically(-1) ?: super.canChildScrollUp()
}
