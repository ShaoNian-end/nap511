package github.zerorooot.nap511.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.ui.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.ui.theme.MiuixTheme
import top.yukonga.miuix.kmp.ui.theme.ThemeController

@Composable
fun Nap511Theme(
    content: @Composable () -> Unit
) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(
        controller = controller,
        content = content
    )
}