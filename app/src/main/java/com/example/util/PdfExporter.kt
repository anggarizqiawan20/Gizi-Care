package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.DailyIntakeRecord
import com.example.data.entity.FoodLog
import com.example.ui.viewmodel.DailyNutritionNeeds
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object PdfExporter {

    fun exportReport(
        context: Context,
        weight: Float,
        height: Float,
        bmi: Float,
        category: String,
        gender: String,
        age: Int,
        nutritionGoal: String,
        needs: DailyNutritionNeeds,
        activeFoods: List<FoodLog>,
        historyRecords: List<DailyIntakeRecord>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size 595 x 842 @ 72 dpi
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            // Colors for theme (Space Slate & Emerald Teal)
            val colorPrimary = Color.parseColor("#0D5C75") // Deep Cyan Teal
            val colorSecondary = Color.parseColor("#14B8A6") // Sea Green Mint
            val colorBgLight = Color.parseColor("#F3F4F6") // Soft light grey
            val colorTextDark = Color.parseColor("#1F2937") // Charcoal Dark
            val colorTextLight = Color.parseColor("#6B7280") // Grey Neutral
            val colorCategory = when {
                bmi < 18.5f -> Color.parseColor("#35A2FF") // Underweight Blue
                bmi in 18.5f..24.9f -> Color.parseColor("#00C853") // Normal Green
                bmi in 25.0f..29.9f -> Color.parseColor("#FF9100") // Overweight Orange
                else -> Color.parseColor("#FF1744") // Obese Red
            }

            // Margin configuration
            val leftMargin = 40f
            val rightMargin = 555f
            var currentY = 50f

            // 1. HEADER SECTION (Beautiful cyan accents)
            paint.color = colorPrimary
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 45f, paint)

            textPaint.apply {
                color = Color.WHITE
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("BODYMETRIC AI - LAPORAN KESEHATAN & ASUPAN NUTRISI", leftMargin + 15f, currentY + 27f, textPaint)

            currentY += 60f

            // Sub-header details
            textPaint.apply {
                color = colorTextLight
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val formatter = SimpleDateFormat("EEEE, d MMMM yyyy (HH:mm)", Locale("id", "ID"))
            canvas.drawText("Dibuat pada: ${formatter.format(Date())}", leftMargin, currentY, textPaint)
            canvas.drawText("Aplikasi Bodymetric AI v1.0", rightMargin - 120f, currentY, textPaint)

            currentY += 20f

            // Divider Line
            paint.color = Color.parseColor("#E5E7EB")
            canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)

            currentY += 20f

            // 2. PHYSICAL PROFILE (PROFIL FISIK USER)
            textPaint.apply {
                color = colorPrimary
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("I. PROFIL FISIK USER", leftMargin, currentY, textPaint)
            currentY += 12f

            // Draw a rounded card background for profile
            paint.color = colorBgLight
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 65f, paint)

            textPaint.apply {
                color = colorTextDark
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Column 1
            canvas.drawText("Jenis Kelamin: $gender", leftMargin + 15f, currentY + 20f, textPaint)
            canvas.drawText("Hingga Umur: $age Tahun", leftMargin + 15f, currentY + 35f, textPaint)
            canvas.drawText("Tinggi Badan: ${height.roundToInt()} cm", leftMargin + 15f, currentY + 50f, textPaint)

            // Column 2
            canvas.drawText("Berat Badan: ${weight.roundToInt()} kg", leftMargin + 200f, currentY + 20f, textPaint)
            canvas.drawText("Tujuan Diet: $nutritionGoal", leftMargin + 200f, currentY + 35f, textPaint)
            
            // BMI Highlights
            canvas.drawText("Index Massa Tubuh (BMI): ${String.format(Locale.US, "%.1f", bmi)}", leftMargin + 360f, currentY + 20f, textPaint)
            
            // Category highlighted
            textPaint.color = colorCategory
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Kategori BMI: $category", leftMargin + 360f, currentY + 38f, textPaint)

            currentY += 85f

            // 3. DAILY NUTRITIONAL NEEDS (KEBUTUHAN GIZI HARIAN)
            textPaint.apply {
                color = colorPrimary
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("II. TARGET KEBUTUHAN ENERGI & MAKRONUTRISI", leftMargin, currentY, textPaint)
            currentY += 12f

            // Draw target cards
            paint.color = Color.parseColor("#E6FFFA") // Very light mint
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 55f, paint)

            textPaint.apply {
                color = colorTextDark
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            canvas.drawText("Angka BMR (Basal Metabolic Rate): ${needs.bmr.roundToInt()} kcal", leftMargin + 15f, currentY + 18f, textPaint)
            canvas.drawText("Kebutuhan TDEE Sebenarnya: ${needs.tdee.roundToInt()} kcal", leftMargin + 15f, currentY + 38f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = colorPrimary
            canvas.drawText("Target Kalori Harian: ${needs.targetCalories.roundToInt()} kcal", leftMargin + 250f, currentY + 18f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = colorTextDark
            canvas.drawText("• Karbohidrat: ${needs.targetCarbs.roundToInt()}g (60%)", leftMargin + 250f, currentY + 35f, textPaint)
            canvas.drawText("• Protein: ${needs.targetProtein.roundToInt()}g (15%)", leftMargin + 380f, currentY + 35f, textPaint)
            canvas.drawText("• Lemak: ${needs.targetFat.roundToInt()}g (25%)", leftMargin + 480f, currentY + 35f, textPaint)

            currentY += 75f

            // 4. ACTIVE FOOD INTAKE TODAY
            textPaint.apply {
                color = colorPrimary
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("III. RINCIAN PENCATATAN ASUPAN MAKANAN HARIAN", leftMargin, currentY, textPaint)
            currentY += 15f

            if (activeFoods.isEmpty()) {
                paint.apply {
                    color = colorBgLight
                    style = Paint.Style.FILL
                }
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 35f, paint)

                textPaint.apply {
                    color = colorTextLight
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                canvas.drawText("Belum ada menu catatan asupan makanan hari ini.", leftMargin + 15f, currentY + 22f, textPaint)
                currentY += 55f
            } else {
                val mealGroups = listOf(
                    "Sarapan" to "🌅 Sarapan",
                    "Selingan Pagi" to "🍎 Selingan Pagi",
                    "Makan Siang" to "🍲 Makan Siang",
                    "Selingan Sore" to "🍪 Selingan Sore",
                    "Makan Malam" to "🌙 Makan Malam"
                )

                // Measure required dynamic height first so we can draw background and border
                textPaint.apply {
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                
                var sectionHeight = 15f // Initial padding
                mealGroups.forEach { (slotKey, _) ->
                    val foodsInSlot = activeFoods.filter { it.category.equals(slotKey, ignoreCase = true) }
                    if (foodsInSlot.isNotEmpty()) {
                        sectionHeight += 14f // Slot Title height
                        foodsInSlot.forEach { food ->
                            val detailText = "• ${food.foodName} (${food.portionGrams.roundToInt()}g): ${food.calories.roundToInt()} Kcal (K: ${food.carbs.roundToInt()}g, P: ${food.protein.roundToInt()}g, L: ${food.fat.roundToInt()}g)"
                            val lines = countWrappedLines(detailText, rightMargin - (leftMargin + 45f), textPaint)
                            sectionHeight += lines * 12f
                        }
                        sectionHeight += 4f // small spacing
                    }
                }
                sectionHeight += 24f // For divider, summary line & padding

                // Draw filled card background
                paint.apply {
                    color = colorBgLight
                    style = Paint.Style.FILL
                }
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + sectionHeight, paint)

                // Draw a subtle border
                paint.apply {
                    color = Color.parseColor("#E5E7EB")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + sectionHeight, paint)

                // Draw text inside Section III card
                var contentY = currentY + 15f
                var totalCaloriesToday = 0f

                paint.style = Paint.Style.FILL // Reset to fill

                mealGroups.forEach { (slotKey, slotLabel) ->
                    val foodsInSlot = activeFoods.filter { it.category.equals(slotKey, ignoreCase = true) }
                    if (foodsInSlot.isNotEmpty()) {
                        val slotCal = foodsInSlot.sumOf { it.calories.toDouble() }.roundToInt()

                        // Draw meal slot category title
                        textPaint.apply {
                            color = colorPrimary
                            textSize = 9.5f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        canvas.drawText("$slotLabel ($slotCal Kcal)", leftMargin + 15f, contentY, textPaint)
                        contentY += 14f

                        // Draw each food item with word wrap
                        textPaint.apply {
                            color = colorTextDark
                            textSize = 8.5f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        }
                        foodsInSlot.forEach { food ->
                            totalCaloriesToday += food.calories
                            val detailText = "• ${food.foodName} (${food.portionGrams.roundToInt()}g): ${food.calories.roundToInt()} Kcal (K: ${food.carbs.roundToInt()}g, P: ${food.protein.roundToInt()}g, L: ${food.fat.roundToInt()}g)"
                            
                            contentY = drawWrappedText(
                                canvas = canvas,
                                text = detailText,
                                x = leftMargin + 30f,
                                startY = contentY,
                                maxWidth = rightMargin - (leftMargin + 45f),
                                paint = textPaint,
                                lineHeight = 12f
                            )
                        }
                        contentY += 4f
                    }
                }

                // Divider line before summary
                contentY += 2f
                paint.color = Color.parseColor("#E5E7EB")
                canvas.drawLine(leftMargin + 15f, contentY, rightMargin - 15f, contentY, paint)
                contentY += 14f

                // Draw Section III summary
                textPaint.apply {
                    color = colorPrimary
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val remainingCal = (needs.targetCalories - totalCaloriesToday).coerceAtLeast(0f).roundToInt()
                canvas.drawText("Total Kalori Terkonsumsi: ${totalCaloriesToday.roundToInt()} Kcal / Target: ${needs.targetCalories.roundToInt()} Kcal (Sisa Saku: $remainingCal Kcal)", leftMargin + 15f, contentY, textPaint)
                
                currentY += sectionHeight + 18f
            }

            // 5. HISTORY DAILY INTAKE RECORDS
            textPaint.apply {
                color = colorPrimary
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("IV. RIWAYAT ARSIP ASUPAN NUTRISI HARIAN", leftMargin, currentY, textPaint)
            currentY += 15f

            // History table headers
            paint.apply {
                color = colorPrimary
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 20f, paint)

            textPaint.apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Tanggal", leftMargin + 10f, currentY + 13f, textPaint)
            canvas.drawText("Target", leftMargin + 120f, currentY + 13f, textPaint)
            canvas.drawText("Asupan Kalori", leftMargin + 180f, currentY + 13f, textPaint)
            canvas.drawText("Karbo / Prot / Lemak", leftMargin + 250f, currentY + 13f, textPaint)
            canvas.drawText("Ringkasan Menu Makanan", leftMargin + 370f, currentY + 13f, textPaint)

            currentY += 20f

            textPaint.apply {
                color = colorTextDark
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            if (historyRecords.isEmpty()) {
                paint.apply {
                    color = Color.parseColor("#F9FAFB")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 40f, paint)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("Belum ada rekor asupan harian yang disimpan / diarsipkan.", leftMargin + 140f, currentY + 25f, textPaint)
            } else {
                // Dynamically adjust history rows based on activeFoods count to never overflow the page
                val maxHistoryRows = if (activeFoods.size > 2) 5 else 8
                val displayHistory = historyRecords.take(maxHistoryRows)
                
                displayHistory.forEachIndexed { index, record ->
                    // Alternate row background shades
                    paint.apply {
                        color = if (index % 2 == 0) Color.WHITE else Color.parseColor("#F9FAFB")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 22f, paint)

                    canvas.drawText(record.dateString, leftMargin + 10f, currentY + 14f, textPaint)
                    canvas.drawText("${record.calorieTarget.roundToInt()} kcal", leftMargin + 120f, currentY + 14f, textPaint)
                    
                    // Bold color for actual calories
                    val textCol = if (record.totalCalories > record.calorieTarget) Color.parseColor("#DC2626") else colorPrimary
                    paint.color = textCol
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.color = textCol
                    canvas.drawText("${record.totalCalories.roundToInt()} kcal", leftMargin + 180f, currentY + 14f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.color = colorTextDark
                    canvas.drawText("${record.totalCarbs.roundToInt()}g / ${record.totalProtein.roundToInt()}g / ${record.totalFat.roundToInt()}g", leftMargin + 250f, currentY + 14f, textPaint)

                    // Truncate summary labels so they never overflow the table columns or the page margins
                    val summaryLabel = if (record.foodsSummary.length > 30) {
                        record.foodsSummary.take(28) + "..."
                    } else {
                        record.foodsSummary
                    }
                    canvas.drawText(summaryLabel, leftMargin + 370f, currentY + 14f, textPaint)

                    currentY += 22f
                }
            }

            // Footer notes and certification
            currentY = 800f
            paint.apply {
                color = Color.parseColor("#E5E7EB")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)

            textPaint.apply {
                color = colorTextLight
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText("Disusun secara otomatis oleh Bodymetric AI. Gunakan sebagai acuan gizi terstruktur mandiri.", leftMargin, currentY + 15f, textPaint)
            canvas.drawText("Halaman 1 dari 1", rightMargin - 70f, currentY + 15f, textPaint)

            pdfDocument.finishPage(page)

            // Save PDF to Cache for clean sharing
            val file = File(context.cacheDir, "Laporan_Kesehatan_Bodymetric.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Open the PDF share chooser
            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Bodymetric AI")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // High compatibility chooser options to open / send
            val chooser = Intent.createChooser(openIntent, "Buka Laporan PDF").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(shareIntent))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = startY
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)
            if (textWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, currentY, paint)
                    currentY += lineHeight
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += lineHeight
        }
        return currentY
    }

    private fun countWrappedLines(text: String, maxWidth: Float, paint: Paint): Int {
        val words = text.split(" ")
        var currentLine = ""
        var lineCount = 0
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lineCount++
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lineCount++
        }
        return if (lineCount == 0) 1 else lineCount
    }
}
