package com.wakemyway.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WakeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val rawId = intent.getStringExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID)
        if (rawId == null) {
            finish()
            return
        }
        val occurrenceId = WakeOccurrenceId(rawId)

        setContent {
            WakeMyWayTheme {
                WakeSurface(
                    onSnooze = {
                        AlarmPlaybackService.requestSnooze(this, occurrenceId)
                        finishAndRemoveTask()
                    },
                    onStop = {
                        AlarmPlaybackService.requestStop(this, occurrenceId)
                        finishAndRemoveTask()
                    },
                )
            }
        }

        window.decorView.post {
            WakeTimingTrace(this).uiVisible(occurrenceId)
        }
    }
}

@Composable
private fun WakeSurface(
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = "Morning.",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
            text = "Just get upright first.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Button(onClick = onSnooze) {
            Text("I need 5 more minutes")
        }
        OutlinedButton(
            modifier = Modifier.padding(top = 12.dp),
            onClick = onStop,
        ) {
            Text("Stop")
        }
    }
}
