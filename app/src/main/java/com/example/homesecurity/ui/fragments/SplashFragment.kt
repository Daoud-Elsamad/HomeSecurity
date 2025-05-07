package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentSplashBinding
import com.example.homesecurity.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startAnimation()
    }

    private fun startAnimation() {
        // Hide loading initially
        binding.loadingIndicator.alpha = 0f
        
        // Set initial states for icon and logo
        binding.shieldIcon.apply {
            alpha = 0f
            scaleX = 0.2f
            scaleY = 0.2f
        }
        
        binding.logoImage.apply {
            alpha = 0f
            scaleX = 0.2f
            scaleY = 0.2f
        }

        // Animate shield icon
        binding.shieldIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction {
                // Start logo animation after shield
                binding.logoImage.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .withEndAction {
                        // Show loading after logo animation
                        binding.loadingIndicator.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .withEndAction {
                                // Wait 2 seconds with loading visible before checking auth
                                lifecycleScope.launch {
                                    delay(2000) // 2 seconds delay
                                    binding.loadingIndicator.animate()
                                        .alpha(0f)
                                        .setDuration(200)
                                        .withEndAction {
                                            checkAuthAndNavigate()
                                        }
                                        .start()
                                }
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun checkAuthAndNavigate() {
        val currentUser = authViewModel.currentUser.value
        
        if (currentUser != null) {
            // User is already logged in, go to dashboard
            navigateToDashboard()
        } else {
            // User needs to log in
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        findNavController().navigate(
            R.id.action_splashFragment_to_loginFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.splashFragment, true)
                .build()
        )
    }
    
    private fun navigateToDashboard() {
        findNavController().navigate(
            R.id.action_splashFragment_to_dashboardFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.splashFragment, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 