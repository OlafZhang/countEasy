package org.olafzhang.counteasy.utils

import java.text.DecimalFormat

object NumberFormatter {
    fun formatNumber(number: Double, decimalPlaces: Int): String {
        val pattern = if (decimalPlaces > 0) {
            "0." + "0".repeat(decimalPlaces)
        } else {
            "0"
        }
        val formatter = DecimalFormat(pattern)
        return formatter.format(number)
    }
} 