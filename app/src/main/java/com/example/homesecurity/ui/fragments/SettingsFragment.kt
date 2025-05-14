package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
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
import com.example.homesecurity.models.User
import com.example.homesecurity.models.UserRole
import com.example.homesecurity.models.UserPermissions
import com.example.homesecurity.ui.adapters.UserAdapter
import com.example.homesecurity.viewmodels.SettingsViewModel
import com.example.homesecurity.viewmodels.UserActionState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.checkbox.MaterialCheckBox
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

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
        setupRoleDropdowns()
        setupUsersList()
        observeUserActionState()
        observeSelectedUser()
        observeCurrentUser()
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
            val roleText = binding.roleDropdown.text.toString()
            
            if (validateAddUserInputs(username, password)) {
                val role = when (roleText) {
                    "Admin" -> UserRole.ADMIN
                    "Resident" -> UserRole.RESIDENT
                    "Guest" -> UserRole.GUEST
                    else -> UserRole.GUEST
                }
                
                viewModel.addUserWithRole(username, password, role)
                
                // Clear the form (will be hidden by observeUserActionState on success)
                binding.usernameInput.text?.clear()
                binding.passwordInput.text?.clear()
                binding.roleDropdown.setText("Guest")
                binding.usernameLayout.error = null
                binding.passwordLayout.error = null
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
                
                // Reset selected user when opening this section
                viewModel.clearSelectedUser()
                userAdapter.selectUser(null)
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
            viewModel.updateSelectedUserPermissions(
                viewLogs = binding.viewLogsPermission.isChecked,
                manageSensors = binding.manageSensorsPermission.isChecked,
                manageUsers = binding.manageUsersPermission.isChecked
            )
        }
        
        binding.deleteUserButton.setOnClickListener {
            // Confirm deletion
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Delete") { dialog, _ ->
                    viewModel.deleteSelectedUser()
                    dialog.dismiss()
                }
                .show()
        }
    }
    
    private fun setupRoleDropdowns() {
        // Setup role dropdown for adding new users
        val roleItems = arrayOf("Guest", "Resident", "Admin")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roleItems)
        binding.roleDropdown.setAdapter(roleAdapter)
        
        // Setup role dropdown for editing users
        val editRoleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roleItems)
        binding.editRoleDropdown.setAdapter(editRoleAdapter)
        
        // Set listener for role changes
        binding.editRoleDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedRole = when (position) {
                0 -> UserRole.GUEST
                1 -> UserRole.RESIDENT
                2 -> UserRole.ADMIN
                else -> UserRole.GUEST
            }
            viewModel.updateSelectedUserRole(selectedRole)
        }
    }
    
    private fun setupUsersList() {
        // Setup RecyclerView with adapter
        userAdapter = UserAdapter(
            onUserSelected = { user ->
                viewModel.selectUser(user.id)
                
                // Open permissions section if it's not already visible
                if (!binding.userPermissionsContent.isVisible) {
                    binding.userPermissionsContent.isVisible = true
                    binding.userPermissionsArrow.rotation = 180f
                    
                    // Close other sections
                    binding.addUserFormContent.isVisible = false
                    binding.addUserArrow.rotation = 0f
                }
            },
            onUserLongPressed = { user, view ->
                // Show popup menu for quick actions
                val popup = PopupMenu(requireContext(), view)
                popup.menuInflater.inflate(R.menu.user_actions_menu, popup.menu)
                
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_delete_user -> {
                            viewModel.selectUser(user.id)
                            // Confirm deletion
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete User")
                                .setMessage("Are you sure you want to delete ${user.username}? This action cannot be undone.")
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton("Delete") { dialog, _ ->
                                    viewModel.deleteSelectedUser()
                                    dialog.dismiss()
                                }
                                .show()
                            true
                        }
                        R.id.action_edit_permissions -> {
                            viewModel.selectUser(user.id)
                            // Open permissions section
                            binding.userPermissionsContent.isVisible = true
                            binding.userPermissionsArrow.rotation = 180f
                            
                            // Close other sections
                            binding.addUserFormContent.isVisible = false
                            binding.addUserArrow.rotation = 0f
                            binding.manageUsersContent.isVisible = false
                            binding.manageUsersArrow.rotation = 0f
                            true
                        }
                        else -> false
                    }
                }
                
                popup.show()
            }
        )
        
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
    
    private fun observeUserActionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userActionState.collectLatest { state ->
                when (state) {
                    is UserActionState.Success -> {
                        Snackbar.make(
                            requireView(),
                            state.message,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        
                        // If adding a user was successful, hide the form
                        if (state.message.contains("added successfully")) {
                            binding.addUserFormContent.isVisible = false
                            binding.addUserArrow.rotation = 0f
                        }
                        
                        // If deleting a user was successful, hide the permissions form
                        if (state.message.contains("deleted successfully")) {
                            binding.userPermissionsContent.isVisible = false
                            binding.userPermissionsArrow.rotation = 0f
                        }
                        
                        // Reset state after handling
                        viewModel.resetUserActionState()
                    }
                    is UserActionState.Error -> {
                        Snackbar.make(
                            requireView(),
                            state.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                        
                        // Reset state after handling
                        viewModel.resetUserActionState()
                    }
                    else -> {
                        // No-op for Idle and Loading states
                    }
                }
            }
        }
    }
    
    private fun observeSelectedUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedUser.collect { user ->
                // Update selected user in the adapter for visual indication
                user?.let {
                    userAdapter.selectUser(it.id)
                    
                    // Update the permission section with user info
                    binding.selectedUserLabel.text = "Editing: ${it.username}"
                    
                    // Set current role in dropdown
                    val roleText = when (it.role) {
                        UserRole.ADMIN -> "Admin"
                        UserRole.RESIDENT -> "Resident"
                        UserRole.GUEST -> "Guest"
                    }
                    binding.editRoleDropdown.setText(roleText, false)
                    
                    // Set current permissions
                    binding.viewLogsPermission.isChecked = it.permissions.canViewLogs
                    binding.manageSensorsPermission.isChecked = it.permissions.canManageSensors
                    binding.manageUsersPermission.isChecked = it.permissions.canManageUsers
                    
                } ?: run {
                    // No user selected
                    binding.selectedUserLabel.text = "Select a user first"
                    binding.editRoleDropdown.setText("", false)
                    binding.viewLogsPermission.isChecked = false
                    binding.manageSensorsPermission.isChecked = false
                    binding.manageUsersPermission.isChecked = false
                }
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

    private fun observeCurrentUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                setupPermissionBasedUI(user)
            }
        }
    }
    
    private fun setupPermissionBasedUI(user: User?) {
        // Default: hide admin settings for non-admin users
        if (user == null || user.role != UserRole.ADMIN) {
            binding.adminSettingsCard.visibility = View.GONE
            binding.addUserSection.visibility = View.GONE
            binding.manageUsersSection.visibility = View.GONE
        } else {
            binding.adminSettingsCard.visibility = View.VISIBLE
            binding.addUserSection.visibility = View.VISIBLE
            binding.manageUsersSection.visibility = View.VISIBLE
        }
        
        // For resident users - they can manage sensors but not users
        if (user?.role == UserRole.RESIDENT) {
            binding.manageUsersSection.visibility = View.GONE
            binding.addUserSection.visibility = View.GONE
            
            // If they only have view logs permission, don't show sensor settings
            if (user.permissions.canViewLogs && !user.permissions.canManageSensors) {
                binding.sensorSettingsCard.visibility = View.GONE
            } else {
                binding.sensorSettingsCard.visibility = View.VISIBLE
            }
        }
        
        // User permissions section is only visible to admins
        binding.userPermissionsSection.visibility = if (user?.role == UserRole.ADMIN) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 