package com.nur.quran.desktop.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Shape-safe verse renderer: shows tajweed-colored HTML inside a real browser
 * engine instead of Compose spans.
 *
 * ## Why a WebView
 * A browser (Chromium/Skia + HarfBuzz) **shapes first, then paints**: the
 * full Arabic run — joins, lam-alef ligatures, tashkeel stacking — is shaped
 * as one run and per-`<span>` colors are applied afterwards, so a color
 * boundary can never detach a join or misplace a diacritic. Compose
 * `AnnotatedString`/`SpanStyle` text may shape each span independently,
 * splitting those runs at color boundaries and breaking the script. This is
 * the desktop equivalent of the Android `TajweedAndroidText`, which relies on
 * `TextView` + `ForegroundColorSpan` for the identical shape-then-paint
 * guarantee.
 *
 * ## Dependency to add (desktop/build.gradle.kts)
 * ```
 * dependencies {
 *     implementation("com.multiplatform.webview:webview-desktop:<version>")
 * }
 * ```
 * then replace the fallback body below with:
 * ```
 * val state = rememberWebViewStateWithHTMLData(html)
 * WebView(
 *     state = state,
 *     modifier = modifier.fillMaxWidth(),
 * )
 * ```
 * (`com.multiplatform.webview.web.WebView` /
 * `rememberWebViewStateWithHTMLData` from
 * https://github.com/KevinnZou/compose-multiplatform-webview).
 * Serve `verse_template.html` with a base URL at the resources directory so
 * the `@font-face` relative URL (`../fonts/kfgqpc_hafs.ttf`) resolves, and
 * scale the page font via CSS `font-size` derived from [fontScale].
 *
 * ## Current state
 * TODO: the webview-desktop dependency is not on the classpath yet, so this
 * renders a selectable [Text] fallback. Wire the WebView implementation above
 * once the dependency is added. (Fallback note: Compose `Text` has the same
 * span-shaping caveat — here it renders the raw HTML source, so it is
 * debug-only, not a visual equivalent.)
 *
 * @param html full page HTML (template with `{{VERSE_HTML}}` replaced).
 * @param fontScale user font scale; multiplies the base 22sp size.
 */
@Composable
fun VerseWebView(
    html: String,
    fontScale: Float,
    modifier: Modifier = Modifier
) {
    // TODO(webview): swap for com.multiplatform.webview WebView once
    // "com.multiplatform.webview:webview-desktop" is added — see KDoc above.
    val scroll = rememberScrollState()
    SelectionContainer {
        Text(
            text = remember(html) { html },
            fontSize = (22 * fontScale).sp,
            textAlign = TextAlign.Right,
            modifier = modifier.fillMaxWidth().verticalScroll(scroll)
        )
    }
}
