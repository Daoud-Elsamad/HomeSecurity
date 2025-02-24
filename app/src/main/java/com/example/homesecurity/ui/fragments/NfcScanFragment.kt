package com.example.homesecurity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.homesecurity.databinding.FragmentNfcScanBinding
import com.example.homesecurity.viewmodels.NfcViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.nfc.Tag

@AndroidEntryPoint
class NfcScanFragment : Fragment() {
    private var _binding: FragmentNfcScanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NfcViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNfcScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupScanButton()
        observeAccessLogs()
    }

    private fun setupScanButton() {
        binding.scanButton.setOnClickListener {
            // Match system animation duration
            binding.scanButton.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(200)
                .withEndAction {
                    binding.scanButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            val mockCardId = "test_card_123"
            val mockDoorId = "main_door"
            viewModel.validateAccess(mockCardId, mockDoorId)
        }
    }

    private fun observeAccessLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accessLogs.collect { logs ->
                logs.firstOrNull()?.let { lastLog ->
                    // Update access result
                    binding.accessResult.text = if (lastLog.isGranted) 
                        "Access Granted" else "Access Denied"
                    
                    // Update timestamp
                    binding.lastScanTime.text = "Last scan: ${formatTimestamp(lastLog.timestamp)}"
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun handleNfcTag(tag: Tag) {
        viewModel.handleNfcTag(tag)
    }
} 