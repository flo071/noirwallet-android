package org.noirofficial.presenter.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import org.noirofficial.R
import org.noirofficial.databinding.ActivityRecurringPaymentsBinding
import org.noirofficial.presenter.activities.callbacks.ActivityRecurringPaymentCallback
import org.noirofficial.presenter.activities.models.RecurringPayment
import org.noirofficial.presenter.activities.models.RecurringPaymentModel
import org.noirofficial.presenter.activities.base.BRActivity
import org.noirofficial.presenter.adapter.MultiTypeDataBoundAdapter
import org.noirofficial.tools.manager.JobsHelper
import org.noirofficial.wallet.BRWalletManager
import java.util.concurrent.Executors

class RecurringPaymentsActivity : BRActivity(), ActivityRecurringPaymentCallback {

    private val recurringPaymentModel: RecurringPaymentModel = RecurringPaymentModel()
    private lateinit var binding: ActivityRecurringPaymentsBinding;
    private var schedule: Schedule = Schedule.NOT_SET;
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: MultiTypeDataBoundAdapter

    public enum class Schedule {
        NOT_SET, DAILY, WEEKLY, MONTHLY
    }

    companion object {
        fun show(activity: AppCompatActivity) {
            val intent = Intent(activity, RecurringPaymentsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,
                R.layout.activity_recurring_payments)
        binding.data = recurringPaymentModel
        binding.callback = this
        setupToolbar()
        adapter = MultiTypeDataBoundAdapter(this)
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
        val recurringPayments: List<RecurringPayment> =
                RecurringPayment.listAll<RecurringPayment>(RecurringPayment::class.java)
        for (recurringPayment in recurringPayments) {
            adapter.addItem(recurringPayment)
        }
    }

    override fun onSetTimeClick() {
        closeKeyboard()
        val context = ContextThemeWrapper(this,
                R.style.AssetPopup)
        val popup = PopupMenu(context, binding.dateSet)
        popup.menuInflater.inflate(R.menu.recurring_payment_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            binding.dateSetComplete.visibility = View.VISIBLE
            when (item.itemId) {
                R.id.daily -> {
                    schedule = Schedule.DAILY
                }

                R.id.weekly -> {
                    schedule = Schedule.WEEKLY

                }

                R.id.monthly -> {
                    schedule = Schedule.MONTHLY

                }
            }
            true
        }
        popup.show()

    }

    override fun onAddClick() {
        closeKeyboard()
        var amount = 0.0f;
        try {
            amount = recurringPaymentModel.amount.toFloat()
            if (BRWalletManager.validateAddress(recurringPaymentModel.recipientAddress)) {
                if (amount > 0.0f) {
                    if (schedule != Schedule.NOT_SET) {
                        val recurringPaymentModel = RecurringPayment(
                                recurringPaymentModel.recipientAddress,
                                recurringPaymentModel.amount,
                                schedule.name,
                                recurringPaymentModel.label
                        )
                        recurringPaymentModel.updateNextScheduledRunTime()
                        adapter.addItem(recurringPaymentModel)
                        binding.address.text.clear()
                        binding.amount.text.clear()
                        binding.label.text.clear()
                        binding.dateSetComplete.visibility = View.GONE
                        schedule = Schedule.NOT_SET
                        executor.execute {
                            recurringPaymentModel.id = recurringPaymentModel.save()
                            JobsHelper.scheduleRecurringPayment(recurringPaymentModel)
                            JobsHelper.sendRecurringPaymentSampleNotification(this, recurringPaymentModel)
                        }
                    } else {
                        Toast.makeText(this, R.string.schedule_not_set, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, R.string.Send_invalidAddressTitle, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRecurringPaymentClick(recurringPayment: RecurringPayment) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.cancel_confirm_title)
        builder.setMessage(R.string.cancel_confirm_message)
        builder.setPositiveButton(R.string.yes, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                JobsHelper.cancelRecurringPayment(recurringPayment)
                adapter.removeItem(recurringPayment)
                executor.execute {
                    recurringPayment.delete()
                }
            }
        })
        builder.setNegativeButton(R.string.no, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
            }
        })
        builder.show()
    }

    private fun closeKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.address.getWindowToken(), 0);
    }
}