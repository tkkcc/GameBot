package gamebot.host.presentation.component

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun Section(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(16.dp)) {
        SectionTitle(
            title, modifier = Modifier.padding(
                start = 16.dp, bottom = 8.dp
            )
        )
        Surface(
            tonalElevation = 1.dp, shape = RoundedCornerShape(16.dp)
        ) {
            Column(content = content)
        }
    }
}
@Composable
fun Section(title: AnnotatedString, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(16.dp)) {
        SectionTitle(
            title, modifier = Modifier.padding(
                start = 16.dp, bottom = 8.dp
            )
        )
        Surface(
            tonalElevation = 1.dp, shape = RoundedCornerShape(16.dp)
        ) {
            Column(content = content)
        }
    }
}

@Preview(
    showBackground = true, showSystemUi = true
)
@Preview(
    uiMode = UI_MODE_NIGHT_YES, showBackground = true, showSystemUi = true
)
@Composable
fun SectionPreview() {
    MaterialTheme {
        Column {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("dd")
            }
            Section(
                title = "dd"
            ) {
                Text(
                    "123",
//                color= MaterialTheme.colorScheme.onBackground
                )
            }
        }

    }
}