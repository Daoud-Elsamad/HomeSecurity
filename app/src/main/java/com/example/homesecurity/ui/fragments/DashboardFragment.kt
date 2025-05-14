package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentDashboardBinding
import com.example.homesecurity.models.UserRole
import com.example.homesecurity.ui.adapters.SensorAdapter
import com.example.homesecurity.viewmodels.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var sensorAdapter: SensorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize adapter with default settings first
        setupRecyclerView(true)
        setupSystemStatus()
        
        // Then observe data and user permissions
        observeCurrentUser()
        observeViewModel()
    }

    private fun setupRecyclerView(canControlSensors: Boolean) {
        sensorAdapter = SensorAdapter(
            onLockToggle = { doorId, isLocked ->
                if (canControlSensors) {
                    viewModel.toggleDoorLock(doorId, isLocked)
                }
            },
            onSensorToggle = { sensorId, isEnabled ->
                if (canControlSensors) {
                    viewModel.toggleSensor(sensorId, isEnabled)
                }
            },
            showControls = canControlSensors
        )
        
        binding.sensorsRecyclerView.apply {
            adapter = sensorAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupSystemStatus() {
        // System arming/disarming functionality has been removed
        binding.toggleSystemButton.visibility = View.GONE
        
        // Set fixed status text
        binding.systemStatusText.text = getString(R.string.monitoring)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sensorData.collectLatest { sensors ->
                sensorAdapter.submitList(sensors)
            }
        }
    }
    
    private fun observeCurrentUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                val canControlSensors = user != null && 
                    (user.role == UserRole.ADMIN || 
                    (user.role == UserRole.RESIDENT && user.permissions.canManageSensors))
                
                // Update adapter's control permissions without recreating it
                sensorAdapter.updateControlPermissions(canControlSensors)
                
                // Update welcome message with username
                if (user != null) {
                    binding.welcomeMessageText.text = "Welcome, ${user.username}"
                } else {
                    binding.welcomeMessageText.text = "Welcome"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 