package com.aeryz.seimbanginapp.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.aeryz.seimbanginapp.R
import com.aeryz.seimbanginapp.data.network.model.profile.FinanceProfile
import com.aeryz.seimbanginapp.data.network.model.transactionHistory.toTransactionItemList
import com.aeryz.seimbanginapp.databinding.FragmentHomeBinding
import com.aeryz.seimbanginapp.model.TransactionItem
import com.aeryz.seimbanginapp.ui.financialProfile.FinancialProfileActivity
import com.aeryz.seimbanginapp.ui.ocr.OcrActivity
import com.aeryz.seimbanginapp.ui.transaction.createTransaction.CreateTransactionActivity
import com.aeryz.seimbanginapp.ui.transaction.transactionDetail.TransactionDetailActivity
import com.aeryz.seimbanginapp.ui.transaction.transactionHistory.TransactionHistoryActivity
import com.aeryz.seimbanginapp.ui.transaction.transactionHistory.TransactionListAdapter
import com.aeryz.seimbanginapp.ui.widget.CardBalanceWidget
import com.aeryz.seimbanginapp.utils.exception.ApiException
import com.aeryz.seimbanginapp.utils.proceedWhen
import com.aeryz.seimbanginapp.utils.withCurrencyFormat
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        getProfileData()
        observeProfileData()
        setOnClickListener()
        observeInsertTransactionToDatabase()
    }

    override fun onResume() {
        super.onResume()
        getProfileData()
        getTransactionHistory()
    }

    private fun getProfileData() {
        viewModel.getProfileData()
    }

    private fun observeProfileData() {
        viewModel.profileData.observe(viewLifecycleOwner) { resultWrapper ->
            resultWrapper.proceedWhen(
                doOnSuccess = {
                    binding.content.isVisible = true
                    binding.contentState.root.isVisible = false
                    it.payload.let { response ->
                        binding.profileImage.load(response?.profileData?.profilePicture) {
                            crossfade(true)
                            placeholder(R.drawable.profile_image)
                            error(R.drawable.profile_image)
                            transformations(CircleCropTransformation())
                        }
                        val firstName = response?.profileData?.fullName?.substringBefore(" ")
                        binding.tvHelloProfile.text = getString(R.string.text_hi_user, firstName)
                        val balance = response?.profileData?.balance
                        binding.tvBalance.text = withCurrencyFormat(balance)
                        updateWidgetBalance(requireContext(), balance)
                        val financeProfile = response?.profileData?.financeProfile
                        if (financeProfile == null) {
                            binding.cvAdvisor.isVisible = false
                            binding.cvFinancialProfileInfo.isVisible = true
                            binding.cvFinancialProfileInfo.setOnClickListener {
                                navigateToFinancialProfile()
                            }
                        } else {
                            binding.cvFinancialProfileInfo.isVisible = false
                            binding.cvAdvisor.isVisible = true
                            observeAdviseFromAI(financeProfile)
                        }
                    }
                    getTransactionHistory()
                    observeTransactionHistory()
                },
                doOnLoading = {
                    binding.content.isVisible = false
                    binding.contentState.root.isVisible = true
                    binding.contentState.tvError.isVisible = false
                    binding.contentState.pbLoading.isVisible = true
                },
                doOnError = {
                    binding.content.isVisible = false
                    binding.contentState.root.isVisible = true
                    binding.contentState.tvError.isVisible = true
                    binding.contentState.pbLoading.isVisible = false
                    if (it.exception is ApiException) {
                        binding.contentState.tvError.text = it.exception.getParsedError()?.message
                    }
                },
            )
        }
    }

    private fun setupRecyclerView(data: List<TransactionItem>) {
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.rvTransactionList.layoutManager = layoutManager
        val adapter =
            TransactionListAdapter {
                TransactionDetailActivity.startActivity(requireActivity(), it)
            }
        binding.rvTransactionList.adapter = adapter
        adapter.submitData(data)
    }

    private fun getTransactionHistory() {
        viewModel.getTransactionHistory(null, null)
    }

    private fun observeTransactionHistory() {
        viewModel.transactionHistory.observe(viewLifecycleOwner) { result ->
            result.proceedWhen(
                doOnSuccess = {
                    binding.pbLoadingTransactionList.isVisible = false
                    binding.tvTransactionListError.isVisible = false
                    binding.rvTransactionList.isVisible = true
                    it.payload?.let { response ->
                        response.data?.let { data ->
                            val sortedData =
                                data
                                    .toTransactionItemList()
                                    .sortedByDescending { item -> item.createdAt }
                            setupRecyclerView(sortedData.take(5))
                            viewModel.insertListToDatabase(sortedData)
                        }
                    }
                },
                doOnLoading = {
                    binding.pbLoadingTransactionList.isVisible = true
                    binding.tvTransactionListError.isVisible = false
                    binding.rvTransactionList.isVisible = false
                },
                doOnError = {
                    binding.pbLoadingTransactionList.isVisible = false
                    binding.tvTransactionListError.isVisible = true
                    binding.rvTransactionList.isVisible = false
                    if (it.exception is ApiException) {
                        binding.tvTransactionListError.text = it.exception.getParsedError()?.message
                    }
                },
                doOnEmpty = {
                    binding.pbLoadingTransactionList.isVisible = false
                    binding.tvTransactionListError.isVisible = true
                    binding.rvTransactionList.isVisible = false
                    binding.tvTransactionListError.text =
                        getString(R.string.text_transaction_still_empty)
                },
            )
        }
    }

    private fun observeInsertTransactionToDatabase() {
        viewModel.insertListToDatabaseResult.observe(viewLifecycleOwner) { result ->
            result.proceedWhen(
                doOnSuccess = {
//                    Toast.makeText(
//                        requireContext(),
//                        "Update Local Transaction Success",
//                        Toast.LENGTH_SHORT
//                    ).show()
                },
            )
        }
    }

    private fun observeAdviseFromAI(financeProfile: FinanceProfile) {
        binding.icRefreshAdvisor.setOnClickListener {
            viewModel.getAdviseFromAI(financeProfile)
        }
        viewModel.aiAdvisor.observe(viewLifecycleOwner) { resultWrapper ->
            resultWrapper.proceedWhen(
                doOnSuccess = {
                    binding.tvUserAdvise.isVisible = true
                    binding.pbLoadingAdvisor.isVisible = false
                    val adviseResult = it.payload
                    adviseResult?.let { it1 -> viewModel.saveAdvise(it1) }
                },
                doOnLoading = {
                    binding.pbLoadingAdvisor.isVisible = true
                    binding.tvUserAdvise.isVisible = false
                },
                doOnError = {
                    binding.pbLoadingAdvisor.isVisible = false
                    binding.tvUserAdvise.isVisible = true
                    if (it.exception is ApiException) {
                        binding.tvUserAdvise.text =
                            it.exception
                                .getParsedError()
                                ?.message
                                .orEmpty()
                    }
                },
            )
        }
        viewModel.getAdviseFromDb()
        viewModel.localAdvisor.observe(viewLifecycleOwner) { advise ->
            if (advise.isNullOrEmpty()) {
                binding.tvUserAdvise.isVisible = false
            } else {
                binding.tvUserAdvise.isVisible = true
                binding.tvUserAdvise.text = advise
            }
        }
    }

    private fun setOnClickListener() {
        binding.btnAdd.setOnClickListener {
            navigateToCreateTransaction()
        }
        binding.tvSeeAll.setOnClickListener {
            navigateToTransactionHistory()
        }
        binding.btnScan.setOnClickListener {
            navigateToScanTransaction()
        }
    }

    private fun navigateToScanTransaction() {
        val intent = Intent(requireContext(), OcrActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToFinancialProfile() {
        val intent = Intent(requireContext(), FinancialProfileActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCreateTransaction() {
        val intent = Intent(requireContext(), CreateTransactionActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTransactionHistory() {
        val intent = Intent(requireContext(), TransactionHistoryActivity::class.java)
        startActivity(intent)
    }

    private fun updateWidgetBalance(
        context: Context,
        balance: String?,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(context, CardBalanceWidget::class.java),
            )
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.card_balance_widget)
            views.setTextViewText(R.id.tv_balance, withCurrencyFormat(balance))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
