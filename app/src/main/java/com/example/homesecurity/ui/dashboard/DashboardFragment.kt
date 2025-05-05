package com.example.homesecurity.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homesecurity.MainViewModel
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentDashboardBinding
import com.example.homesecurity.ui.adapters.SensorAdapter
import com.example.homesecurity.viewmodels.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
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

        // Set initial state from MainViewModel
        updateSystemState(mainViewModel.systemArmed.value ?: false)
        
        // Initialize the sensor adapter
        setupRecyclerView()
        
        // Observe sensor data from DashboardViewModel
        observeSensorData()
        
        // Add logging
        println("Initial state set")

        // Set up system status toggle
        binding.toggleSystemButton.setOnClickListener {
            println("Button clicked")
            mainViewModel.toggleSystem()
        }

        // Observe system status from MainViewModel
        mainViewModel.systemArmed.observe(viewLifecycleOwner) { isArmed ->
            println("State changed to: $isArmed")
            updateSystemState(isArmed)
            // Also update the DashboardViewModel
            dashboardViewModel.toggleSystemArmed()
        }
    }
    
    private fun setupRecyclerView() {
        sensorAdapter = SensorAdapter(
            onLockToggle = { sensorId, isLocked ->
                dashboardViewModel.toggleDoorLock(sensorId, isLocked)
            },
            onSensorToggle = { sensorId, isEnabled ->
                dashboardViewModel.toggleSensor(sensorId, isEnabled)
            }
        )
        
        binding.sensorsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sensorAdapter
        }
    }
    
    private fun observeSensorData() {
        // Use viewLifecycleOwner to avoid leaks
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.sensorData.collect { sensors ->
                println("Received ${sensors.size} sensors from Firebase")
                sensorAdapter.submitList(sensors)
            }
        }
    }

    private fun updateSystemState(isArmed: Boolean) {
        binding.apply {
            // Update status text and color
            systemStatusText.text = if (isArmed) "ARMED" else "DISARMED"
            systemStatusText.setTextColor(
                ContextCompat.getColor(requireContext(), 
                    if (isArmed) R.color.system_armed else R.color.system_disarmed)
            )

            // Update active status text and color
            systemActiveStatus.text = if (isArmed) "●  Active" else "●  Not Active"
            systemActiveStatus.setTextColor(
                ContextCompat.getColor(requireContext(),
                    if (isArmed) R.color.system_armed else R.color.system_disarmed)
            )

            // Update button text
            toggleSystemButton.text = if (isArmed) "DISARM SYSTEM" else "ARM SYSTEM"
            
            // Update button color
            toggleSystemButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                    if (isArmed) R.color.system_disarmed else R.color.system_armed)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 