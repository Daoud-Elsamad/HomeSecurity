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
import com.example.homesecurity.ui.adapters.UserAdapter
import com.example.homesecurity.viewmodels.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
        binding.addUserSection.setOnClickListener {
            // Toggle the form visibility
            binding.addUserFormContent.isVisible = !binding.addUserFormContent.isVisible
            // Rotate arrow based on form visibility
            binding.addUserArrow.rotation = if (binding.addUserFormContent.isVisible) 180f else 0f
            
            // Close other sections if they're open
            if (binding.addUserFormContent.isVisible) {
                binding.manageUsersContent.isVisible = false
                binding.manageUsersArrow.rotation = 0f
                binding.userPermissionsContent.isVisible = false
                binding.userPermissionsArrow.rotation = 0f
            }
        }

        binding.addUserButton.setOnClickListener {
            // Get input values
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            val isAdmin = binding.adminCheckbox.isChecked
            
            if (validateAddUserInputs(username, password)) {
                viewModel.addUser(username, password, isAdmin)
                Snackbar.make(
                    requireView(),
                    "User $username added successfully",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                // Clear the form and hide it
                binding.usernameInput.text?.clear()
                binding.passwordInput.text?.clear()
                binding.adminCheckbox.isChecked = false
                binding.usernameLayout.error = null
                binding.passwordLayout.error = null
                binding.addUserFormContent.isVisible = false
                binding.addUserArrow.rotation = 0f
            }
        }

        binding.manageUsersSection.setOnClickListener {
            // Toggle the content visibility
            binding.manageUsersContent.isVisible = !binding.manageUsersContent.isVisible
            // Rotate arrow based on content visibility
            binding.manageUsersArrow.rotation = if (binding.manageUsersContent.isVisible) 180f else 0f
            
            // Close other sections if they're open
            if (binding.manageUsersContent.isVisible) {
                binding.addUserFormContent.isVisible = false
                binding.addUserArrow.rotation = 0f
                binding.userPermissionsContent.isVisible = false
                binding.userPermissionsArrow.rotation = 0f
                
                // Load users data when opening this section
                setupUsersList()
            }
        }

        binding.userPermissionsSection.setOnClickListener {
            // Toggle the content visibility
            binding.userPermissionsContent.isVisible = !binding.userPermissionsContent.isVisible
            // Rotate arrow based on content visibility
            binding.userPermissionsArrow.rotation = if (binding.userPermissionsContent.isVisible) 180f else 0f
            
            // Close other sections if they're open
            if (binding.userPermissionsContent.isVisible) {
                binding.addUserFormContent.isVisible = false
                binding.addUserArrow.rotation = 0f
                binding.manageUsersContent.isVisible = false
                binding.manageUsersArrow.rotation = 0f
            }
        }
        
        binding.savePermissionsButton.setOnClickListener {
            viewModel.updatePermissions(
                viewLogs = binding.viewLogsPermission.isChecked,
                manageSensors = binding.manageSensorsPermission.isChecked,
                manageUsers = binding.manageUsersPermission.isChecked
            )
            
            Snackbar.make(
                requireView(),
                "User permissions updated successfully",
                Snackbar.LENGTH_SHORT
            ).show()
            
            // Hide the section after saving
            binding.userPermissionsContent.isVisible = false
            binding.userPermissionsArrow.rotation = 0f
        }
    }
    
    private fun setupUsersList() {
        // Setup RecyclerView with adapter
        val userAdapter = UserAdapter()
        binding.usersRecyclerView.adapter = userAdapter
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Observe and update users data 
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collect { users ->
                userAdapter.updateUsers(users)
                
                // Show a helpful message if no users are present
                binding.emptyUsersMessage.isVisible = users.isEmpty()
            }
        }
    }

    private fun validateAddUserInputs(
        username: String,
        password: String
    ): Boolean {
        var isValid = true
        
        // Validate username
        if (username.isBlank()) {
            binding.usernameLayout.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            binding.usernameLayout.error = "Username must be at least 3 characters"
            isValid = false
        } else {
            binding.usernameLayout.error = null
        }
        
        // Validate password
        if (password.isBlank()) {
            binding.passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }
        
        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 