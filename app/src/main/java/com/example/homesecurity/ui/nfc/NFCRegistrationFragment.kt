package com.example.homesecurity.ui.nfc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.example.homesecurity.databinding.FragmentNfcRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NFCRegistrationFragment : Fragment() {
    
    private var _binding: FragmentNfcRegistrationBinding? = null
    private val binding get() = _binding!!
    private var lastCheckedId: Int? = null
    
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
        setupCards()
        setupClearButton()
    }
    
    private fun setupCards() {
        val cards = listOf(
            binding.adminCard to binding.radioAdmin,
            binding.residentCard to binding.radioResident,
            binding.guestCard to binding.radioGuest
        )

        cards.forEach { (card, radio) ->
            card.setOnClickListener {
                handleCardClick(card, radio)
            }
        }
    }

    private fun handleCardClick(card: MaterialCardView, radioButton: RadioButton) {
        if (radioButton.isChecked) {
            // Uncheck if already checked
            radioButton.isChecked = false
            lastCheckedId = null
            binding.accessLevelGroup.clearCheck()
        } else {
            // Clear previous selection and check this one
            binding.accessLevelGroup.clearCheck()
            radioButton.isChecked = true
            lastCheckedId = radioButton.id
        }
    }

    private fun setupClearButton() {
        binding.clearSelectionButton.setOnClickListener {
            binding.accessLevelGroup.clearCheck()
            lastCheckedId = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 