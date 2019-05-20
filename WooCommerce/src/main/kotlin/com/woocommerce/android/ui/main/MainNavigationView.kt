package com.woocommerce.android.ui.main

import android.content.Context
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemReselectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.extensions.active
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.main.BottomNavigationPosition.DASHBOARD
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration

class MainNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr),
        OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private lateinit var navAdapter: NavAdapter
    private lateinit var fragmentManager: androidx.fragment.app.FragmentManager
    private lateinit var listener: MainNavigationListener
    private lateinit var badgeView: View

    companion object {
        private var previousNavPos: BottomNavigationPosition? = null
    }

    interface MainNavigationListener {
        fun onNavItemSelected(navPos: BottomNavigationPosition)

        fun onNavItemReselected(navPos: BottomNavigationPosition)
    }

    val activeFragment: TopLevelFragment
        get() = navAdapter.getFragment(currentPosition)

    var currentPosition: BottomNavigationPosition
        get() = findNavigationPositionById(selectedItemId)
        set(navPos) = updateCurrentPosition(navPos)

    fun init(fm: androidx.fragment.app.FragmentManager, listener: MainNavigationListener) {
        this.fragmentManager = fm
        this.listener = listener

        navAdapter = NavAdapter()

        // set up the bottom bar
        val menuView = getChildAt(0) as BottomNavigationMenuView
        val inflater = LayoutInflater.from(context)
        val itemView = menuView.getChildAt(BottomNavigationPosition.NOTIFICATIONS.position) as BottomNavigationItemView
        badgeView = inflater.inflate(R.layout.notification_badge_view, menuView, false)
        itemView.addView(badgeView)

        assignNavigationListeners(true)

        // Default to the dashboard position
        active(DASHBOARD.position)
    }

    fun getFragment(navPos: BottomNavigationPosition): TopLevelFragment = navAdapter.getFragment(navPos)

    fun updatePositionAndDeferInit(navPos: BottomNavigationPosition) {
        updateCurrentPosition(navPos, true)
    }

    /**
     * For use when restoring the navigation bar after the host activity
     * state has been restored.
     */
    fun restoreSelectedItemState(itemId: Int) {
        assignNavigationListeners(false)
        selectedItemId = itemId
        assignNavigationListeners(true)
    }

    fun showNotificationBadge(show: Boolean) {
        with(badgeView) {
            if (show && visibility != View.VISIBLE) {
                WooAnimUtils.fadeIn(this, Duration.MEDIUM)
            } else if (!show && visibility == View.VISIBLE) {
                WooAnimUtils.fadeOut(this, Duration.MEDIUM)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navPos = findNavigationPositionById(item.itemId)
        currentPosition = navPos

        listener.onNavItemSelected(navPos)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val navPos = findNavigationPositionById(item.itemId)
        val activeFragment = fragmentManager.findFragmentByTag(navPos.getTag())
        if (!clearFragmentBackStack(activeFragment)) {
            (activeFragment as? TopLevelFragment)?.scrollToTop()
        }

        listener.onNavItemReselected(navPos)
    }

    private fun updateCurrentPosition(navPos: BottomNavigationPosition, deferInit: Boolean = false) {
        assignNavigationListeners(false)
        try {
            selectedItemId = navPos.id
        } finally {
            assignNavigationListeners(true)
        }

        val fragment = navAdapter.getFragment(navPos)
        fragment.deferInit = deferInit

        // Close any child fragments if open
        clearFragmentBackStack(fragment)

        // hide previous fragment if it exists
        val fragmentTransaction = fragmentManager.beginTransaction()
        previousNavPos?.let { fragmentTransaction.hide(navAdapter.getFragment(it)) }

        // add the fragment if it hasn't been added yet
        val tag = navPos.getTag()
        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentTransaction.add(R.id.container, fragment, tag)
        }

        // show the new fragment
        fragmentTransaction.show(fragment)
        fragmentTransaction.commitAllowingStateLoss()

        previousNavPos = navPos
    }

    private fun assignNavigationListeners(assign: Boolean) {
        setOnNavigationItemSelectedListener(if (assign) this else null)
        setOnNavigationItemReselectedListener(if (assign) this else null)
    }

    /**
     * Pop all child fragments to return to the top-level view.
     * returns true if child fragments existed.
     */
    private fun clearFragmentBackStack(fragment: androidx.fragment.app.Fragment?): Boolean {
        fragment?.let {
            if (!it.isAdded) {
                return false
            }
            with(it.childFragmentManager) {
                if (backStackEntryCount > 0) {
                    val firstEntry = getBackStackEntryAt(0)
                    popBackStackImmediate(firstEntry.id, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Extension function for retrieving an existing fragment from the [FragmentManager]
     * if one exists, if not, create a new instance of the requested fragment.
     */
    private fun androidx.fragment.app.FragmentManager.findFragment(position: BottomNavigationPosition): TopLevelFragment {
        return (findFragmentByTag(position.getTag()) ?: position.createFragment()) as TopLevelFragment
    }

    // region Private Classes
    private inner class NavAdapter {
        private val fragments = SparseArray<TopLevelFragment>(BottomNavigationPosition.values().size)

        internal fun getFragment(navPos: BottomNavigationPosition): TopLevelFragment {
            fragments[navPos.position]?.let {
                return it
            }

            val fragment = fragmentManager.findFragment(navPos)
            fragments.put(navPos.position, fragment)
            return fragment
        }
    }
    // endregion
}
