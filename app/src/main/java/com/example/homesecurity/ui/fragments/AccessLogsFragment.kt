package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.databinding.FragmentAccessLogsBinding
import com.example.homesecurity.models.ActivityLog
import com.example.homesecurity.models.toActivityLog
import com.example.homesecurity.ui.adapters.AccessLogsAdapter
import com.example.homesecurity.viewmodels.DashboardViewModel
import com.example.homesecurity.viewmodels.NfcViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccessLogsFragment : Fragment() {
    private var _binding: FragmentAccessLogsBinding? = null
    private val binding get() = _binding!!
    private val nfcViewModel: NfcViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var logsAdapter: AccessLogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccessLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeLogs()
        
        // Initially show empty state
        showEmptyState(true)
    }

    private fun setupRecyclerView() {
        logsAdapter = AccessLogsAdapter()
        binding.logsRecyclerView.apply {
            adapter = logsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Update empty state based on adapter items
        logsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                showEmptyState(logsAdapter.itemCount == 0)
            }
        })
    }

    private fun observeLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                nfcViewModel.accessLogs,
                dashboardViewModel.alerts
            ) { accessLogs, alerts ->
                val combinedLogs = mutableListOf<ActivityLog>()
                combinedLogs.addAll(accessLogs.map { it.toActivityLog() })
                combinedLogs.addAll(alerts.map { it.toActivityLog() })
                combinedLogs.sortedByDescending { it.timestamp }
            }.collect { logs ->
                logsAdapter.submitList(logs)
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.logsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.logsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 