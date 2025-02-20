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
import com.example.homesecurity.MainViewModel
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

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

        // Set initial state
        updateSystemState(false)
        
        // Add logging
        println("Initial state set")

        // Set up system status toggle
        binding.toggleSystemButton.setOnClickListener {
            println("Button clicked")
            viewModel.toggleSystem()
        }

        // Observe system status
        viewModel.systemArmed.observe(viewLifecycleOwner) { isArmed ->
            println("State changed to: $isArmed")
            updateSystemState(isArmed)
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