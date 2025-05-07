package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentLoginBinding
import com.example.homesecurity.viewmodels.AuthViewModel
import com.example.homesecurity.viewmodels.LoginState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupLoginButton()
        observeLoginState()
        observeSystemInitialization()
    }

    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            
            if (validateInputs(username, password)) {
                viewModel.login(username, password)
            }
        }
    }
    
    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true
        
        if (username.isEmpty()) {
            binding.usernameLayout.error = "Username is required"
            isValid = false
        } else {
            binding.usernameLayout.error = null
        }
        
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }
        
        return isValid
    }
    
    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Idle -> {
                        binding.progressBar.isVisible = false
                        binding.errorText.isVisible = false
                    }
                    is LoginState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.errorText.isVisible = false
                    }
                    is LoginState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.errorText.isVisible = false
                        navigateToDashboard()
                    }
                    is LoginState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.errorText.isVisible = true
                        binding.errorText.text = state.message
                    }
                    is LoginState.RequiresPasswordChange -> {
                        binding.progressBar.isVisible = false
                        binding.errorText.isVisible = false
                        showPasswordChangeDialog()
                    }
                }
            }
        }
    }
    
    private fun observeSystemInitialization() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.systemInitialized.collect { initialized ->
                if (initialized == false) {
                    // System not initialized, show first login message
                    binding.firstLoginMessage.isVisible = true
                } else {
                    binding.firstLoginMessage.isVisible = false
                }
            }
        }
    }
    
    private fun showPasswordChangeDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)
        
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.newPasswordInput)
        val confirmInput = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordInput)
        val passwordLayout = dialogView.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmPasswordLayout)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Default Password")
            .setMessage("For security reasons, you need to change the default password before continuing.")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Change Password") { _, _ ->
                val newPassword = passwordInput.text.toString()
                val confirmPassword = confirmInput.text.toString()
                
                if (newPassword.length < 6) {
                    passwordLayout.error = "Password must be at least 6 characters"
                } else if (newPassword != confirmPassword) {
                    confirmLayout.error = "Passwords don't match"
                } else {
                    viewModel.updateDefaultPassword(newPassword)
                }
            }
            .create()
            
        dialog.show()
        
        // Set dialog width to 90% of screen width
        val window = dialog.window
        window?.let {
            val params = WindowManager.LayoutParams()
            params.copyFrom(it.attributes)
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            it.attributes = params
        }
    }
    
    private fun navigateToDashboard() {
        findNavController().navigate(
            R.id.action_loginFragment_to_dashboardFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 