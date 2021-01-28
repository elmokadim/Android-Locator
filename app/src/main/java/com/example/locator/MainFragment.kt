package com.example.locator

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import dev.mokadim.locator.Locator
import dev.mokadim.locator.Locator.Companion.LocationStatus
import dev.mokadim.locator.Locator.Companion.PERMISSION_PERMANENTLY_REJECTED
import dev.mokadim.locator.Locator.Companion.PERMISSION_REJECTED
import dev.mokadim.locator.Locator.Companion.SETTINGS_REJECTED
import dev.mokadim.locator.Locator.Companion.SETTINGS_UNAVAILABLE
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.support.v4.alert

class MainFragment : Fragment(R.layout.fragment_main) {

  private val updatedLocation = StringBuilder()

  private val locator by lazy {
    Locator.Builder(this)
        .interval(5000L)
        .fastestInterval(2500L)
        .smallestDisplacement(50F)
        .requireLocationSettings(true)
        .printLog(true)
        .build()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    btnCurrentLocation.setOnClickListener { startLocationUpdates() }
  }

  override fun onDestroy() {
    locator.stopLocationUpdates()
    super.onDestroy()
  }

  @SuppressLint("SetTextI18n")
  private fun getCurrentLocation() {
    locator.getCurrentLocation({ tvLocation.text = "${it.latitude}, ${it.longitude}" }) {
      handleLocationError(it)
    }
  }

  private fun startLocationUpdates() {
    locator.startLocationUpdates({
      updatedLocation.append("\n${it.latitude}, ${it.longitude}")
      tvLocation.text = updatedLocation
    }) { handleLocationError(it) }
  }

  @SuppressLint("SetTextI18n")
  private fun handleLocationError(@LocationStatus status: Int) {
    Log.d("Locator", "handleLocationError: $status")
    when (status) {
      PERMISSION_REJECTED -> tvLocation.text = "Permission request rejected"
      PERMISSION_PERMANENTLY_REJECTED -> showSettingsAlert()
      SETTINGS_REJECTED -> tvLocation.text = "Open location request rejected"
      SETTINGS_UNAVAILABLE -> tvLocation.text =
          "Location settings are not satisfied. However, we have no way to fix the settings."
    }
  }

  /**
   * Show settings alert dialog.
   */
  private fun showSettingsAlert() {
    alert(R.string.locator_location_permission, R.string.locator_attention) {
      positiveButton(R.string.locator_go_settings) {
        it.dismiss()
        openSettings()
      }
      negativeButton(R.string.locator_cancel) { it.dismiss() }
    }.show()
  }

  /**
   * Navigating user to app settings
   */
  private fun openSettings() {
    Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", requireActivity().packageName, null)
      startActivity(this)
    }
  }
}