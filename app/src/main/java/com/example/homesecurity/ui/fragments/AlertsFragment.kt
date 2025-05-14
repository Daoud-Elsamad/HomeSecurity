package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.databinding.FragmentAlertsBinding
import com.example.homesecurity.models.Alert
import com.example.homesecurity.models.AlertType
import com.example.homesecurity.ui.adapters.AlertsAdapter
import com.example.homesecurity.viewmodels.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AlertsFragment"

@AndroidEntryPoint
class AlertsFragment : Fragment() {
    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var alertsAdapter: AlertsAdapter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeAlerts()
        
        // Initialize with empty state
        showEmptyState(true)
        
        // For debugging - check if alerts load from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000) // Wait a second to see if alerts load from Firebase
            
            if (alertsAdapter.itemCount == 0) {
                Log.d(TAG, "No alerts found, checking Firebase connection")
                viewModel.logAlertsState()
            }
        }
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter { alertId ->
            // Handle alert click - acknowledge alert
            viewModel.acknowledgeAlert(alertId)
            Toast.makeText(requireContext(), "Alert acknowledged", Toast.LENGTH_SHORT).show()
        }
        
        binding.alertsRecyclerView.apply {
            adapter = alertsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        // Update empty state based on adapter items
        alertsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                val itemCount = alertsAdapter.itemCount
                Log.d(TAG, "Adapter data changed: $itemCount items")
                showEmptyState(itemCount == 0)
            }
            
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                Log.d(TAG, "$itemCount items inserted at position $positionStart")
                showEmptyState(alertsAdapter.itemCount == 0)
            }
        })
    }
    
    private fun setupButtons() {
        // Setup refresh button
        binding.refreshButton.setOnClickListener {
            Log.d(TAG, "Manual refresh triggered")
            viewModel.refreshAlerts()
            Toast.makeText(requireContext(), "Refreshing alerts...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeAlerts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.alerts.collect { alerts ->
                Log.d(TAG, "Received ${alerts.size} alerts from ViewModel")
                
                // Filter to get only unique alerts by originalId
                // For alerts without originalId, keep them all
                val latestAlerts = mutableListOf<Alert>()
                val processedOriginalIds = mutableSetOf<String>()
                
                // Sort alerts by timestamp (newest first)
                val sortedAlerts = alerts.sortedByDescending { it.timestamp }
                
                // First check if we have the special "0" alert from Realtime DB
                val realtimeAlert = sortedAlerts.find { it.originalId == "0" }
                if (realtimeAlert != null) {
                    Log.d(TAG, "Found Realtime DB '0' alert: ${realtimeAlert.id}, message: ${realtimeAlert.message}")
                    latestAlerts.add(realtimeAlert)
                    processedOriginalIds.add("0")
                }
                
                // Then add other unique alerts
                for (alert in sortedAlerts) {
                    if (alert.originalId == "0") continue // Already handled above
                    
                    // Get the originalId from the alert (might be null for older alerts)
                    val originalId = alert.originalId
                    
                    if (originalId == null || !processedOriginalIds.contains(originalId)) {
                        latestAlerts.add(alert)
                        if (originalId != null) {
                            processedOriginalIds.add(originalId)
                        }
                    }
                }
                
                Log.d(TAG, "Filtered down to ${latestAlerts.size} unique alerts")
                
                // Log the alerts for debugging
                latestAlerts.forEachIndexed { index, alert ->
                    Log.d(TAG, "Alert $index: ${alert.id}, type: ${alert.type}, message: ${alert.message}, originalId: ${alert.originalId}")
                }
                
                // Submit the filtered list
                alertsAdapter.submitList(latestAlerts)
                
                // Update UI
                showEmptyState(latestAlerts.isEmpty())
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        Log.d(TAG, "Showing empty state: $show")
        if (show) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.alertsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.alertsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 