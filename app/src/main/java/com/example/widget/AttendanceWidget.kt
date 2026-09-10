package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.UniLecaApplication
import com.example.data.model.AttendanceStatus
import com.example.data.model.HeldStatus
import com.example.data.model.TimetableSlotWithModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class AttendanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = UniLecaApplication.container(context).repository
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
            WidgetContent(sessionState.value, loadingState.value)
        }
    }

    @Composable
    private fun WidgetContent(session: TimetableSlotWithModule?, isLoading: Boolean) {
        val primary = ColorProvider(Color(0xFF0288D1))
        val background = ColorProvider(Color.White)
        val text = ColorProvider(Color(0xFF334155))
        val white = ColorProvider(Color.White)

        Column(
            modifier = GlanceModifier.fillMaxSize().background(background).padding(8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(primary)
                    .padding(vertical = 5.dp, horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Uni Leca",
                    style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(GlanceModifier.height(6.dp))

            when {
                isLoading -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…", style = TextStyle(color = text, fontSize = 12.sp))
                }
                session == null -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No more sessions today", style = TextStyle(color = text, fontSize = 12.sp))
                }
                else -> {
                    val slot = session.slot
                    val start = runCatching { LocalTime.parse(slot.startTime) }.getOrNull()
                    val canMark = start != null && !LocalTime.now().isBefore(start) &&
                        (slot.isRecurring || slot.specificDate == LocalDate.now().toString())

                    Text(
                        "${session.moduleName} · ${slot.sessionType}",
                        style = TextStyle(color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        "${slot.startTime} – ${slot.endTime}${if (slot.isExtraSession) " · EXTRA" else ""}",
                        style = TextStyle(color = text, fontSize = 11.sp)
                    )
                    Spacer(GlanceModifier.defaultWeight())

                    if (canMark) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                text = "Present",
                                onClick = actionRunCallback<MarkWidgetAttendanceAction>(
                                    actionParametersOf(
                                        MarkWidgetAttendanceAction.slotIdKey to slot.id,
                                        MarkWidgetAttendanceAction.statusKey to AttendanceStatus.PRESENT.dbValue,
                                        MarkWidgetAttendanceAction.heldKey to HeldStatus.HELD.dbValue
                                    )
                                )
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            Button(
                                text = "Absent",
                                onClick = actionRunCallback<MarkWidgetAttendanceAction>(
                                    actionParametersOf(
                                        MarkWidgetAttendanceAction.slotIdKey to slot.id,
                                        MarkWidgetAttendanceAction.statusKey to AttendanceStatus.ABSENT.dbValue,
                                        MarkWidgetAttendanceAction.heldKey to HeldStatus.HELD.dbValue
                                    )
                                )
                            )
                        }
                    } else {
                        Text("Upcoming · marking unlocks at ${slot.startTime}", style = TextStyle(color = text, fontSize = 11.sp))
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
        val held = parameters[heldKey] ?: HeldStatus.HELD.dbValue
        val repository = UniLecaApplication.container(context).repository
        repository.markAttendance(slotId, LocalDate.now().toString(), held, status)
        AttendanceWidget().update(context, glanceId)
    }

    companion object {
        val slotIdKey = ActionParameters.Key<Int>("slotId")
        val statusKey = ActionParameters.Key<String>("status")
        val heldKey = ActionParameters.Key<String>("held")
    }
}
