package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.nfc.Tag
import com.example.homesecurity.utils.NfcStatus
import com.example.homesecurity.utils.ReaderStatus
import com.example.homesecurity.utils.AccessState

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
        
        setupNfcScanning()
        observeNfcStatus()
        setupUI()
        setupCardManagement()
        observeNfcComponents()
        setupTouchHandling()
    }

    private fun setupNfcScanning() {
        binding.registerButton.setOnClickListener {
            viewModel.startScanning()
        }
    }

    private fun observeNfcStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.nfcStatus.collect { status ->
                updateUIForNfcStatus(status)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scannedCard.collect { card ->
                card?.let { updateUIWithCardInfo(it) }
            }
        }
    }

    private fun observeNfcComponents() {
        // Existing NFC status observation
        
        // Add Reader Status observation
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.readerStatus.collect { status ->
                updateReaderStatus(status)
            }
        }

        // Add Access State observation
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accessState.collect { state ->
                updateAccessState(state)
            }
        }
    }

    private fun updateUIForNfcStatus(status: NfcStatus) {
        binding.apply {
            when (status) {
                NfcStatus.NOT_SUPPORTED -> {
                    nfcStatusText.text = "NFC not supported on this device"
                    registerButton.isEnabled = false
                }
                NfcStatus.DISABLED -> {
                    nfcStatusText.text = "Please enable NFC in settings"
                    registerButton.isEnabled = false
                }
                NfcStatus.SCANNING -> {
                    nfcStatusText.text = "Hold NFC card near device"
                    registerButton.isEnabled = false
                }
                NfcStatus.SUCCESS -> {
                    nfcStatusText.text = "Card scanned successfully"
                    registerButton.isEnabled = true
                }
                NfcStatus.ERROR -> {
                    nfcStatusText.text = "Error scanning card"
                    registerButton.isEnabled = true
                }
                else -> {}
            }
        }
    }

    private fun updateUIWithCardInfo(card: NfcCard) {
        binding.apply {
            nfcCardIdText.text = "Card ID: ${card.id}"
            registrationFormLayout.visibility = View.VISIBLE
        }
    }

    private fun setupTouchHandling() {
        // Set up click listeners for parent layouts to dismiss keyboard
        binding.root.setOnClickListener {
            hideKeyboard()
            binding.root.requestFocus()
        }

        // Clear focus when touching outside of EditText
        binding.userIdInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = requireActivity().currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            binding.root.requestFocus() // Request focus to the root layout
        } else {
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
            binding.root.requestFocus()
        }
    }

    private fun setupUI() {
        // Check admin access first
        if (!viewModel.isAdminUser()) {
            showUnauthorizedError()
            findNavController().navigateUp()
            return
        }

        // Setup clear selection button
        binding.clearSelectionButton.setOnClickListener {
            binding.accessLevelGroup.clearCheck()
        }

        // Setup input field behavior
        binding.userIdInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                binding.registerButton.performClick()
                true
            } else {
                false
            }
        }

        // Setup radio buttons
        binding.apply {
            // Make radio buttons individually clickable
            radioAdmin.setOnClickListener {
                if (radioAdmin.isChecked) {
                    accessLevelGroup.check(R.id.radioAdmin)
                } else {
                    accessLevelGroup.clearCheck()
                }
            }

            radioResident.setOnClickListener {
                if (radioResident.isChecked) {
                    accessLevelGroup.check(R.id.radioResident)
                } else {
                    accessLevelGroup.clearCheck()
                }
            }

            radioGuest.setOnClickListener {
                if (radioGuest.isChecked) {
                    accessLevelGroup.check(R.id.radioGuest)
                } else {
                    accessLevelGroup.clearCheck()
                }
            }
        }

        binding.registerButton.setOnClickListener {
            val userId = binding.userIdInput.text.toString()
            if (userId.isBlank()) {
                binding.userIdInput.error = "User ID is required"
                return@setOnClickListener
            }

            hideKeyboard()

            val accessLevel = when (binding.accessLevelGroup.checkedRadioButtonId) {
                R.id.radioAdmin -> AccessLevel.ADMIN
                R.id.radioResident -> AccessLevel.RESIDENT
                else -> AccessLevel.GUEST
            }
            
            viewModel.registerCard(userId, accessLevel)
            showSuccessMessage()
        }
    }

    private fun setupCardManagement() {
        binding.apply {
            deactivateButton.setOnClickListener {
                viewModel.scannedCard.value?.let { card ->
                    showDeactivateConfirmation(card)
                }
            }

            completeRegistrationButton.setOnClickListener {
                val userId = userIdInput.text.toString()
                val accessLevel = when (accessLevelGroup.checkedRadioButtonId) {
                    R.id.radioAdmin -> AccessLevel.ADMIN
                    R.id.radioResident -> AccessLevel.RESIDENT
                    else -> AccessLevel.GUEST
                }
                viewModel.completeRegistration(userId, accessLevel)
            }
        }
    }

    private fun showDeactivateConfirmation(card: NfcCard) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Deactivate Card")
            .setMessage("Are you sure you want to deactivate this card?")
            .setPositiveButton("Deactivate") { _, _ ->
                viewModel.deactivateCard(card.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun updateReaderStatus(status: ReaderStatus) {
        binding.apply {
            readerStatusText.text = when(status) {
                is ReaderStatus.Active -> "Reader Online"
                is ReaderStatus.Inactive -> "Reader Offline"
                is ReaderStatus.Error -> "Reader Error: ${status.message}"
                ReaderStatus.Unknown -> "Checking Reader Status..."
            }
            
            readerStatusIcon.setImageResource(when(status) {
                is ReaderStatus.Active -> R.drawable.ic_status_active
                is ReaderStatus.Inactive -> R.drawable.ic_status_inactive
                is ReaderStatus.Error -> R.drawable.ic_status_error
                ReaderStatus.Unknown -> R.drawable.ic_status_unknown
            })
        }
    }

    private fun updateAccessState(state: AccessState) {
        binding.apply {
            accessStatusText.text = when(state) {
                is AccessState.Granted -> "Access Granted"
                is AccessState.Denied -> "Access Denied: ${state.reason}"
                is AccessState.Checking -> "Checking Access..."
                AccessState.Unknown -> ""
            }
        }
    }

    fun handleNfcTag(tag: Tag) {
        viewModel.handleNfcTag(tag)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 