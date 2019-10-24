package com.woocommerce.android.ui.refunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_issue_refund.*

class IssueRefundFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by viewModels { viewModelFactory }

    private val navArgs: IssueRefundFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_issue_refund, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        initializeViews(viewModel)
        setupObservers(viewModel)
        viewModel.start(navArgs.orderId)
    }

    private fun initializeViews(viewModel: IssueRefundViewModel) {
        issueRefund_btnNext.setOnClickListener {
            viewModel.onRefundEntered()
        }
    }

    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.resetEvents()

        viewModel.screenTitle.observe(this, Observer {
            activity?.title = it
        })

        viewModel.isNextButtonEnabled.observe(this, Observer {
            issueRefund_btnNext.isEnabled = it
        })

        viewModel.showRefundSummary.observe(this, Observer {
            val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundSummaryFragment()
            findNavController().navigate(action)
        })
    }
}
