package dev.mokadim.locator

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Ahmed Elmokadim
 * elmokadim@gmail.com
 * 01/01/2021
 */
class Locator private constructor(@NonNull private val activity: AppCompatActivity) {

  companion object {
    internal const val REQUEST_CHECK_SETTINGS = 1000

    internal const val REQUIRE_LOCATION_SETTINGS = "RequireLocationSettings"
    internal const val PRINT_LOG = "PrintLog"
    internal const val TAG = "Locator"

    internal val locationStatusLiveData by lazy { MutableLiveData<@LocationStatus Int>() }

    @Retention(SOURCE)
    @Target(TYPE, VALUE_PARAMETER)
    @IntDef(NONE, PERMISSION_ACCEPTED, PERMISSION_REJECTED, PERMISSION_PERMANENTLY_REJECTED,
        SETTINGS_SATISFIED, SETTINGS_REJECTED, SETTINGS_UNAVAILABLE, ALL_GRANTED)
    annotation class LocationStatus

    internal const val NONE = 0
    internal const val PERMISSION_ACCEPTED = 1
    internal const val SETTINGS_SATISFIED = 2
    internal const val ALL_GRANTED = 3
    const val PERMISSION_REJECTED = 4
    const val PERMISSION_PERMANENTLY_REJECTED = 5
    const val SETTINGS_REJECTED = 6
    const val SETTINGS_UNAVAILABLE = 7
  }

  private var mInterval = 3000L
  private var mFastestInterval = 1500L
  private var mSmallestDisplacement = 50F
  private var mRequireLocationSettings = true
  private var mPrintLog = true

  private var locationCallbackAttached = false
  private lateinit var locationProviderClient: FusedLocationProviderClient
  private val lastLocation = MutableLiveData<Location>()

