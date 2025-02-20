package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.homesecurity.databinding.FragmentFloorPlanBinding
import com.example.homesecurity.models.SensorData
import com.example.homesecurity.viewmodels.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FloorPlanFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentFloorPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFloorPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.floorPlanView.setOnSensorClickListener { sensor ->
            showSensorDetails(sensor)
        }
        observeSensorData()
    }

    private fun observeSensorData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sensorData.collectLatest { sensors ->
                binding.floorPlanView.updateSensors(sensors)
            }
        }
    }

    private fun showSensorDetails(sensor: SensorData) {
        val message = """
            Location: ${sensor.location}
            Type: ${sensor.type}
            Status: ${sensor.status}
            Value: ${sensor.value}
        """.trimIndent()
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sensor Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 