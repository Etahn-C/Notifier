package com.example.notifier

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.example.notifier.ui.theme.NotifierTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class MainActivity : ComponentActivity() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)

        setContent {
            NotifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun ImagePickerButton(onImageSelected: (Uri?) -> Unit) {

    // Launcher for the image picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }
    Column(
        modifier = Modifier.padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { launcher.launch(arrayOf("image/*")) }) {
            Text("Choose Image")
        }
    }
}

@Composable
fun HorizontalLine() {
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color.Gray)
    Spacer(modifier = Modifier.height(10.dp))
}

fun copyImageToAppStorage(
    context: Context,
    uri: Uri,
): File? {
    val resolver = context.contentResolver
    val fileName = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(
                cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            )
        } else null
    } ?: return null
    val file = File(context.filesDir, fileName)
    resolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    } ?: return null
    return file
}


data class TimerConfig(
    val title: String,
    val text: String,
    val time: String,
    val days: String,
    val color: ULong,
    val image: String?,
    val on: Boolean
)

fun saveTimers(context: Context, timers: Map<String, TimerConfig>) {
    val json = Gson().toJson(timers)
    File(context.filesDir, "timers.json").writeText(json)
}

fun loadTimers(context: Context): MutableMap<String, TimerConfig> {
    val file = File(context.filesDir, "timers.json")
    if (!file.exists()) return mutableMapOf()

    val json = file.readText()
    return Gson().fromJson(
        json,
        object : TypeToken<MutableMap<String, TimerConfig>>() {}.type
    )
}

