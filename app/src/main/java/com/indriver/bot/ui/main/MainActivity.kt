package com.indriver.bot.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indriver.bot.R
import com.indriver.bot.databinding.ActivityMainBinding
import com.indriver.bot.databinding.DialogSettingsBinding
import com.indriver.bot.service.BotService
import com.indriver.bot.utils.PermissionHelper
import com.indriver.bot.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)

        setupUI()
        updateUIState()
    }

    private fun setupUI() {
        binding.switchAutoAccept.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasAccessibilityPermission()) {
                binding.switchAutoAccept.isChecked = false
                showPermissionExplanation()
                return@setOnCheckedChangeListener
            }
            toggleBot(isChecked)
        }

        binding.btnSettings.setOnClickListener { showSettingsDialog() }
        binding.btnStats.setOnClickListener { showStatsDialog() }
        binding.btnGrantPermissions.setOnClickListener { requestAccessibilityPermission() }
    }

    private fun toggleBot(enable: Boolean) {
        prefs.setBotEnabled(enable)
        if (enable) {
            BotService.start(this)
            updateStatusRunning()
            Toast.makeText(this, "🟢 Bot Started!", Toast.LENGTH_SHORT).show()
        } else {
            BotService.stop(this)
            updateStatusStopped()
            Toast.makeText(this, "🔴 Bot Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasAccessibilityPermission(): Boolean {
        return PermissionHelper.isAccessibilityEnabled(this)
    }

    private fun updateUIState() {
        val hasPermission = hasAccessibilityPermission()

        if (!hasPermission) {
            if (prefs.isBotEnabled()) {
                prefs.setBotEnabled(false)
                BotService.stop(this)
            }
            binding.switchAutoAccept.isChecked = false
            updateStatusPermissionNeeded()
            binding.btnGrantPermissions.visibility = View.VISIBLE
            binding.cardPermission.visibility = View.VISIBLE
        } else {
            binding.btnGrantPermissions.visibility = View.GONE
            binding.cardPermission.visibility = View.GONE
            val enabled = prefs.isBotEnabled()
            binding.switchAutoAccept.isChecked = enabled
            if (enabled) {
                updateStatusRunning()
            } else {
                updateStatusStopped()
            }
        }

        loadStats()
    }

    private fun updateStatusRunning() {
        binding.tvStatus.text = getString(R.string.status_running)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_500))
    }

    private fun updateStatusStopped() {
        binding.tvStatus.text = getString(R.string.status_stopped)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red_500))
    }

    private fun updateStatusPermissionNeeded() {
        binding.tvStatus.text = getString(R.string.status_permission_needed)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_500))
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun showPermissionExplanation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_accessibility)
            .setMessage(R.string.permission_accessibility_description)
            .setPositiveButton(R.string.grant_permissions) { _, _ -> requestAccessibilityPermission() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadStats() {
        prefs.checkDateReset()
        binding.tvAccepted.text = prefs.getAcceptedCount().toString()
        binding.tvRejected.text = prefs.getMissedCount().toString()
        binding.tvEarnings.text = "$${String.format("%.2f", prefs.getTotalEarnings())}"
    }

    private fun showSettingsDialog() {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)

        dialogBinding.switchAutoBid.isChecked = prefs.isAutoBidEnabled()
        dialogBinding.switchNotifications.isChecked = prefs.areNotificationsEnabled()

        val minPrice = prefs.getMinPrice().toInt()
        dialogBinding.seekMinPrice.progress = minPrice
        dialogBinding.tvMinPriceValue.text = "$$minPrice"

        val maxDistance = prefs.getMaxDistance().toInt()
        dialogBinding.seekMaxDistance.progress = maxDistance
        dialogBinding.tvMaxDistanceValue.text = "$maxDistance km"

        dialogBinding.seekMinPrice.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                dialogBinding.tvMinPriceValue.text = "$$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding.seekMaxDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                dialogBinding.tvMaxDistanceValue.text = "$progress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding.btnManageSideIcon.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("📱 Hide Side Floating Icon")
                .setMessage(
                    "The side floating icon is the Android Accessibility Shortcut.\n\n" +
                    "To hide it:\n" +
                    "1. Tap 'Open Settings' below\n" +
                    "2. Select 'inDriver Bot'\n" +
                    "3. Turn OFF 'inDriver Bot shortcut' (keep the main service ON)\n\n" +
                    "The floating icon will disappear from your screen while the bot continues to work normally!"
                )
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefs.setAutoBidEnabled(dialogBinding.switchAutoBid.isChecked)
                prefs.setNotificationsEnabled(dialogBinding.switchNotifications.isChecked)
                prefs.setMinPrice(dialogBinding.seekMinPrice.progress.toDouble())
                prefs.setMaxDistance(dialogBinding.seekMaxDistance.progress.toDouble())
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showStatsDialog() {
        prefs.checkDateReset()
        val stats = """
            📊 Statistics
            
            ✅ Total Accepted: ${prefs.getAcceptedCount()}
            ❌ Total Missed: ${prefs.getMissedCount()}
            💰 Total Earnings: $${String.format("%.2f", prefs.getTotalEarnings())}
            📅 Today's Accepted: ${prefs.getTodayAccepted()}
            💵 Today's Earnings: $${String.format("%.2f", prefs.getTodayEarnings())}
            📈 Win Rate: ${String.format("%.1f", prefs.getWinRate())}%
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.statistics)
            .setMessage(stats)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton("Reset") { _, _ ->
                prefs.resetStats()
                loadStats()
                Toast.makeText(this, "Stats reset", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }
}
