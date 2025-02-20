package com.example.homesecurity.ui.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentDashboardBinding
import com.example.homesecurity.ui.adapters.SensorAdapter
import com.example.homesecurity.viewmodels.DashboardViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.view.animation.OvershootInterpolator

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val sensorAdapter = SensorAdapter(
        onLockToggle = { doorId, isLocked ->
            viewModel.toggleDoorLock(doorId, isLocked)
        },
        onSensorToggle = { sensorId, isEnabled ->
            viewModel.toggleSensor(sensorId, isEnabled)
        }
    )

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
        
        setupRecyclerView()
        setupSystemStatus()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.sensorsRecyclerView.apply {
            adapter = sensorAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupSystemStatus() {
        binding.toggleSystemButton.apply {
            setOnClickListener {
                viewModel.toggleSystemArmed()
                // Add ripple effect
                animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        }

        // Observe system armed state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSystemArmed.collectLatest { isArmed ->
                updateSystemStatus(isArmed)
            }
        }
    }

    private fun updateSystemStatus(isArmed: Boolean) {
        binding.apply {
            // Update button text and style
            toggleSystemButton.apply {
                text = if (isArmed) getString(R.string.disarm_system) else getString(R.string.arm_system)
                setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        if (isArmed) R.color.system_armed else R.color.system_disarmed
                    )
                )
                // Add elevation for 3D effect
                elevation = resources.getDimension(R.dimen.button_elevation)
            }

            // Update status text with fade and scale animation
            systemStatusText.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction {
                    systemStatusText.text = if (isArmed) "ARMED" else "DISARMED"
                    systemStatusText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isArmed) R.color.system_armed else R.color.system_disarmed
                        )
                    )
                    systemStatusText.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator(1.2f))
                        .start()
                }
                .start()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sensorData.collectLatest { sensors ->
                sensorAdapter.submitList(sensors)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 