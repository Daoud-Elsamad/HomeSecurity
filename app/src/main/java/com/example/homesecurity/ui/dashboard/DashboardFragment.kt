package com.example.homesecurity.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homesecurity.databinding.FragmentDashboardBinding
import com.example.homesecurity.ui.adapters.SensorAdapter
import com.example.homesecurity.viewmodels.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
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
        
        // Initialize the sensor adapter
        setupRecyclerView()
        
        // Observe sensor data from DashboardViewModel
        observeSensorData()
        
        // System arming/disarming functionality has been removed
        // Hide the toggle button
        binding.toggleSystemButton.visibility = View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 