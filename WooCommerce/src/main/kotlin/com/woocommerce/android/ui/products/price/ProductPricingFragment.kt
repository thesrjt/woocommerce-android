package com.woocommerce.android.ui.products.price

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.databinding.FragmentProductPricingBinding
import com.woocommerce.android.extensions.capitalize
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.ui.products.BaseProductEditorFragment
import com.woocommerce.android.ui.products.ProductItemSelectorDialog
import com.woocommerce.android.ui.products.ProductItemSelectorDialog.ProductItemSelectorDialogListener
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.WCMaterialOutlinedSpinnerView
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class ProductPricingFragment :
    BaseProductEditorFragment(R.layout.fragment_product_pricing), ProductItemSelectorDialogListener {
    companion object {
        private const val SUBSCRIPTION_INTERVAL_ITEMS_COUNT = 6
    }

    private val viewModel: ProductPricingViewModel by viewModels()

    override val lastEvent: Event?
        get() = viewModel.event.value

    private var productTaxStatusSelectorDialog: ProductItemSelectorDialog? = null
    private var productTaxClassSelectorDialog: ProductItemSelectorDialog? = null

    private var startDatePickerDialog: DatePickerDialog? = null
    private var endDatePickerDialog: DatePickerDialog? = null

    private var _binding: FragmentProductPricingBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var dateUtils: DateUtils

    override fun onPause() {
        super.onPause()
        productTaxStatusSelectorDialog?.dismiss()
        productTaxStatusSelectorDialog = null

        productTaxClassSelectorDialog?.dismiss()
        productTaxClassSelectorDialog = null

        startDatePickerDialog?.dismiss()
        startDatePickerDialog = null

        endDatePickerDialog?.dismiss()
        endDatePickerDialog = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductPricingBinding.bind(view)
        initSubscriptionViews()
        setupObservers(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getFragmentTitle() = getString(R.string.product_price)

    private fun setupObservers(viewModel: ProductPricingViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.currency?.takeIfNotEqualTo(old?.currency) {
                setupViews(new.currency, new.isCurrencyPrefix, viewModel.pricingData)
            }
            new.taxClassList?.takeIfNotEqualTo(old?.taxClassList) {
                updateProductTaxClassList(it, viewModel.pricingData)
            }
            new.pricingData.saleStartDate?.takeIfNotEqualTo(old?.pricingData?.saleStartDate) {
                binding.scheduleSaleStartDate.setText(it.formatToMMMddYYYY())
            }
            new.pricingData.saleEndDate?.takeIfNotEqualTo(old?.pricingData?.saleEndDate) {
                binding.scheduleSaleEndDate.setText(it.formatToMMMddYYYY())
            }
            new.isRemoveEndDateButtonVisible.takeIfNotEqualTo(old?.isRemoveEndDateButtonVisible) { isVisible ->
                binding.scheduleSaleRemoveEndDateButton.visibility = if (isVisible) View.VISIBLE else {
                    binding.scheduleSaleEndDate.setText("")
                    View.GONE
                }
            }
            new.isTaxSectionVisible?.takeIfNotEqualTo(old?.isTaxSectionVisible) { isVisible ->
                if (isVisible) {
                    binding.productTaxSection.show()
                } else {
                    binding.productTaxSection.hide()
                }
            }
            new.salePriceErrorMessage?.takeIfNotEqualTo(old?.salePriceErrorMessage) { displaySalePriceError(it) }
            new.pricingData.isSubscription.takeIfNotEqualTo(old?.pricingData?.isSubscription) {
                binding.subscriptionGroup.isVisible = it
            }
            new.pricingData.subscriptionInterval?.takeIfNotEqualTo(old?.pricingData?.subscriptionInterval) { interval ->
                binding.subscriptionInterval.setText(interval.formatSubscriptionInterval())
                updateSubscriptionSaleHelperText()
                // Refresh the period spinner to fix localization if needed
                new.pricingData.subscriptionPeriod?.let {
                    binding.subscriptionPeriod.setText(it.format(interval))
                }
                setupSubscriptionPeriodSpinner()
            }
            new.pricingData.subscriptionPeriod?.takeIfNotEqualTo(old?.pricingData?.subscriptionPeriod) {
                binding.subscriptionPeriod.setText(it.format(new.pricingData.subscriptionInterval))
                updateSubscriptionSaleHelperText()
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(KEY_PRICING_DIALOG_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun setupViews(currency: String, isCurrencyPrefix: Boolean, pricingData: PricingData) {
        if (!isAdded) return

        with(binding.productRegularPrice) {
            if (isCurrencyPrefix) {
                prefixText = currency
            } else suffixText = currency

            pricingData.regularPrice?.let { text = it.toString() }
            setOnTextChangedListener {
                val price = it.toString().toBigDecimalOrNull()
                viewModel.onRegularPriceEntered(price)
            }
        }

        with(binding.subscriptionSignupFee) {
            if (isCurrencyPrefix) {
                prefixText = currency
            } else suffixText = currency

            pricingData.subscriptionSignUpFee?.let { text = it.toString() }
            setOnTextChangedListener {
                val signupFee = it.toString().toBigDecimalOrNull()
                viewModel.onDataChanged(subscriptionSignupFee = signupFee)
            }
        }

        with(binding.productSalePrice) {
            if (isCurrencyPrefix) {
                prefixText = currency
            } else suffixText = currency

            pricingData.salePrice?.let { text = it.toString() }
            setOnTextChangedListener {
                val price = it.toString().toBigDecimalOrNull()
                viewModel.onSalePriceEntered(price)
            }
        }

        val scheduleSale = pricingData.isSaleScheduled == true

        enableScheduleSale(scheduleSale)
        with(binding.scheduleSaleSwitch) {
            isChecked = scheduleSale
            setOnCheckedChangeListener { _, isChecked ->
                enableScheduleSale(isChecked)
                viewModel.onScheduledSaleChanged(isChecked)
            }
        }

        updateSaleStartDate(pricingData.saleStartDate, pricingData.saleEndDate)
        with(binding.scheduleSaleStartDate) {
            setClickListener {
                startDatePickerDialog = displayDatePickerDialog(
                    binding.scheduleSaleStartDate,
                    OnDateSetListener { _, selectedYear, selectedMonth, dayOfMonth ->
                        val selectedDate = dateUtils.getDateAtStartOfDay(selectedYear, selectedMonth, dayOfMonth)

                        viewModel.onDataChanged(saleStartDate = selectedDate)
                    }
                )
            }
        }

        updateSaleEndDate(pricingData.saleEndDate)
        with(binding.scheduleSaleEndDate) {
            setClickListener {
                endDatePickerDialog = displayDatePickerDialog(
                    binding.scheduleSaleEndDate,
                    OnDateSetListener { _, selectedYear, selectedMonth, dayOfMonth ->
                        val selectedDate = dateUtils.getDateAtStartOfDay(selectedYear, selectedMonth, dayOfMonth)

                        viewModel.onDataChanged(saleEndDate = selectedDate)
                    }
                )
            }
        }

        with(binding.scheduleSaleRemoveEndDateButton) {
            setOnClickListener {
                viewModel.onRemoveEndDateClicked()
            }
        }

        pricingData.taxStatus?.let { status ->
            with(binding.productTaxStatus) {
                setText(ProductTaxStatus.taxStatusToDisplayString(requireContext(), status))
                setClickListener {
                    productTaxStatusSelectorDialog = ProductItemSelectorDialog.newInstance(
                        this@ProductPricingFragment, RequestCodes.PRODUCT_TAX_STATUS,
                        getString(string.product_tax_status), ProductTaxStatus.toMap(requireContext()),
                        getText()
                    ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
                }
            }
        }
    }

    override fun onExit() {
        viewModel.onExit()
    }

    /**
     * Method to update the start date of a sale
     *
     * If the [selectedStartDate] is empty or null, then the default is set to the current date,
     * only if the [endDate] > the current date.
     *
     * The [viewModel] is only updated if the [selectedStartDate] is not null. This is to prevent
     * the discard dialog from being displayed when there have been no user initiated changes made
     * to the screen.
     */
    private fun updateSaleStartDate(selectedStartDate: Date?, endDate: Date?) {
        val currentDate = Date()
        val date = selectedStartDate
            ?: if (endDate?.after(currentDate) == true) {
                currentDate
            } else null

        date?.let { binding.scheduleSaleStartDate.setText(it.formatForDisplay()) }
        selectedStartDate?.let { viewModel.onDataChanged(saleStartDate = it) }
    }

    private fun updateSaleEndDate(selectedDate: Date?) {
        // The end sale date is optional => null is a valid value
        if (selectedDate != null) {
            binding.scheduleSaleEndDate.setText(selectedDate.formatForDisplay())
        } else {
            binding.scheduleSaleEndDate.setText("")
        }
        viewModel.onDataChanged(saleEndDate = selectedDate)
    }

    private fun updateProductTaxClassList(taxClassList: List<TaxClass>?, pricingData: PricingData) {
        val taxClass = viewModel.getTaxClassBySlug(pricingData.taxClass ?: Product.TAX_CLASS_DEFAULT)
        val name = taxClass?.name
        if (!isAdded || name == null) return

        binding.productTaxClass.setText(name)
        taxClassList?.let { taxClasses ->
            binding.productTaxClass.setClickListener {
                productTaxClassSelectorDialog = ProductItemSelectorDialog.newInstance(
                    this@ProductPricingFragment,
                    RequestCodes.PRODUCT_TAX_CLASS,
                    getString(string.product_tax_class),
                    taxClasses.map { it.slug to it.name }.toMap(),
                    binding.productTaxClass.getText()
                ).also { it.show(parentFragmentManager, ProductItemSelectorDialog.TAG) }
            }
        }
    }

    private fun displaySalePriceError(messageId: Int) {
        if (messageId != 0) {
            binding.productSalePrice.error = getString(messageId)
        } else {
            binding.productSalePrice.error = null
        }
    }

    private fun enableScheduleSale(scheduleSale: Boolean) {
        if (scheduleSale) {
            binding.scheduleSaleMorePanel.expand()
        } else {
            binding.scheduleSaleMorePanel.collapse()
        }
    }

    private fun displayDatePickerDialog(
        spinnerEditText: WCMaterialOutlinedSpinnerView,
        dateSetListener: OnDateSetListener
    ): DatePickerDialog {
        val dateString = if (spinnerEditText.getText().isNotBlank()) {
            dateUtils.formatToYYYYmmDD(spinnerEditText.getText())
        } else {
            dateUtils.formatToYYYYmmDD(Date().formatToMMMddYYYY())
        }
        val (year, month, day) = dateString?.split("-").orEmpty()
        val datePicker = DatePickerDialog(
            requireActivity(), dateSetListener, year.toInt(), month.toInt() - 1, day.toInt()
        )

        datePicker.show()
        return datePicker
    }

    private fun initSubscriptionViews() {
        setupSubscriptionIntervalSpinner()
        setupSubscriptionPeriodSpinner()
    }

    private fun setupSubscriptionIntervalSpinner() {
        binding.subscriptionInterval.setup(
            values = Array(SUBSCRIPTION_INTERVAL_ITEMS_COUNT) { it + 1 },
            onSelected = { viewModel.onDataChanged(subscriptionInterval = it) },
            mapper = { entry -> entry.formatSubscriptionInterval() }
        )
    }

    private fun setupSubscriptionPeriodSpinner() {
        binding.subscriptionPeriod.setup(
            arrayOf(
                SubscriptionPeriod.Day,
                SubscriptionPeriod.Week,
                SubscriptionPeriod.Month,
                SubscriptionPeriod.Year
            ),
            onSelected = { viewModel.onDataChanged(subscriptionPeriod = it) },
            mapper = { it.format(viewModel.pricingData.subscriptionInterval) }
        )
    }

    private fun updateSubscriptionSaleHelperText() {
        val interval = viewModel.pricingData.subscriptionInterval
        val period = viewModel.pricingData.subscriptionPeriod

        if (interval == null || period == null) {
            binding.productSalePrice.helperText = null
            return
        }
        binding.productSalePrice.helperText = period.formatWithInterval(requireContext(), interval)
    }

    /**
     * Formats the given [date] or the current date if it's null to `'MMM dd, YYYY'`
     */
    private fun Date?.formatForDisplay(): String {
        val date = this ?: Date()
        return date.formatToMMMddYYYY()
    }

    private fun Int.formatSubscriptionInterval() =
        getString(R.string.subscription_period_interval_single, this.toString())

    private fun SubscriptionPeriod.format(interval: Int?) =
        interval?.let { getPeriodString(requireContext(), interval) }.orEmpty().capitalize()

    override fun onProductItemSelected(resultCode: Int, selectedItem: String?) {
        when (resultCode) {
            RequestCodes.PRODUCT_TAX_STATUS -> {
                selectedItem?.let {
                    binding.productTaxStatus.setText(getString(ProductTaxStatus.toStringResource(it)))
                    viewModel.onDataChanged(taxStatus = ProductTaxStatus.fromString(it))
                }
            }

            RequestCodes.PRODUCT_TAX_CLASS -> {
                selectedItem?.let { selectedTaxClass ->
                    // Fetch the display name of the selected tax class slug
                    val selectedProductTaxClass = viewModel.getTaxClassBySlug(selectedTaxClass)
                    selectedProductTaxClass?.let {
                        binding.productTaxClass.setText(it.name)
                        viewModel.onDataChanged(taxClass = it.slug)
                    }
                }
            }
        }
    }
}
