package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BmiRecord
import com.example.data.entity.FoodLog
import com.example.data.entity.DailyIntakeRecord
import com.example.data.model.NutritionArticle
import coil.compose.AsyncImage
import com.example.util.PdfExporter
import com.example.ui.viewmodel.BmiViewModel
import com.example.ui.viewmodel.DailyNutritionNeeds
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmiScreen(
    viewModel: BmiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weight by viewModel.weight.collectAsStateWithLifecycle()
    val height by viewModel.height.collectAsStateWithLifecycle()
    val gender by viewModel.gender.collectAsStateWithLifecycle()
    val historyRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val foodLogs by viewModel.allFoodLogs.collectAsStateWithLifecycle()
    val dailyIntakeRecords by viewModel.allDailyIntakeRecords.collectAsStateWithLifecycle()

    val currentBmi = viewModel.calculateBmi(weight, height)
    val category = viewModel.getCategory(currentBmi)
    val idealWeight = viewModel.getIdealWeight(height)
    val idealRange = viewModel.getIdealRange(height)

    // Color definitions based on category
    val categoryColor = when {
        currentBmi < 18.5f -> Color(0xFF35A2FF) // Underweight blue
        currentBmi in 18.5f..24.9f -> Color(0xFF00C853) // Normal green
        currentBmi in 25.0f..29.9f -> Color(0xFFFF9100) // Overweight orange
        else -> Color(0xFFFF1744) // Obese red
    }

    var selectedTab by remember { mutableStateOf(0) }
    var dragAmountAccumulated by remember { mutableStateOf(0f) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(selectedTab) {
                    detectHorizontalDragGestures(
                        onDragStart = { _ ->
                            dragAmountAccumulated = 0f
                        },
                        onDragEnd = {
                            if (dragAmountAccumulated > 140f) { // Swipe right (backwards)
                                if (selectedTab > 0) {
                                    selectedTab -= 1
                                }
                            } else if (dragAmountAccumulated < -140f) { // Swipe left (forwards)
                                if (selectedTab < 3) {
                                    selectedTab += 1
                                }
                            }
                        },
                        onDragCancel = {
                            dragAmountAccumulated = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAmountAccumulated += dragAmount
                        }
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-fidelity Bold Theme Header
            HeaderSection()

            // Custom segmented visual tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("BMI & Ideal", "Kebutuhan", "Catatan Makan", "Edukasi Gizi")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 9.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Render selected view
            when (selectedTab) {
                0 -> {
                    BmiCalculatorTabContent(
                        weight = weight,
                        height = height,
                        gender = gender,
                        currentBmi = currentBmi,
                        category = category,
                        categoryColor = categoryColor,
                        idealWeight = idealWeight,
                        idealRange = idealRange,
                        historyRecords = historyRecords,
                        viewModel = viewModel,
                        context = context
                    )
                }
                1 -> {
                    DailyNeedsTabContent(
                        weight = weight,
                        height = height,
                        gender = gender,
                        viewModel = viewModel,
                        context = context
                    )
                }
                2 -> {
                    MealTrackerTabContent(
                        foodLogs = foodLogs,
                        dailyIntakeRecords = dailyIntakeRecords,
                        viewModel = viewModel,
                        weight = weight,
                        height = height,
                        gender = gender,
                        context = context
                    )
                }
                3 -> {
                    EducationTabContent(
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_header_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GIZICARE",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("app_title")
            )
            Text(
                text = "Halo, Teman Sehat",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Avatar Profile Badge matching the design
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User info",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("welcome_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tips",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Pantau Kesehatan Mandiri",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Dapatkan kalkulasi BMI presisi, berat badan ideal yang sesuai, serta panduan riwayat perkembangan Anda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun GenderSelector(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Jenis Kelamin",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val genders = listOf("Laki-laki" to "👨", "Perempuan" to "👩")
            genders.forEach { (genderOption, emoji) ->
                val isSelected = selectedGender == genderOption
                val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onGenderSelected(genderOption) }
                        .testTag("gender_card_$genderOption"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = genderOption,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeightInputCard(
    height: Float,
    onHeightChange: (Float) -> Unit
) {
    var textValue by remember(height) { mutableStateOf(height.roundToInt().toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("height_input_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tinggi Badan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            val cleaned = newValue.filter { it.isDigit() }.take(3)
                            textValue = cleaned
                            if (cleaned.isNotEmpty()) {
                                cleaned.toFloatOrNull()?.let { parsed ->
                                    if (parsed in 80f..230f) {
                                        onHeightChange(parsed)
                                    }
                                }
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.End
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .width(32.dp)
                            .testTag("height_text_input")
                    )
                    Text(
                        text = "cm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Minus stepper button
                IconButton(
                    onClick = { onHeightChange((height - 1).coerceIn(80f, 230f)) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .testTag("height_minus_btn")
                ) {
                    Text(
                        text = "—",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }

                // Slider
                Slider(
                    value = height.coerceIn(80f, 230f),
                    onValueChange = onHeightChange,
                    valueRange = 80f..230f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("height_slider"),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Plus stepper button
                IconButton(
                    onClick = { onHeightChange((height + 1).coerceIn(80f, 230f)) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .testTag("height_plus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah tinggi",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeightInputCard(
    weight: Float,
    onWeightChange: (Float) -> Unit
) {
    var textValue by remember(weight) { 
        mutableStateOf(String.format(Locale.US, "%.1f", weight)) 
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_input_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Berat Badan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            val cleaned = newValue.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)
                            textValue = cleaned
                            if (cleaned.isNotEmpty() && cleaned != ".") {
                                cleaned.toFloatOrNull()?.let { parsed ->
                                    if (parsed in 30f..180f) {
                                        onWeightChange(parsed)
                                    }
                                }
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.End
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .width(48.dp)
                            .testTag("weight_text_input")
                    )
                    Text(
                        text = "kg",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Minus stepper button
                IconButton(
                    onClick = { onWeightChange((weight - 0.5f).coerceIn(30f, 180f)) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .testTag("weight_minus_btn")
                ) {
                    Text(
                        text = "—",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }

                // Slider
                Slider(
                    value = weight.coerceIn(30f, 180f),
                    onValueChange = onWeightChange,
                    valueRange = 30f..180f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weight_slider"),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Plus stepper button
                IconButton(
                    onClick = { onWeightChange((weight + 0.5f).coerceIn(30f, 180f)) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .testTag("weight_plus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah berat",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResultGaugeCard(
    bmi: Float,
    category: String,
    categoryColor: Color,
    weight: Float,
    height: Float,
    idealWeight: Float,
    idealRange: Pair<Float, Float>,
    onSaveClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_gauge_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Indeks Massa Tubuh Anda",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Giant, Bold, tracking-tighter typography focus display matching the theme perfectly!
            Row(
                modifier = Modifier.wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", bmi),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-4).sp,
                    modifier = Modifier.testTag("giant_bmi_text")
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Floating category status badge overlapping style
                Box(
                    modifier = Modifier
                        .background(
                            color = categoryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = category.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = categoryColor
                    )
                }
            }

            // High-fidelity multi-segment bar gauge replacing circular meter
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Segment block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Underweight (Blue): BMI < 18.5 (approx 20% weight)
                        Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFF35A2FF)))
                        // Ideal (Green): BMI 18.5 - 24.9 (approx 40% weight)
                        Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFF00C853)))
                        // Overweight (Yellow): BMI 25.0 - 29.9 (approx 20% weight)
                        Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFFFF9100)))
                        // Obese (Red): BMI >= 30.0 (approx 20% weight)
                        Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFFFF1744)))
                    }
                }

                // Sliding Cursor cursor representation based on BMI value mapping (from 10f to 40f)
                val coercedBmi = bmi.coerceIn(10f, 40f)
                val activePercentage = (coercedBmi - 10f) / 30f // ratio between 0.0f and 1.0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activePercentage > 0.02f) {
                        Spacer(modifier = Modifier.weight(activePercentage.coerceIn(0.01f, 0.98f)))
                    }
                    Canvas(
                        modifier = Modifier.size(12.dp)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path = path, color = categoryColor)
                    }
                    if (1f - activePercentage > 0.02f) {
                        Spacer(modifier = Modifier.weight((1f - activePercentage).coerceIn(0.01f, 0.98f)))
                    }
                }

                // Subtitle labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("KURANG", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                    Text("IDEAL", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                    Text("LEBIH", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                    Text("OBESITAS", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Grid Stats Row of 2 columns replacing individual panel entries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ideal weight container
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Target",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).padding(bottom = 4.dp)
                        )
                        Text(
                            text = "BERAT BB IDEAL",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f kg", idealWeight),
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Height info / current configuration container
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Ruler",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).padding(bottom = 4.dp)
                        )
                        Text(
                            text = "TINGGI BADAN",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "${height.roundToInt()} cm",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Ideal health range (WHO status) info widget
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "Rentang berat sehat rekomendasi WHO: %.1f - %.1f kg.",
                            idealRange.first,
                            idealRange.second
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 15.sp
                    )
                }
            }

            // Save results action button
            Button(
                onClick = onSaveClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_results_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Simpan",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan Riwayat Berat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun NutritionistCtaCard(
    context: Context,
    currentWeight: Float,
    currentHeight: Float,
    currentBmi: Float,
    currentCategory: String,
    gender: String
) {
    // Pulse animation state representing highly-interactive bold memorable call-out
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nutritionist_cta_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9) // Beautiful, memorable wellness soft green background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, Color(0xFF00C853).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clinical doctor badge avatar circle
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF00C853), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🩺",
                        fontSize = 22.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Layanan Ahli Gizi",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Mulai konsultasi menu diet, asupan kalori & gaya hidup sehat berdasarkan hasil analisis tadi.",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Fast diagnostic interactive WA intent CTA consultation button
            Button(
                onClick = {
                    val message = """
                        Halo Ahli Gizi, saya ingin berkonsultasi mengenai berat badan saya. Berikut data pengukuran terakhir saya:
                        - Jenis Kelamin: $gender
                        - Tinggi Badan: ${currentHeight.roundToInt()} cm
                        - Berat Badan: ${String.format(Locale.US, "%.1f", currentWeight)} kg
                        - Nilai BMI: ${String.format(Locale.US, "%.2f", currentBmi)} ($currentCategory)
                        
                        Mohon rekomendasi program diet sehat dan menu makan bernutrisi yang cocok untuk tubuh saya. Terima kasih.
                    """.trimIndent()

                    val encodedMessage = Uri.encode(message)
                    val whatsappUrl = "https://wa.me/6282298010372?text=$encodedMessage"
                    val dialIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                    
                    try {
                        context.startActivity(dialIntent)
                    } catch (e: Exception) {
                        val fallbackPhoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+6281234567890"))
                        context.startActivity(fallbackPhoneIntent)
                        Toast.makeText(context, "Membuka sambungan telepon konsultasi ahli gizi...", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("consult_nutritionist_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Hubungi",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Konsultasi Ahli Gizi Sekarang",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = "Tersedia 24/7 dengan praktisi berlisensi",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HistoryListSection(
    records: List<BmiRecord>,
    onDeleteRecord: (BmiRecord) -> Unit,
    onClearAll: () -> Unit
) {
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "History Log",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RIWAYAT TERAKHIR",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
            }

            if (records.isNotEmpty()) {
                TextButton(
                    onClick = { showConfirmClearDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Clear All",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Hapus Semua", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (records.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📊",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum Ada Riwayat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Simpan hasil perhitungan berat Anda untuk melihat grafik riwayat perkembangan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // Sort records chronologically helper to compute diffs safely
            val chronologicalList = records.sortedBy { it.timestamp }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                records.take(15).forEach { record ->
                    // Find preceding weight record chronologically older to show weight progression diff!
                    val recordIndex = chronologicalList.indexOf(record)
                    val diffStr = if (recordIndex > 0) {
                        val prevRecord = chronologicalList[recordIndex - 1]
                        val diff = record.weight - prevRecord.weight
                        if (diff > 0) {
                            "+${String.format(Locale.US, "%.1f", diff)} kg"
                        } else if (diff < 0) {
                            "${String.format(Locale.US, "%.1f", diff)} kg"
                        } else {
                            "0.0 kg"
                        }
                    } else {
                        "Awal"
                    }

                    val isLoss = diffStr.startsWith("-")
                    val isGain = diffStr.startsWith("+")
                    val diffColor = when {
                        isLoss -> Color(0xFF00C853) // good weight loss green
                        isGain -> Color(0xFFFF1744) // weight gain red
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }

                    HistoryItemRow(
                        record = record,
                        weightDifference = diffStr,
                        diffColor = diffColor,
                        onDelete = { onDeleteRecord(record) }
                    )
                }
                
                if (records.size > 15) {
                    Text(
                        text = "Menampilkan 15 pengukuran terbaru...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text(text = "Hapus Semua Riwayat?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Tindakan ini tidak bisa dibatalkan. Seluruh riwayat perhitungan berat badan Anda akan dihapus secara permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text(text = "Batal")
                }
            }
        )
    }
}

@Composable
fun HistoryItemRow(
    record: BmiRecord,
    weightDifference: String,
    diffColor: Color,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = sdf.format(Date(record.timestamp))

    // Match status category colors
    val categoryBadgeColor = when {
        record.bmi < 18.5f -> Color(0xFF35A2FF)
        record.bmi in 18.5f..24.9f -> Color(0xFF00C853)
        record.bmi in 25.0f..29.9f -> Color(0xFFFF9100)
        else -> Color(0xFFFF1744)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${record.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circle history visual icon matching HTML spec
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "History Item",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${record.weight.roundToInt()} kg",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Surface(
                            color = categoryBadgeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = record.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = categoryBadgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Right side stats matching high fidelity HTML specification layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = weightDifference,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = diffColor
                    )
                    Text(
                        text = String.format(Locale.US, "BMI: %.1f", record.bmi),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Small modern delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus catatan",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BmiCalculatorTabContent(
    weight: Float,
    height: Float,
    gender: String,
    currentBmi: Float,
    category: String,
    categoryColor: Color,
    idealWeight: Float,
    idealRange: Pair<Float, Float>,
    historyRecords: List<BmiRecord>,
    viewModel: BmiViewModel,
    context: android.content.Context
) {
    // Introduction & Welcome Banner
    WelcomeCard()

    // Gender Selector
    GenderSelector(
        selectedGender = gender,
        onGenderSelected = { viewModel.updateGender(it) }
    )

    // Height Input Slider
    HeightInputCard(
        height = height,
        onHeightChange = { viewModel.updateHeight(it) }
    )

    // Weight Input Slider
    WeightInputCard(
        weight = weight,
        onWeightChange = { viewModel.updateWeight(it) }
    )

    // Dynamic Result Gauge & Ideal Weight Panel
    ResultGaugeCard(
        bmi = currentBmi,
        category = category,
        categoryColor = categoryColor,
        weight = weight,
        height = height,
        idealWeight = idealWeight,
        idealRange = idealRange,
        onSaveClicked = {
            viewModel.saveRecord()
            Toast.makeText(context, "Hasil perhitungan berhasil disimpan!", Toast.LENGTH_SHORT).show()
        }
    )

    // Dynamic Quick Nutritionist CTA Call-Out
    NutritionistCtaCard(
        context = context,
        currentWeight = weight,
        currentHeight = height,
        currentBmi = currentBmi,
        currentCategory = category,
        gender = gender
    )

    // Interactive History Section
    HistoryListSection(
        records = historyRecords,
        onDeleteRecord = { viewModel.deleteRecord(it) },
        onClearAll = { viewModel.clearAllHistory() }
    )
}

@Composable
fun DailyNeedsTabContent(
    weight: Float,
    height: Float,
    gender: String,
    viewModel: BmiViewModel,
    context: android.content.Context
) {
    val age by viewModel.age.collectAsStateWithLifecycle()
    val activityLevel by viewModel.activityLevel.collectAsStateWithLifecycle()
    val nutritionGoal by viewModel.nutritionGoal.collectAsStateWithLifecycle()

    val needs = viewModel.calculateDailyNeeds(weight, height, age, gender, activityLevel, nutritionGoal)

    // Introduction text
    Text(
        text = "HITUNG KEBUTUHAN NUTRISI HARIAN",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    )

    // Age Input Card
    Card(
        modifier = Modifier.fillMaxWidth().testTag("age_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Umur Anda", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("$age Tahun", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { viewModel.updateAge(age - 1) },
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape)
                ) {
                    Text("—", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = age.toFloat(),
                    onValueChange = { viewModel.updateAge(it.roundToInt()) },
                    valueRange = 10f..90f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                IconButton(
                    onClick = { viewModel.updateAge(age + 1) },
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    }

    // Activity level selector
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aktivitas Fisik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val activities = listOf(
            Triple("Sedentary", "Sedentary (Sangat Jarang Olahraga)", "Bekerja kantoran, rebahan, sedikit aktivitas fisik"),
            Triple("Ringan", "Ringan (1-3x Olahraga/Minggu)", "Jalan kaki santai, aktivitas rumah tangga rutin"),
            Triple("Sedang", "Sedang (3-5x Olahraga/Minggu)", "Jogging, renang, atau bersepeda intensitas sedang"),
            Triple("Berat", "Berat (6-7x Olahraga/Minggu)", "Latihan fisik berat harian atau atlet profesional")
        )
        activities.forEach { (level, label, desc) ->
            val isSelected = activityLevel == level
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.updateActivityLevel(level) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = { viewModel.updateActivityLevel(level) })
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    // Goal selector
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Target Kalori", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val goals = listOf(
                Triple("Menurunkan", "Turun BB", "Defisit 500 kcal"),
                Triple("Biasa", "Jaga BB", "Seimbang / Normal"),
                Triple("Menaikkan", "Naik BB", "Surplus 500 kcal")
            )
            goals.forEach { (goal, label, desc) ->
                val isSelected = nutritionGoal == goal
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.updateNutritionGoal(goal) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // Daily Needs output display card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("KEBUTUHAN ENERGI HARIAN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), letterSpacing = 1.5.sp)
            Text("${needs.targetCalories.roundToInt()} Kcal", fontWeight = FontWeight.Black, fontSize = 36.sp, color = MaterialTheme.colorScheme.onPrimary, letterSpacing = (-0.5).sp)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BMR (Metabolisme Basal)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        Text("${needs.bmr.roundToInt()} kcal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TDEE (Energi Aktivitas)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        Text("${needs.tdee.roundToInt()} kcal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Text("REKOMENDASI TARGET MAKRO HARIAN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroItemCard(
                    label = "KARBOHIDRAT",
                    grams = needs.targetCarbs.roundToInt(),
                    calories = (needs.targetCarbs * 4).roundToInt(),
                    color = Color(0xFFE3F2FD),
                    textOnColor = Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f)
                )
                MacroItemCard(
                    label = "PROTEIN",
                    grams = needs.targetProtein.roundToInt(),
                    calories = (needs.targetProtein * 4).roundToInt(),
                    color = Color(0xFFE8F5E9),
                    textOnColor = Color(0xFF1B5E20),
                    modifier = Modifier.weight(1f)
                )
                MacroItemCard(
                    label = "LEMAK",
                    grams = needs.targetFat.roundToInt(),
                    calories = (needs.targetFat * 9).roundToInt(),
                    color = Color(0xFFFFF3E0),
                    textOnColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    viewModel.saveRecord()
                    Toast.makeText(context, "Kebutuhan gizi, usia, dan target harian berhasil direkam sebagai acuan tetap!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_nutrition_needs_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Simpan Kebutuhan",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan Usia & Kebutuhan Gizi",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MealTrackerTabContent(
    foodLogs: List<FoodLog>,
    dailyIntakeRecords: List<DailyIntakeRecord>,
    viewModel: BmiViewModel,
    weight: Float,
    height: Float,
    gender: String,
    context: android.content.Context
) {
    val age by viewModel.age.collectAsStateWithLifecycle()
    val activityLevel by viewModel.activityLevel.collectAsStateWithLifecycle()
    val nutritionGoal by viewModel.nutritionGoal.collectAsStateWithLifecycle()

    val historyRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val latestRecord = historyRecords.firstOrNull()
    val isUsingRecordedBaseline = latestRecord != null && latestRecord.targetCalories > 0f

    val needs = if (isUsingRecordedBaseline && latestRecord != null) {
        DailyNutritionNeeds(
            bmr = latestRecord.bmr,
            tdee = latestRecord.tdee,
            targetCalories = latestRecord.targetCalories,
            targetCarbs = latestRecord.targetCarbs,
            targetProtein = latestRecord.targetProtein,
            targetFat = latestRecord.targetFat
        )
    } else {
        viewModel.calculateDailyNeeds(weight, height, age, gender, activityLevel, nutritionGoal)
    }

    var selectedMealTimeSlot by remember { mutableStateOf("Sarapan") }
    var showRecordConfirmationDialog by remember { mutableStateOf(false) }
    val recordDateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")) }
    var recordDateString by remember { mutableStateOf(recordDateFormatter.format(Date())) }

    val customFoodItems by viewModel.allCustomFoodItems.collectAsStateWithLifecycle()
    var showAddCustomFoodDialog by remember { mutableStateOf(false) }

    val totalCalories = foodLogs.sumOf { it.calories.toDouble() }.toFloat()
    val totalCarbs = foodLogs.sumOf { it.carbs.toDouble() }.toFloat()
    val totalProtein = foodLogs.sumOf { it.protein.toDouble() }.toFloat()
    val totalFat = foodLogs.sumOf { it.fat.toDouble() }.toFloat()

    // Title Comparison
    Text(
        text = "MONITORING NUTRISI HARIAN",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    )

    // Baseline alert/banner
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUsingRecordedBaseline) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isUsingRecordedBaseline) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isUsingRecordedBaseline) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        },
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUsingRecordedBaseline) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = "Standard",
                    tint = if (isUsingRecordedBaseline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUsingRecordedBaseline) "Acuan: Rekor Terekam Sebelumnya" else "Acuan: Perhitungan Harian Aktif (Sementara)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isUsingRecordedBaseline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isUsingRecordedBaseline && latestRecord != null) {
                        "Membandingkan asupan dengan data terekam terakhir: Berat ${latestRecord.weight.roundToInt()}kg, Tinggi ${latestRecord.height.roundToInt()}cm, Usia ${latestRecord.age}thn (Target ${latestRecord.targetCalories.roundToInt()} Kcal)."
                    } else {
                        "Belum ada catatan kebutuhan gizi yang disimpan. Memakai nilai aktif slider: Berat ${weight.roundToInt()}kg, Tinggi ${height.roundToInt()}cm, Usia ${age}thn."
                    },
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }

    // Comparison Dashboard Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Calorie progress bar
            val calRatio = if (needs.targetCalories > 0f) (totalCalories / needs.targetCalories).coerceIn(0f, 1f) else 0f
            val calColor = if (totalCalories > needs.targetCalories) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Energi (Kalori)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "${totalCalories.roundToInt()} / ${needs.targetCalories.roundToInt()} kcal",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = calColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { calRatio },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = calColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            }

            // Macros targets vs intake
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroMiniProgress(
                    label = "Karbohidrat",
                    current = totalCarbs,
                    target = needs.targetCarbs,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                MacroMiniProgress(
                    label = "Protein",
                    current = totalProtein,
                    target = needs.targetProtein,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
                MacroMiniProgress(
                    label = "Lemak",
                    current = totalFat,
                    target = needs.targetFat,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Actions block: SAVE RECORD & EXPORT REPORT PDF
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                if (foodLogs.isNotEmpty()) {
                    recordDateString = recordDateFormatter.format(Date())
                    showRecordConfirmationDialog = true
                } else {
                    Toast.makeText(context, "Tambahkan makanan terlebih dahulu untuk diarsipkan!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Simpan", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan Rekor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                val reportWeight = if (isUsingRecordedBaseline && latestRecord != null) latestRecord.weight else weight
                val reportHeight = if (isUsingRecordedBaseline && latestRecord != null) latestRecord.height else height
                val reportAge = if (isUsingRecordedBaseline && latestRecord != null) latestRecord.age else age
                val reportGoal = if (isUsingRecordedBaseline && latestRecord != null) latestRecord.nutritionGoal else nutritionGoal
                val reportBmi = if (isUsingRecordedBaseline && latestRecord != null) latestRecord.bmi else viewModel.calculateBmi(weight, height)
                val reportCategory = if (isUsingRecordedBaseline && latestRecord != null) latestRecord.category else viewModel.getCategory(reportBmi)

                PdfExporter.exportReport(
                    context = context,
                    weight = reportWeight,
                    height = reportHeight,
                    bmi = reportBmi,
                    category = reportCategory,
                    gender = gender,
                    age = reportAge,
                    nutritionGoal = reportGoal,
                    needs = needs,
                    activeFoods = foodLogs,
                    historyRecords = dailyIntakeRecords
                )
            },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = "PDF Laporan", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }



    // Search and Log TKPI section
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CARI BAHAN MAKANAN (TKPI + KUSTOM)",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        TextButton(
            onClick = { showAddCustomFoodDialog = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("add_custom_food_trigger")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Pangan Baru", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pangan Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(800)
        debouncedSearchQuery = searchQuery
    }

    var selectedFoodItem by remember { mutableStateOf<com.example.data.model.TkpiFoodItem?>(null) }
    var portionGramsStr by remember { mutableStateOf("100") }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Cari nasi, tempe, telur, ayam, buah...") },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Cari") },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )

    // Match TKPI foods
    val filteredFoods = remember(debouncedSearchQuery, customFoodItems) {
        val convertedCustomItems = customFoodItems.map { it.toTkpiFoodItem() }
        val allFoodsCombined = convertedCustomItems + com.example.data.model.TkpiDatabase.items
        if (debouncedSearchQuery.isBlank()) {
            allFoodsCombined.take(8)
        } else {
            allFoodsCombined.filter {
                it.name.contains(debouncedSearchQuery, ignoreCase = true) ||
                it.category.contains(debouncedSearchQuery, ignoreCase = true)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        if (filteredFoods.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Bahan makanan tidak ditemukan",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredFoods.forEach { food ->
                    // Check if is custom food
                    val isCustom = customFoodItems.any { it.name.equals(food.name, ignoreCase = true) && it.category.equals(food.category, ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedFoodItem = food
                                portionGramsStr = "100"
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(food.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(food.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isCustom) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("KUSTOM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Text(" • ${food.caloriesPer100g.roundToInt()} kcal / 100g", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Pilih",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (customFoodItems.isNotEmpty()) {
        var expandedCustomList by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expandedCustomList = !expandedCustomList },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍲", fontSize = 14.sp)
                        }
                        Text(
                            text = "Database Pangan Kustom Anda (${customFoodItems.size} Item)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { expandedCustomList = !expandedCustomList },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expandedCustomList) Icons.Default.ArrowForward else Icons.Default.AddCircle, // Safe fallback icons
                            contentDescription = "Expand/Collapse Custom Foods List",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (expandedCustomList) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customFoodItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${item.category} • ${item.caloriesPer100g.roundToInt()} Kcal (K: ${item.carbsPer100g.roundToInt()}g, P: ${item.proteinPer100g.roundToInt()}g, L: ${item.fatPer100g.roundToInt()}g per 100g)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCustomFoodItem(item) },
                                    modifier = Modifier.size(28.dp).testTag("delete_custom_food_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Pangan Kustom",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomFoodDialog) {
        var foodNameInput by remember { mutableStateOf("") }
        var foodCategoryInput by remember { mutableStateOf("Camilan & Kudapan") }
        var caloriesInput by remember { mutableStateOf("") }
        var proteinInput by remember { mutableStateOf("") }
        var fatInput by remember { mutableStateOf("") }
        var carbsInput by remember { mutableStateOf("") }

        var validationError by remember { mutableStateOf<String?>(null) }

        val categoriesForSelection = listOf(
            "Serealia dan Padi-padian",
            "Lauk Pauk (Daging/Ikan/Telur)",
            "Kacang-kacangan & Olahan",
            "Sayuran & Buah-buahan",
            "Camilan & Kudapan",
            "Minuman & Lainnya"
        )

        AlertDialog(
            onDismissRequest = { showAddCustomFoodDialog = false },
            title = { Text("Tambah Kandungan Gizi Baru Kustom", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Masukkan nilai gizi per 100 gram bahan makanan makanan.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = foodNameInput,
                        onValueChange = { foodNameInput = it },
                        label = { Text("Nama Makanan (Contoh: Bakwan Jagung Cihampelas)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_food_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Dropdown/Selection
                    Text("Kategori:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoriesForSelection.forEach { cat ->
                            val isSelected = foodCategoryInput == cat
                            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgColor)
                                    .clickable { foodCategoryInput = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("custom_cat_$cat"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it },
                        label = { Text("Kalori per 100g (kcal)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_food_cal_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = { carbsInput = it },
                        label = { Text("Karbohidrat per 100g (gram)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_food_carbs_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it },
                        label = { Text("Protein per 100g (gram)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_food_protein_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = fatInput,
                        onValueChange = { fatInput = it },
                        label = { Text("Lemak per 100g (gram)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_food_fat_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    validationError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (foodNameInput.isBlank()) {
                            validationError = "Nama makanan tidak boleh kosong!"
                            return@Button
                        }
                        val c = caloriesInput.toFloatOrNull()
                        val carbs = carbsInput.toFloatOrNull()
                        val prot = proteinInput.toFloatOrNull()
                        val fat = fatInput.toFloatOrNull()

                        if (c == null || carbs == null || prot == null || fat == null) {
                            validationError = "Semua nilai gizi harus diisi dengan angka valid!"
                            return@Button
                        }
                        if (c < 0 || carbs < 0 || prot < 0 || fat < 0) {
                            validationError = "Nilai gizi tidak boleh negatif!"
                            return@Button
                        }

                        viewModel.addCustomFoodItem(
                            name = foodNameInput.trim(),
                            category = foodCategoryInput,
                            caloriesPer100g = c,
                            proteinPer100g = prot,
                            fatPer100g = fat,
                            carbsPer100g = carbs
                        )
                        showAddCustomFoodDialog = false
                        android.widget.Toast.makeText(context, "${foodNameInput.trim()} berhasil ditambahkan ke database kustom!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("dialog_submit_custom_food")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomFoodDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Daily meal diary list
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "DAFTAR WAKTU MAKAN HARI INI",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        if (foodLogs.isNotEmpty()) {
            TextButton(
                onClick = { viewModel.clearAllFoodLogs() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("clear_food_logs_button")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear All", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kosongkan Hari Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Five chronological meal slots
    val mealSlots = listOf(
        Triple("Sarapan", "Sajian makan pagi utama harian Anda", "🌅"),
        Triple("Selingan Pagi", "Camilan/buah di antara sarapan & makan siang", "🍎"),
        Triple("Makan Siang", "Energi dan gizi utama di siang hari", "🍲"),
        Triple("Selingan Sore", "Kudapan sore untuk menekan lapar malam", "🍪"),
        Triple("Makan Malam", "Sajian sehat sebelum istirahat malam", "🌙")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        mealSlots.forEach { (slotName, description, emoji) ->
            val logsInSlot = foodLogs.filter { it.category.equals(slotName, ignoreCase = true) }
            
            if (logsInSlot.isNotEmpty()) {
                // Filled meal slot
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("meal_slot_$slotName"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = slotName,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val totalSlotCal = logsInSlot.sumOf { it.calories.toDouble() }.toInt()
                                    Text(
                                        text = "$totalSlotCal Kcal • ${logsInSlot.size} item tercatat",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    selectedMealTimeSlot = slotName
                                    Toast.makeText(context, "Cari makanan/resep di kolom pencarian di bawah untuk ditambahkan ke $slotName.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("add_more_$slotName")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Tambah lagi",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        logsInSlot.forEach { logItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(logItem.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${logItem.portionGrams.roundToInt()}g • ${logItem.calories.roundToInt()} Kcal (K: ${logItem.carbs.roundToInt()}g, P: ${logItem.protein.roundToInt()}g, L: ${logItem.fat.roundToInt()}g)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteFoodLog(logItem) },
                                    modifier = Modifier.size(32.dp).testTag("delete_${logItem.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus item",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty meal slot card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedMealTimeSlot = slotName
                            Toast.makeText(context, "Mencari resep makanan untuk $slotName. Cari di kolom pencarian di bawah.", Toast.LENGTH_LONG).show()
                        }
                        .testTag("meal_slot_empty_$slotName"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = slotName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                selectedMealTimeSlot = slotName
                                Toast.makeText(context, "Mencari resep makanan untuk $slotName. Cari di kolom pencarian di bawah.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.testTag("add_trigger_$slotName")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Ke $slotName",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Portion modal dialog builder
    selectedFoodItem?.let { food ->
        var chosenMealTime by remember(food) { mutableStateOf(selectedMealTimeSlot) }

        AlertDialog(
            onDismissRequest = { selectedFoodItem = null },
            title = { Text(text = "Porsi & Waktu Makan", fontWeight = FontWeight.Bold) },
            text = {
                var portionGrams = portionGramsStr.toFloatOrNull() ?: 100f
                if (portionGrams <= 0f) portionGrams = 100f
                
                val calcCalories = food.getCaloriesFor(portionGrams)
                val calcCarbs = food.getCarbsFor(portionGrams)
                val calcProtein = food.getProteinFor(portionGrams)
                val calcFat = food.getFatFor(portionGrams)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = food.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    
                    // Interactive Meal Time Selector
                    Text(text = "Pilih Waktu Makan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val options = listOf("Sarapan", "Selingan Pagi", "Makan Siang", "Selingan Sore", "Makan Malam")
                        options.forEach { opt ->
                            val isSelected = chosenMealTime == opt
                            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgColor)
                                    .clickable { chosenMealTime = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("dialog_chip_$opt"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }

                    val existingCount = foodLogs.count { it.category.equals(chosenMealTime, ignoreCase = true) }
                    if (existingCount > 0) {
                        Text(
                            text = "ℹ️ Sudah ada $existingCount makanan tercatat di $chosenMealTime. Makanan baru akan ditambahkan ke daftar.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = portionGramsStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                                portionGramsStr = input
                            }
                        },
                        label = { Text("Berat Makanan (Gram)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("portion_grams_dialog_input"),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quickPortions = listOf("50", "100", "150", "200")
                        quickPortions.forEach { p ->
                            Button(
                                onClick = { portionGramsStr = p },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (portionGramsStr == p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("quick_portion_$p")
                            ) {
                                Text(p + "g", fontSize = 11.sp, color = if (portionGramsStr == p) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Text(text = "Estimasi Kandungan Gizi:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                textTagBox("KALORI")
                                Text("${calcCalories.roundToInt()} Kcal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                textTagBox("KARBO")
                                Text("${calcCarbs.roundToInt()}g", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                textTagBox("PROTEIN")
                                Text("${calcProtein.roundToInt()}g", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                textTagBox("LEMAK")
                                Text("${calcFat.roundToInt()}g", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val portion = portionGramsStr.toFloatOrNull() ?: 100f
                        if (portion > 0f) {
                            viewModel.addFoodLog(food, portion, chosenMealTime)
                            selectedFoodItem = null
                            portionGramsStr = "100"
                            Toast.makeText(context, "${food.name} ditambahkan untuk $chosenMealTime!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialog_submit_button")
                ) {
                    Text(text = "Tambahkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFoodItem = null }) {
                    Text(text = "Batal")
                }
            }
        )
    }

    if (showRecordConfirmationDialog) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                recordDateString = recordDateFormatter.format(selectedCalendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { showRecordConfirmationDialog = false },
            title = { Text(text = "Arsipkan Catatan Makan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Ringkasan asupan makan lengkap harian Anda akan dihitung dan diringkas ke riwayat.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Ringkasan Asupan Hari Ini:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Total Kalori: ${totalCalories.roundToInt()} / ${needs.targetCalories.roundToInt()} kcal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Makro: K ${totalCarbs.roundToInt()}g • P ${totalProtein.roundToInt()}g • L ${totalFat.roundToInt()}g",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Tanggal Pencatatan:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = recordDateString,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Pilih Tanggal"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                                .testTag("record_date_text_field"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveDailyIntake(foodLogs, needs.targetCalories, recordDateString)
                        showRecordConfirmationDialog = false
                        Toast.makeText(context, "Asupan harian berhasil diarsipkan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("dialog_confirm_record_button")
                ) {
                    Text(text = "Simpan Rekor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordConfirmationDialog = false }) {
                    Text(text = "Batal")
                }
            }
        )
    }

    // 5. HISTORY REKOR ASUPAN HARIAN (PREVIOUS DAYS)
    Text(
        text = "RIWAYAT REKOR ASUPAN HARIAN",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp)
    )

    if (dailyIntakeRecords.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📅", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Belum Ada Riwayat Arsip", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Daftar asupan harian yang Anda simpan akan diringkas di sini.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            dailyIntakeRecords.forEach { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(record.dateString, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Berat: ${record.weightAtThatTime.roundToInt()}kg • BMI: ${String.format(Locale.US, "%.1f", record.bmiAtThatTime)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteDailyIntakeRecord(record) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Total Energi Konsumsi",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${record.totalCalories.roundToInt()} / ${record.calorieTarget.roundToInt()} kcal",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (record.totalCalories > record.calorieTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "K / P / L",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${record.totalCarbs.roundToInt()}g / ${record.totalProtein.roundToInt()}g / ${record.totalFat.roundToInt()}g",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (record.foodsSummary.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Menu: ${record.foodsSummary}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Option to clear history
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { viewModel.clearAllDailyIntakeRecords() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus Semua", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Semua Riwayat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun textTagBox(label: String) {
    Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun MacroMiniProgress(
    label: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(8.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${current.roundToInt()} / ${target.roundToInt()}g", fontSize = 11.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun MacroItemCard(
    label: String,
    grams: Int,
    calories: Int,
    color: Color,
    textOnColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textOnColor.copy(alpha = 0.7f)
            )
            Text(
                text = "${grams}g",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = textOnColor
            )
            Text(
                text = "${calories} kcal",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = textOnColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EducationTabContent(
    viewModel: BmiViewModel,
    context: Context
) {
    val articles by viewModel.nutritionArticles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingArticles.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. WhatsApp Nutritionist Consultation Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("whatsapp_consultation_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular WhatsApp Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF25D366), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KONSULTASI GIZI ONLINE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tanya Ahli Gizi Langsung",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hubungi Dietisien profesional kami untuk konsultasi asupan makanan sehat Anda.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            val whatsappUrl = "https://wa.me/6282298010372?text=Halo%20Ahli%20Gizi%20GiziCare,%20saya%20ingin%20berkonsultasi%20mengenai%20menu%20makanan%20dan%20status%20gizi%20saya."
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak dapat membuka WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "WA", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hubungi via WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Row for Education Header & Refresh Online Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ARTIKEL MAHASISWA UNDIP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Edukasi Gizi Sehat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { viewModel.loadArticles() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Muat Ulang",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Article Loading and Listing Space
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (articles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📚", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gagal Memuat Artikel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Silakan klik tombol segarkan di atas untuk mencoba lagi.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                articles.forEachIndexed { index, article ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column {
                            // 1. Article Image Header
                            val imageToLoad = article.imageUrl ?: when (index % 3) {
                                0 -> "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&auto=format&fit=crop"
                                1 -> "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600&auto=format&fit=crop"
                                else -> "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600&auto=format&fit=crop"
                            }

                            AsyncImage(
                                model = imageToLoad,
                                contentDescription = article.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )

                            // 2. Text Information Content
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Author & Date Badge Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Penulis",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = article.creator,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Tanggal",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = article.pubDate,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Article Title
                                Text(
                                    text = article.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )

                                // Excerpt
                                Text(
                                    text = article.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Button Action for Reading Detail
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Selengkapnya di Undip FK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal membuka artikel", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Baca Artikel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Buka",
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "Tentang Aplikasi & Pengembang" Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_app_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tentang Aplikasi",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Tentang Aplikasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))

                Text(
                    text = "Aplikasi GiziCare didesain secara profesional untuk memantau status gizi, menghitung Indeks Massa Tubuh (IMT/BMI), memperkirakan kebutuhan kalori dan zat gizi makro harian, serta melacak asupan pangan harian secara presisi.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                )

                Text(
                    text = "Pengembang Utama:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Pengembang",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Angga Rizqiawan, S.Gz., M.Si.",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dosen dan Peneliti di Bidang Gizi Pangan Berkelanjutan",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
