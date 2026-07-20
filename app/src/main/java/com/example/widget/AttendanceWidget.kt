package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.data.model.TimetableSlotWithModule
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = AttendanceRepository(context)

        provideContent {
            val sessionState = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf<TimetableSlotWithModule?>(null)
            }
            val loadingState = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(true)
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    sessionState.value = repository.getCurrentOrNextSession()
                    loadingState.value = false
                }
            }

            WidgetContent(
                context = context,
                session = sessionState.value,
                isLoading = loadingState.value
            )
        }
    }

    @Composable
    private fun WidgetContent(context: Context, session: TimetableSlotWithModule?, isLoading: Boolean) {
        val primaryBlue = ColorProvider(android.graphics.Color.parseColor("#0288D1"))
        val backgroundWhite = ColorProvider(android.graphics.Color.parseColor("#FFFFFF"))
        val textGray = ColorProvider(android.graphics.Color.parseColor("#555555"))
        val textLight = ColorProvider(android.graphics.Color.parseColor("#FFFFFF"))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundWhite)
                .padding(8.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(primaryBlue)
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Uni Leca Attendance",
                    style = TextStyle(
                        color = textLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            if (isLoading) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Loading...", style = TextStyle(color = textGray, fontSize = 12.sp))
                }
            } else if (session == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No more sessions today!",
                        style = TextStyle(color = textGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                }
            } else {
                val slot = session.slot
                Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 4.dp)) {
                    Text(
                        text = "${session.moduleName} (${slot.sessionType})",
                        style = TextStyle(color = textGray, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = "${slot.startTime} - ${slot.endTime}",
                        style = TextStyle(color = textGray, fontSize = 11.sp)
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Action Buttons Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            text = "Present",
                            onClick = actionRunCallback<MarkWidgetAttendanceAction>(
                                actionParametersOf(
                                    MarkWidgetAttendanceAction.slotIdKey to slot.id,
                                    MarkWidgetAttendanceAction.statusKey to "Present",
                                    MarkWidgetAttendanceAction.heldKey to "Held"
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Button(
                            text = "Absent",
                            onClick = actionRunCallback<MarkWidgetAttendanceAction>(
                                actionParametersOf(
                                    MarkWidgetAttendanceAction.slotIdKey to slot.id,
                                    MarkWidgetAttendanceAction.statusKey to "Absent",
                                    MarkWidgetAttendanceAction.heldKey to "Held"
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Button(
                            text = "Medical",
                            onClick = actionRunCallback<MarkWidgetAttendanceAction>(
                                actionParametersOf(
                                    MarkWidgetAttendanceAction.slotIdKey to slot.id,
                                    MarkWidgetAttendanceAction.statusKey to "Medical",
                                    MarkWidgetAttendanceAction.heldKey to "Held"
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Button(
                            text = "Skip",
                            onClick = actionRunCallback<MarkWidgetAttendanceAction>(
                                actionParametersOf(
                                    MarkWidgetAttendanceAction.slotIdKey to slot.id,
                                    MarkWidgetAttendanceAction.statusKey to "Present",
                                    MarkWidgetAttendanceAction.heldKey to "Not Held"
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

class MarkWidgetAttendanceAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val slotId = parameters[slotIdKey] ?: return
        val status = parameters[statusKey] ?: return
        val held = parameters[heldKey] ?: "Held"

        val repository = AttendanceRepository(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        repository.markAttendance(slotId, today, held, status)

        // Force update widget on dispatchers main/IO
        AttendanceWidget().update(context, glanceId)
    }

    companion object {
        val slotIdKey = ActionParameters.Key<Int>("slotId")
        val statusKey = ActionParameters.Key<String>("status")
        val heldKey = ActionParameters.Key<String>("held")
    }
}
