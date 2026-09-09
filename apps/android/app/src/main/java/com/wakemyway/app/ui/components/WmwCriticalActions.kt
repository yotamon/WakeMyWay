package com.wakemyway.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes

@Composable
fun WmwIntentionalStopAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.SleepyTouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, WmwColors.Hairline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = WmwColors.MorningPaper,
        )
    }
}
