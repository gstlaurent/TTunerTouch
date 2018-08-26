package com.thefourthspecies.ttunertouch.util

import android.util.Log

/**
 * Created by Graham on 2018-01-21.
 */
const val ENABLE_ASSERTIONS: Boolean = true

const val DEBUG_TAG = "TTuner"

fun assert(test: Boolean, message: () -> String) {
    if (!test) {
        if (ENABLE_ASSERTIONS) {
            throw AssertionError(message())
        } else {
            Log.e(DEBUG_TAG, message())
        }
    }
}
