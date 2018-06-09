package br.com.mls.contactnumbersearch;

import android.util.Log;

class LogUtil {

    void logError(Throwable exception, String className, String message) {
        Log.e(className, message, exception);
    }
}