@Composable
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val timers = remember {
        mutableMapOf<String, TimerConfig>().apply {
            putAll(loadTimers(context))
        }
    }
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            MainScreen(
                timers = timers,
                onNext = { key ->
                    navController.navigate("settings/$key")
                }
            )
        }

        composable(
            route = "settings/{timerKey}",
            arguments = listOf(navArgument("timerKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val timerKey = backStackEntry.arguments?.getString("timerKey") ?: "0"

            SettingsScreen(
                timerId = timerKey,
                timers = timers,
                onBack = { navController.popBackStack() }
            )
        }
    }

}


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    timerId: String = "0",
    onBack: () -> Unit,
    timers: MutableMap<String, TimerConfig>
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    val id = if (timerId == "0") {
        ((timers.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
    } else {
        timerId
    }
    var title by remember(id) { mutableStateOf(timers[id]?.title ?: "") }
    var text by remember(id) { mutableStateOf(timers[id]?.text ?: "") }
    var time by remember(id) {
        mutableStateOf(
            timers[id]?.let { LocalTime.parse(it.time, formatter) } ?: LocalTime.now()
        )
    }
    var days by remember(id) { mutableStateOf(timers[id]?.days ?: "0000000") }
    var color by remember(id) { mutableStateOf(timers[id]?.color ?: Color.White.value) }
    var image by remember(id) { mutableStateOf(timers[id]?.image) }


    Column(
        Modifier
            .background(color = Color(0xFFE8E8E8))
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = modifier
                .background(Color(0xFF171717))
                .height(25.dp)
                .fillMaxWidth()
        ) {}
        Column(
            modifier = modifier
                .padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                label = { Text("Enter Notifier Title") },
                placeholder = { Text("Type Here") },
                maxLines = 1,
                modifier = modifier.padding(5.dp)
            )
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                },
                label = { Text("Enter Notifier Description") },
                placeholder = { Text("Type Here") },
                maxLines = 1,
                modifier = modifier.padding(5.dp)
            )
            HorizontalLine()
            Spacer(modifier = modifier.height(5.dp))
            TimeInput(time = time, onTimeChange = { time = it })
            HorizontalLine()

            Row(modifier = modifier) {
                val daysMap = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                for (x in 0..3) {
                    Button(
                        onClick = {
                            days = if (days[x] == '1')
                                days.replaceRange(x, x + 1, "0")
                            else days.replaceRange(x, x + 1, "1")
                        },
                        modifier = modifier.padding(start = 5.dp, end = 5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (days[x] == '1') Color(0xFF55AA55)
                            else Color(0xFFAA5555)
                        )
                    ) { Text(daysMap[x]) }
                }
            }
            Row(modifier = modifier) {
                val daysMap = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                for (x in 4..6) {
                    Button(
                        onClick = {
                            days = if (days[x] == '1')
                                days.replaceRange(x, x + 1, "0")
                            else days.replaceRange(x, x + 1, "1")
                        },
                        modifier = modifier.padding(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (days[x] == '1') Color(0xFF55AA55)
                            else Color(0xFFAA5555)
                        )
                    ) { Text(daysMap[x]) }
                }
            }
            HorizontalLine()
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                var colorText by remember {
                    mutableStateOf(
                        String.format(
                            "#%02X%02X%02X",
                            (Color(color).red * 255).toInt(),
                            (Color(color).green * 255).toInt(),
                            (Color(color).blue * 255).toInt()
                        )
                    )
                }
                OutlinedTextField(
                    value = colorText,
                    onValueChange = { input ->
                        colorText = input

                        if (
                            (input.length == 7 || input.length == 9) &&
                            input.startsWith("#")
                        ) {
                            try {
                                color = Color(input.toColorInt()).value
                            } catch (_: Exception) {
                            }
                        }
                    },
                    label = { Text("Enter Valid Hex Color") },
                    placeholder = { Text("#RRGGBB") },
                    maxLines = 1,
                    modifier = modifier.padding(5.dp)
                )

                Box(
                    modifier = modifier
                        .size(60.dp)
                        .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 5.dp)
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(Color(color))
                        .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                )
            }
            HorizontalLine()
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current
                ImagePickerButton { uri ->
                    uri?.let { safeUri ->
                        image = copyImageToAppStorage(context, safeUri)?.absolutePath
                    }
                }
                Image(
                    painter = rememberAsyncImagePainter(image),
                    contentDescription = "Image",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            HorizontalLine()
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = modifier.width(100.dp),
                    onClick = { onBack() }) { Text("Cancel") }
                Button(
                    modifier = modifier.width(100.dp),
                    onClick = {

                        if (timerId != "0") {
                            timers.remove(timerId)
                        }
                        saveTimers(context, timers)
                        AlarmScheduler.cancelTimer(context, id)
                        onBack()
                    }) { Text("Delete") }
                Button(
                    modifier = modifier.width(100.dp),
                    onClick = {
                        timers[id] = TimerConfig(
                            title = title,
                            text = text,
                            time = time.format(formatter),
                            days = days,
                            color = color,
                            image = image,
                            on = true
                        )
                        saveTimers(context, timers)
                        try{
                            AlarmScheduler.cancelTimer(context, id)
                        }catch(_: Exception){}
                        AlarmScheduler.scheduleTimer(context, id, timers[id]!!)
                        onBack()
                    }) { Text("Save") }
            }
        }
    }
}

