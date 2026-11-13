package com.indriver.bot.ui.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
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
            Toast.makeText(this, "🟢 تم تشغيل البوت!", Toast.LENGTH_SHORT).show()
        } else {
            BotService.stop(this)
            updateStatusStopped()
            Toast.makeText(this, "🔴 تم إيقاف البوت", Toast.LENGTH_SHORT).show()
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
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadStats() {
        prefs.checkDateReset()
        binding.tvAccepted.text = prefs.getAcceptedCount().toString()
        binding.tvRejected.text = prefs.getMissedCount().toString()
        binding.tvEarnings.text = "${String.format("%.2f", prefs.getTotalEarnings())} د.م."
    }

    private fun showSettingsDialog() {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)

        dialogBinding.switchAutoBid.isChecked = prefs.isAutoBidEnabled()
        dialogBinding.switchNotifications.isChecked = prefs.areNotificationsEnabled()

        val currentMinPrice = prefs.getMinPrice()
        val currentMaxDistance = prefs.getMaxDistance()

        dialogBinding.etMinPrice.setText(if (currentMinPrice > 0) currentMinPrice.toInt().toString() else "0")
        dialogBinding.tvMinPriceValue.text = "${currentMinPrice.toInt()} د.م."
        dialogBinding.seekMinPrice.progress = currentMinPrice.toInt().coerceIn(0, 200)

        dialogBinding.etMaxDistance.setText(if (currentMaxDistance > 0) currentMaxDistance.toInt().toString() else "10")
        dialogBinding.tvMaxDistanceValue.text = "${currentMaxDistance.toInt()} كم"
        dialogBinding.seekMaxDistance.progress = currentMaxDistance.toInt().coerceIn(0, 50)

        var isUpdatingPriceFromSeek = false
        dialogBinding.seekMinPrice.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    isUpdatingPriceFromSeek = true
                    dialogBinding.etMinPrice.setText(progress.toString())
                    dialogBinding.tvMinPriceValue.text = "$progress د.م."
                    isUpdatingPriceFromSeek = false
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding.etMinPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingPriceFromSeek) {
                    val price = s?.toString()?.toIntOrNull() ?: 0
                    dialogBinding.tvMinPriceValue.text = "$price د.م."
                    if (price in 0..200) {
                        dialogBinding.seekMinPrice.progress = price
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        var isUpdatingDistanceFromSeek = false
        dialogBinding.seekMaxDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    isUpdatingDistanceFromSeek = true
                    dialogBinding.etMaxDistance.setText(progress.toString())
                    dialogBinding.tvMaxDistanceValue.text = "$progress كم"
                    isUpdatingDistanceFromSeek = false
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogBinding.etMaxDistance.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingDistanceFromSeek) {
                    val dist = s?.toString()?.toIntOrNull() ?: 0
                    dialogBinding.tvMaxDistanceValue.text = "$dist كم"
                    if (dist in 0..50) {
                        dialogBinding.seekMaxDistance.progress = dist
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.btnManageSideIcon.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("📱 إخفاء الأيقونة الجانبية العائمة")
                .setMessage(
                    "هذه الأيقونة العائمة على جانب الشاشة هي اختصار إمكانية الوصول في أندرويد.\n\n" +
                    "لإخفائها بسهولة:\n" +
                    "1. اضغط على 'فتح الإعدادات' بالأسفل\n" +
                    "2. اختر 'بوت إن درايف' (inDriver Bot)\n" +
                    "3. أوقف خيار 'اختصار بوت إن درايف' (واترك الخدمة الأساسية مفعلة)\n\n" +
                    "ستختفي الأيقونة العائمة فوراً وسيستمر البوت في العمل بشكل طبيعي!"
                )
                .setPositiveButton("فتح الإعدادات") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(dialogBinding.root)
            .setPositiveButton("حفظ") { _, _ ->
                val minPrice = dialogBinding.etMinPrice.text?.toString()?.toDoubleOrNull()
                    ?: dialogBinding.seekMinPrice.progress.toDouble()
                val maxDist = dialogBinding.etMaxDistance.text?.toString()?.toDoubleOrNull()
                    ?: dialogBinding.seekMaxDistance.progress.toDouble()

                prefs.setAutoBidEnabled(dialogBinding.switchAutoBid.isChecked)
                prefs.setNotificationsEnabled(dialogBinding.switchNotifications.isChecked)
                prefs.setMinPrice(minPrice)
                prefs.setMaxDistance(maxDist)

                Toast.makeText(
                    this,
                    "✅ تم حفظ الإعدادات: أدنى سعر $minPrice د.م.، أقصى مسافة $maxDist كم",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showStatsDialog() {
        prefs.checkDateReset()
        val stats = """
            📊 إحصائيات الأداء والأرباح
            
            ✅ الطلبات المقبولة: ${prefs.getAcceptedCount()}
            ❌ الطلبات المتجاهلة: ${prefs.getMissedCount()}
            💰 إجمالي الأرباح: ${String.format("%.2f", prefs.getTotalEarnings())} د.م.
            📅 مقبولة اليوم: ${prefs.getTodayAccepted()}
            💵 أرباح اليوم: ${String.format("%.2f", prefs.getTodayEarnings())} د.م.
            📈 نسبة القبول: ${String.format("%.1f", prefs.getWinRate())}%
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.statistics)
            .setMessage(stats)
            .setPositiveButton("حسناً", null)
            .setNeutralButton("تصفير العداد") { _, _ ->
                prefs.resetStats()
                loadStats()
                Toast.makeText(this, "تم تصفير الإحصائيات", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }
}