  private val locationRequest by lazy {
    LocationRequest().apply {
      priority = PRIORITY_HIGH_ACCURACY
      interval = mInterval
      fastestInterval = mFastestInterval
      smallestDisplacement = mSmallestDisplacement
    }
  }

  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
      lastLocation.value = locationResult.lastLocation
    }
  }

  data class Builder(@NonNull private val activity: AppCompatActivity) {

    private var interval = 3000L
    private var fastestInterval = 1500L
    private var smallestDisplacement = 50F
    private var requireLocationSettings = true
    private var printLog = true

    constructor(@NonNull fragment: Fragment) : this(fragment.requireActivity() as AppCompatActivity)

    /**
     * Set the desired interval for active location updates, in milliseconds.
     * By default this is 5000.
     *
     * @param interval desired interval in millisecond, inexact
     */
    fun interval(interval: Long) = apply { this.interval = interval }

    /**
     * Explicitly set the fastest interval for location updates, in milliseconds.
     * By default this is 2500.
     *
     * @param fastestInterval fastest interval for updates in milliseconds, exact
     */
    fun fastestInterval(fastestInterval: Long) = apply { this.fastestInterval = fastestInterval }

    /**
     * Set the minimum displacement between location updates in meters
     * By default this is 100.
     *
     * @param smallestDisplacement the smallest displacement in meters the user must
     * move between location updates.
     */
    fun smallestDisplacement(smallestDisplacement: Float) =
        apply { this.smallestDisplacement = smallestDisplacement }

    /**
     * Choose to check location settings or not.
     * By default this is true.
     *
     * @param requireLocationSettings true to check location settings otherwise false.
     */
    fun requireLocationSettings(requireLocationSettings: Boolean) =
        apply { this.requireLocationSettings = requireLocationSettings }

    /**
     * Enable or disable logging.
     *
     * @param printLog true to enable and false to disable.
     */
    fun printLog(printLog: Boolean) = apply { this.printLog = printLog }

    fun build() = Locator(activity).apply {
      mInterval = interval
      mFastestInterval = fastestInterval
      mSmallestDisplacement = smallestDisplacement
      mRequireLocationSettings = requireLocationSettings
      mPrintLog = printLog
    }
  }

  /**
   * Determine as precise a location as possible from the available location providers, GPS as well as WiFi
   * and mobile cell data. if the caller has location permission, otherwise it will ask user for permission.
   *
   * @param onSuccess The call back to receive the location.
   * @param onError The call back to trigger an action.
   */
  @SuppressLint("MissingPermission")
  fun getCurrentLocation(onSuccess: (location: Location) -> Unit,
                         onError: (status: @LocationStatus Int) -> Unit) {
    checkPermissions({ getLastLocation(onSuccess) }) { onError.invoke(it) }
  }

  /**
   * Listen to location updates.
   * Don't forget to call stopLocationUpdates() when no more required.
   *
   * @param onLocationChanged The call back to receive the updated location.
   * @param onError The call back to trigger an action.
   */
  @SuppressLint("MissingPermission")
  fun startLocationUpdates(onLocationChanged: (location: Location) -> Unit,
                           onError: (status: @LocationStatus Int) -> Unit) {
    checkPermissions({ requestLocationUpdates(onLocationChanged) }) { onError.invoke(it) }
  }

  /**
   * Stops location updates when no more required.
   */
  fun stopLocationUpdates() {
    if (locationCallbackAttached) {
      locationProviderClient.removeLocationUpdates(locationCallback)
      locationCallbackAttached = false
      lastLocation.removeObservers(activity)
      if (mPrintLog) Log.i(TAG, "Location updates stopped")
    }
  }

  /**
   * Initialize location provider client if it is not before.
   *
   * @return FusedLocationProviderClient
   */
  private fun getLocationProviderClient(): FusedLocationProviderClient {
    if (!::locationProviderClient.isInitialized)
      locationProviderClient = getFusedLocationProviderClient(activity)
    return locationProviderClient
  }

  private fun checkPermissions(onSuccess: () -> Unit, onError: (status: @LocationStatus Int) -> Unit) {
    activity.startActivity(Intent(activity, LocationActivity::class.java).apply {
      putExtra(PRINT_LOG, mPrintLog)
      putExtra(REQUIRE_LOCATION_SETTINGS, mRequireLocationSettings)
    })

    locationStatusLiveData.observe(activity) {
      when (it) {
        NONE -> return@observe
        PERMISSION_ACCEPTED, SETTINGS_SATISFIED, ALL_GRANTED -> onSuccess.invoke()
        else -> onError.invoke(it)
      }
      locationStatusLiveData.value = NONE
      locationStatusLiveData.removeObservers(activity)
    }
  }

  /**
   * The fused Location Provider will only maintain background location if at least one client is
   * connected to it. But we don't want to launch the Maps app to get last location, and also we can't say
   * our users to launch Maps app to get last location.
   * What we need to do is request location updates once it get location and stop it.
   *
   * @param onSuccess The The call back when location returned.
   */
  @SuppressLint("MissingPermission")
  private fun getLastLocation(onSuccess: (location: Location) -> Unit) {
    getLocationProviderClient().lastLocation.addOnSuccessListener { location: Location? ->
      if (location != null) {
        if (mPrintLog) Log.i(TAG, "LastLocation: ${location.latitude}, ${location.longitude}")
        onSuccess.invoke(location)
      } else {
        if (mPrintLog) Log.i(TAG, "Failed to get last location, Start request location updates")
        requestLocationUpdates {
          onSuccess.invoke(it)
          stopLocationUpdates()
        }
      }
    }
  }

  /**
   * Listen to location changes.
   *
   * @param onLocationChanged The call back which triggered every time location changed.
   */
  @SuppressLint("MissingPermission")
  private fun requestLocationUpdates(onLocationChanged: (location: Location) -> Unit) {
    if (mPrintLog) Log.i(TAG, "Starting location updates")
    getLocationProviderClient()
        .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    locationCallbackAttached = true
    lastLocation.observe(activity) {
      if (mPrintLog) Log.i(TAG, "LocationUpdates: ${it.latitude}, ${it.longitude}")
      onLocationChanged.invoke(it)
    }
  }
}