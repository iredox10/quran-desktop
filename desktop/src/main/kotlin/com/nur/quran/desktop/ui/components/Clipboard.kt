package com.nur.quran.desktop.ui.components

/** Copies [text] to the system clipboard. Returns false when unavailable. */
fun copyToClipboard(text: String): Boolean = try {
    val selection = java.awt.datatransfer.StringSelection(text)
    java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    true
} catch (_: Exception) {
    false
}
