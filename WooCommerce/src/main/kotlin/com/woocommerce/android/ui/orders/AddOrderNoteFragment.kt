package com.woocommerce.android.ui.orders

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_ADD_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_add_order_note.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import javax.inject.Inject

class AddOrderNoteFragment : Fragment(), AddOrderNoteContract.View {
    companion object {
        const val TAG = "AddOrderNoteFragment"
        private const val FIELD_ORDER_IDENTIFIER = "order-identifier"
        private const val FIELD_ORDER_NUMBER = "order-number"
        private const val FIELD_NOTE_TEXT = "note_text"
        private const val FIELD_IS_CUSTOMER_NOTE = "is_customer_note"
        private const val FIELD_IS_CONFIRMING_DISCARD = "is_confirming_discard"

        fun newInstance(order: WCOrderModel): AddOrderNoteFragment {
            val args = Bundle().also {
                it.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())
                it.putString(FIELD_ORDER_NUMBER, order.number)
            }

            val fragment = AddOrderNoteFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: AddOrderNoteContract.Presenter
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var orderId: OrderIdentifier
    private lateinit var orderNumber: String

    private var isConfirmingDiscard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_order_note, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        // TODO supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)

        orderId = arguments?.getString(FIELD_ORDER_IDENTIFIER) ?: ""
        orderNumber = arguments?.getString(FIELD_ORDER_NUMBER) ?: ""

        savedInstanceState?.let { state ->
            addNote_switch.isChecked = state.getBoolean(FIELD_IS_CUSTOMER_NOTE)
            if (state.getBoolean(FIELD_IS_CONFIRMING_DISCARD)) {
                confirmDiscard()
            }
            state.getString(FIELD_NOTE_TEXT)?.let {
                addNote_editor.setText(it)
            }
        }

        if (orderId.isEmpty() || orderNumber.isEmpty()) {
            activity?.onBackPressed()
            return
        }

        if (presenter.hasBillingEmail(orderId)) {
            addNote_switch.setOnCheckedChangeListener { _, isChecked ->
                AnalyticsTracker.track(
                        ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
                        mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked)))

                val drawableId = if (isChecked) R.drawable.ic_note_public else R.drawable.ic_note_private
                addNote_icon.setImageDrawable(ContextCompat.getDrawable(activity!!, drawableId))
            }
        } else {
            addNote_switch.visibility = View.GONE
            addNote_switchDivider.visibility = View.GONE
            addNote_editDivider.visibility = View.GONE
        }

        activity?.title = getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

        presenter.takeView(this)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_add_note, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val noteText = getNoteText()

        return when (item?.itemId) {
            android.R.id.home -> {
                if (noteText.isNotEmpty()) {
                    confirmDiscard()
                    true
                } else {
                    super.onOptionsItemSelected(item)
                }
            }
            R.id.menu_add -> {
                AnalyticsTracker.track(ADD_ORDER_NOTE_ADD_BUTTON_TAPPED)

                if (!networkStatus.isConnected()) {
                    uiMessageResolver.showOfflineSnack()
                } else {
                    val isCustomerNote = addNote_switch.isChecked
                    if (noteText.isNotEmpty()) {
                        (activity as? AddOrderNoteActionListener)?.let {
                            it.onAddOrderNote(noteText, isCustomerNote)
                            activity?.onBackPressed()
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState?.putString(FIELD_NOTE_TEXT, getNoteText())
        outState?.putBoolean(FIELD_IS_CUSTOMER_NOTE, addNote_switch.isChecked)
        outState?.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        super.onSaveInstanceState(outState)
    }

    override fun getNoteText() = addNote_editor.text.toString().trim()

    override fun confirmDiscard() {
        isConfirmingDiscard = true
        AlertDialog.Builder(activity)
                .setMessage(R.string.add_order_note_confirm_discard)
                .setCancelable(true)
                .setPositiveButton(R.string.discard) { _, _ ->
                    activity?.onBackPressed()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    isConfirmingDiscard = false
                }
                .show()
    }
}