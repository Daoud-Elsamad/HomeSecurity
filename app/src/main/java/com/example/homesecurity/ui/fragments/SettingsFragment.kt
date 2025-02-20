package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentSettingsBinding
import com.example.homesecurity.databinding.DialogAddUserBinding
import com.example.homesecurity.databinding.DialogManageUsersBinding
import com.example.homesecurity.databinding.DialogUserPermissionsBinding
import com.example.homesecurity.viewmodels.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.checkbox.MaterialCheckBox
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupExpandableSettings()
        observeSettings()
        setupSliderListeners()
        setupButtons()
    }

    private fun setupExpandableSettings() {
        binding.apply {
            // Sensor Settings Section
            sensorSettingsCard.setOnClickListener {
                sensorSettingsContent.isVisible = !sensorSettingsContent.isVisible
                sensorSettingsArrow.rotation = if (sensorSettingsContent.isVisible) 180f else 0f
            }

            // Notification Settings Section
            notificationSettingsCard.setOnClickListener {
                notificationSettingsContent.isVisible = !notificationSettingsContent.isVisible
                notificationSettingsArrow.rotation = if (notificationSettingsContent.isVisible) 180f else 0f
            }

            // Admin Settings Section (only visible for admin users)
            adminSettingsCard.setOnClickListener {
                adminSettingsContent.isVisible = !adminSettingsContent.isVisible
                adminSettingsArrow.rotation = if (adminSettingsContent.isVisible) 180f else 0f
            }
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gasThreshold.collect { threshold ->
                binding.gasThresholdSlider.value = threshold.toFloat()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vibrationSensitivity.collect { sensitivity ->
                binding.vibrationSensitivitySlider.value = sensitivity.toFloat()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationEnabled.collect { enabled ->
                binding.notificationSwitch.isChecked = enabled
            }
        }
    }

    private fun setupSliderListeners() {
        binding.apply {
            gasThresholdSlider.addOnChangeListener { _, value, _ ->
                viewModel.updateGasThreshold(value.toDouble())
                gasThresholdValue.text = "Current: $value ppm"
            }

            vibrationSensitivitySlider.addOnChangeListener { _, value, _ ->
                viewModel.updateVibrationSensitivity(value.toDouble())
                vibrationSensitivityValue.text = "Current: $value"
            }
        }
    }

    private fun setupButtons() {
        binding.addUserButton.setOnClickListener {
            showAddUserDialog()
        }

        binding.manageUsersButton.setOnClickListener {
            showManageUsersDialog()
        }

        binding.userPermissionsButton.setOnClickListener {
            showUserPermissionsDialog()
        }
    }

    private fun showAddUserDialog() {
        val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialogBinding.cancelButton.setOnClickListener { dismiss() }
                dialogBinding.addButton.setOnClickListener {
                    val username = dialogBinding.usernameInput.text.toString()
                    val password = dialogBinding.passwordInput.text.toString()
                    val isAdmin = dialogBinding.adminCheckbox.isChecked
                    if (username.isNotBlank() && password.isNotBlank()) {
                        viewModel.addUser(username, password, isAdmin)
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun showManageUsersDialog() {
        val dialogBinding = DialogManageUsersBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialogBinding.doneButton.setOnClickListener { dismiss() }
                // Setup RecyclerView here if needed
                show()
            }
    }

    private fun showUserPermissionsDialog() {
        val dialogBinding = DialogUserPermissionsBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialogBinding.cancelButton.setOnClickListener { dismiss() }
                dialogBinding.saveButton.setOnClickListener {
                    viewModel.updatePermissions(
                        viewLogs = dialogBinding.viewLogsPermission.isChecked,
                        manageSensors = dialogBinding.manageSensorsPermission.isChecked,
                        manageUsers = dialogBinding.manageUsersPermission.isChecked
                    )
                    dismiss()
                }
                show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 