package bilabila.gamebot.host.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.intl.Locale
import cafe.adriel.lyricist.LanguageTag
import cafe.adriel.lyricist.Lyricist
import cafe.adriel.lyricist.ProvideStrings
import bilabila.gamebot.host.presentation.string.EN
import bilabila.gamebot.host.presentation.string.Strings
import bilabila.gamebot.host.presentation.string.ZH


val Strings: Map<LanguageTag, Strings> = mapOf(
    "en" to EN,
    "zh" to ZH
)

val LocalStrings: ProvidableCompositionLocal<Strings> = compositionLocalOf { EN }



// it's for i18n, is provide LocalStrings.current and S
@Composable
fun StringView(content: @Composable () -> Unit) {
    val lyricist =
        remember {
            Lyricist("en", Strings).apply {
                languageTag = Locale.current.toLanguageTag()
            }
        }
    val S = LocalStrings.current
    val locale = LocalConfiguration.current.locales[0]
//    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    LaunchedEffect(true){
        ToUIString.S = S
        ToUIString.locale = locale
    }

    ProvideStrings(lyricist, LocalStrings, content = content)
}