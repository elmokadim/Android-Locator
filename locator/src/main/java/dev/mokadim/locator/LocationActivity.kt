package dev.mokadim.locator

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes.RESOLUTION_REQUIRED
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE
import dev.mokadim.locator.Locator.Companion.ALL_GRANTED
import dev.mokadim.locator.Locator.Companion.LocationStatus
import dev.mokadim.locator.Locator.Companion.PERMISSION_ACCEPTED
import dev.mokadim.locator.Locator.Companion.PERMISSION_PERMANENTLY_REJECTED
import dev.mokadim.locator.Locator.Companion.PERMISSION_REJECTED
import dev.mokadim.locator.Locator.Companion.PRINT_LOG
import dev.mokadim.locator.Locator.Companion.REQUEST_CHECK_SETTINGS
import dev.mokadim.locator.Locator.Companion.REQUIRE_LOCATION_SETTINGS
import dev.mokadim.locator.Locator.Companion.SETTINGS_REJECTED
import dev.mokadim.locator.Locator.Companion.SETTINGS_UNAVAILABLE
import dev.mokadim.locator.Locator.Companion.TAG
import dev.mokadim.locator.Locator.Companion.locationStatusLiveData

/**
 * Ahmed Elmokadim
 * elmokadim@gmail.com
 * 01/01/2021
 */
class LocationActivity : AppCompatActivity() {

  private val printLog by lazy { intent.getBooleanExtra(PRINT_LOG, true) }
  private val requireLocationSettings by lazy { intent.getBooleanExtra(REQUIRE_LOCATION_SETTINGS, false) }

  private val locationSettingsRequestBuilder by lazy {
    LocationSettingsRequest.Builder().addLocationRequest(LocationRequest()).setAlwaysShow(true).build()
  }

  private val locationPermission = registerForActivityResult(RequestPermission()) { isGranted ->
    when (isGranted) {
      true -> if (requireLocationSettings) checkLocationSettings() else publishResult(PERMISSION_ACCEPTED)
      false -> {
        val showRationale = shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)
        publishResult(if (!showRationale) PERMISSION_PERMANENTLY_REJECTED else PERMISSION_REJECTED)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestLocationPermission()
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_CHECK_SETTINGS -> {
        when (resultCode) {
          RESULT_OK -> publishResult(ALL_GRANTED)
          RESULT_CANCELED -> publishResult(SETTINGS_REJECTED)
        }
        finish()
      }
    }
  }

  private fun requestLocationPermission() = locationPermission.launch(ACCESS_FINE_LOCATION)

  /**
   * Checks if location is enabled, if not, opens a dialog asking to open it.
   */
  private fun checkLocationSettings() {
    LocationServices.getSettingsClient(this)
        .checkLocationSettings(locationSettingsRequestBuilder)
        .addOnSuccessListener { publishResult(ALL_GRANTED) }
        .addOnFailureListener { exception ->
          when ((exception as ApiException).statusCode) {
            RESOLUTION_REQUIRED -> try {
              if (printLog) Log.e(TAG, "Show location settings dialog.")
              (exception as ResolvableApiException).startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
            } catch (sendEx: IntentSender.SendIntentException) {
              if (printLog) Log.e(TAG, "checkLocationSettings: Ignore the error.")
              publishResult(SETTINGS_UNAVAILABLE)
            }
            SETTINGS_CHANGE_UNAVAILABLE -> {
              if (printLog) Log.e(TAG,
                  "Location settings are not satisfied. However, we have no way to fix the settings.")
              publishResult(SETTINGS_UNAVAILABLE)
            }
          }
        }
  }

  private fun publishResult(@LocationStatus status: Int) {
    locationStatusLiveData.value = status
    finish()
  }
}