@Composable
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun MainScreen(
    modifier: Modifier = Modifier,
    onNext: (String) -> Unit,
    timers: MutableMap<String, TimerConfig>
) {
    val context = LocalContext.current

    Column(
        Modifier
            .background(color = Color(0xFFE8E8E8))
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    )
    {
        Row(
            modifier = modifier
                .background(Color(0xFF171717))
                .height(25.dp)
                .fillMaxWidth()
        ) {}
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = modifier
                .padding(top = 10.dp, bottom = 10.dp, end = 10.dp, start = 10.dp)
                .fillMaxWidth()
        ) {
            Button(onClick = { onNext("0") }, modifier = modifier)
            { Text("Add a Notifier") }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = modifier
                .verticalScroll(rememberScrollState())
        ) {
            val timerIds = loadTimers(context = context).keys.toList()
            for (id in timerIds) {
                TimerButton(onNext = { onNext(id) }, timerId = id, timers = timers)
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
@Composable
fun TimerButton(
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    timerId: String,
    timers: MutableMap<String, TimerConfig>
) {
    val context = LocalContext.current

    val timer = timers[timerId]
    var isToggled by remember { mutableStateOf(timer!!.on) }
    val selectedTime = timer!!.time
    val selectedColor = Color(timer.color)
    val selectedDays = timer.days
    val selectedImage = timer.image
    val selectedTitle = timer.title
    val selectedText = timer.text

    val textColor =
        if (ColorUtils.calculateLuminance(selectedColor.toArgb()) < 0.5)
            Color(0xFFFFFFFF)
        else
            Color(0xFF000000)

    Row(

        modifier = modifier
            .padding(top = 5.dp, bottom = 5.dp, end = 10.dp, start = 10.dp)
            .fillMaxWidth()
            .heightIn(max = 125.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(selectedColor)
            .border(2.dp, shape = RoundedCornerShape(16.dp), color = Color.Black),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    )
    {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    NotificationHelper.showNotification(
                        context,
                        title = selectedTitle,
                        text = selectedText,
                        imgPath = selectedImage,
                        backgroundColor = Color(timer.color).toArgb()
                    )
                },
                modifier = modifier
                    .padding(5.dp)
            ) { Text(selectedTime) }
            val daysString = StringBuilder("")
            val digits = selectedDays.map { it - '0' }
            val daysMap = listOf("Mo, ", "Tu, ", "We, ", "Th, ", "Fr, ", "Sa, ", "Su, ")
            for (x in 0..6) {
                if (digits[x] == 1) {
                    if (daysString.length + 1 % 17 == 17) {
                        daysString.append("\n")
                    }
                    daysString.append(daysMap[x])
                }
            }
            if (daysString.length > 2) {
                daysString.deleteCharAt(daysString.lastIndex)
                daysString.deleteCharAt(daysString.lastIndex)
            }
            Text(
                daysString.toString(),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(5.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth(0.25f)
        ) {
            Text(
                text = selectedTitle,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = modifier.padding(top = 5.dp, bottom = 5.dp)
            )
            Text(
                text = selectedText,
                color = textColor,
                textAlign = TextAlign.Center,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                maxLines = 5,
                modifier = modifier.padding(bottom = 5.dp)
            )
        }


        Image(
            painter = rememberAsyncImagePainter(selectedImage),
            contentDescription = "My image",
            modifier = Modifier
                .size(100.dp)
                .padding(5.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.widthIn(min = 100.dp)
        ) {
            Button(
                onClick = {
                    isToggled = !isToggled
                    timers[timerId] = TimerConfig(
                        title = selectedTitle,
                        text = selectedText,
                        time = selectedTime,
                        days = selectedDays,
                        color = selectedColor.value,
                        image = selectedImage,
                        on = isToggled
                    )
                    saveTimers(context, timers)
                    if (isToggled) {
                        try{
                            AlarmScheduler.cancelTimer(context, timerId)
                        }catch(_: Exception){}
                        AlarmScheduler.scheduleTimer(context, timerId, timers[timerId]!!)
                    } else {
                        AlarmScheduler.cancelTimer(context, timerId)
                    }
                },
                modifier = modifier.padding(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isToggled) Color(0xFF55AA55)
                    else Color(0xFFAA5555)
                )
            ) { Text(text = if (isToggled) "ON" else "OFF") }
            Button(onClick = onNext, modifier = modifier.padding(5.dp))
            {
                Text(
                    "EDIT"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInput(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {

    val timePickerState = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = true,
    )
    LaunchedEffect(
        timePickerState.hour,
        timePickerState.minute
    ) {
        onTimeChange(
            LocalTime.of(
                timePickerState.hour,
                timePickerState.minute
            )
        )
    }

    TimeInput(state = timePickerState)
}
