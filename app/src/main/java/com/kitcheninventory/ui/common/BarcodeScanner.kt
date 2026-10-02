package com.kitcheninventory.ui.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Returns a function that opens Google's barcode scanner and passes the scanned text to
 * [onScanned]. Cancelling the scan does nothing.
 */
@Composable
fun rememberBarcodeScanner(onScanned: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onScanned)
    return remember(context) {
        {
            GmsBarcodeScanning.getClient(context).startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.trim()?.takeIf { it.isNotEmpty() }?.let { callback(it) }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Scanner not available. Check Google Play services.", Toast.LENGTH_LONG).show()
                }
        }
    }
}
