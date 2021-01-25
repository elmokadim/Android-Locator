package dev.mokadim.locator

import androidx.annotation.IntDef
import androidx.lifecycle.MutableLiveData
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Ahmed Elmokadim
 * elmokadim@gmail.com
 * 01/01/2021
 */

internal const val REQUEST_CHECK_SETTINGS = 1000

internal const val REQUIRE_LOCATION_SETTINGS = "RequireLocationSettings"
internal const val PRINT_LOG = "PrintLog"
internal const val TAG = "Locator"

//region Location Settings
internal val locationStatusLiveData by lazy { MutableLiveData<@LocationStatus Int>() }

@MustBeDocumented
@Retention(SOURCE)
@Target(TYPE, VALUE_PARAMETER)
@IntDef(NONE, PERMISSION_ACCEPTED, PERMISSION_REJECTED, PERMISSION_PERMANENTLY_REJECTED,
    SETTINGS_SATISFIED, SETTINGS_REJECTED, SETTINGS_UNAVAILABLE, ALL_GRANTED)
annotation class LocationStatus

internal const val NONE = 0
internal const val PERMISSION_ACCEPTED = 1
internal const val SETTINGS_SATISFIED = 2
internal const val ALL_GRANTED = 3
public const val PERMISSION_REJECTED = 4
const val PERMISSION_PERMANENTLY_REJECTED = 5
const val SETTINGS_REJECTED = 6
const val SETTINGS_UNAVAILABLE = 7
//endregion