package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.homesecurity.R
import com.example.homesecurity.databinding.FragmentNfcRegistrationBinding
import com.example.homesecurity.models.AccessLevel
import com.example.homesecurity.models.NfcCard
import com.example.homesecurity.viewmodels.NfcViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NfcRegistrationFragment : Fragment() {
    private var _binding: FragmentNfcRegistrationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NfcViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNfcRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRegistrationForm()
        observeRegisteredCards()
    }

    private fun setupRegistrationForm() {
        // Check admin access first
        if (!viewModel.isAdminUser()) {
            showUnauthorizedError()
            findNavController().navigateUp()
            return
        }

        binding.registerButton.setOnClickListener {
            val userId = binding.userIdInput.text.toString()
            if (userId.isBlank()) {
                binding.userIdInput.error = "User ID is required"
                return@setOnClickListener
            }

            val accessLevel = when (binding.accessLevelGroup.checkedRadioButtonId) {
                R.id.radioAdmin -> AccessLevel.ADMIN
                R.id.radioResident -> AccessLevel.RESIDENT
                else -> AccessLevel.GUEST
            }
            
            viewModel.registerCard(userId, accessLevel)
            showSuccessMessage()
        }
    }

    private fun observeRegisteredCards() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registeredCards.collect { cards ->
                updateCardsList(cards)
            }
        }
    }

    private fun showSuccessMessage() {
        Snackbar.make(
            binding.root,
            "NFC Card registered successfully",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showUnauthorizedError() {
        Snackbar.make(
            binding.root,
            "Admin access required",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun updateCardsList(cards: List<NfcCard>) {
        // Update the cards list in the UI
        // This can be implemented when needed for displaying registered cards
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 