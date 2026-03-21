package com.alpha.features.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

private enum class CalcMode { STANDARD, PROGRAMMER, LOGIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {

    var display          by remember { mutableStateOf("0") }
    var expression       by remember { mutableStateOf("") }
    var firstOperand     by remember { mutableStateOf<Double?>(null) }
    var operator         by remember { mutableStateOf<String?>(null) }
    var waitingForSecond by remember { mutableStateOf(false) }
    var justCalculated   by remember { mutableStateOf(false) }
    var mode             by remember { mutableStateOf(CalcMode.STANDARD) }
    // PROG mode: which base the user is currently entering in (2/8/10/16)
    var inputBase        by remember { mutableStateOf(10) }

    // ── Helpers ───────────────────────────────────────────────────────────

    fun formatResult(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        return if (v == v.toLong().toDouble() && kotlin.math.abs(v) < 1e15)
            v.toLong().toString()
        else "%.8f".format(v).trimEnd('0').trimEnd('.')
    }

    // Parse display string in the current inputBase → Long
    fun displayAsLong(): Long? =
        display.toLong(inputBase)
            .let { it }
            .runCatching { display.toLong(inputBase) }
            .getOrNull()

    // Convert Long value to display string in inputBase
    fun longToDisplay(v: Long): String = v.toString(inputBase).uppercase()

    // Convert Long to a given base string
    fun toBase(v: Long?, base: Int): String {
        if (v == null) return "—"
        return v.toString(base).uppercase()
    }

    // Get current value as Long for bitwise ops / base display
    fun currentLong(): Long? =
        runCatching { display.toLong(inputBase) }.getOrNull()

    fun onNumber(num: String) {
        when {
            justCalculated   -> { display = num; expression = ""; justCalculated = false }
            waitingForSecond -> { display = num; waitingForSecond = false }
            display == "0"   -> display = num
            else             -> if (display.length < 16) display += num
        }
    }

    fun onDecimal() {
        if (mode != CalcMode.STANDARD) return
        when {
            justCalculated || waitingForSecond -> {
                display = "0."; justCalculated = false; waitingForSecond = false
            }
            !display.contains('.') -> display += "."
        }
    }

    fun onOperator(op: String) {
        justCalculated = false
        // Store operand as DEC Long internally regardless of input base
        firstOperand   = currentLong()?.toDouble() ?: display.toDoubleOrNull()
        operator       = op
        expression     = "${display} $op"
        waitingForSecond = true
    }

    fun calculate() {
        val secondLong = runCatching { display.toLong(inputBase) }.getOrNull()
        val second     = secondLong?.toDouble() ?: display.toDoubleOrNull() ?: return
        val first      = firstOperand ?: return
        val result = when (operator) {
            "+"    -> first + second
            "-"    -> first - second
            "×"    -> first * second
            "÷"    -> if (second != 0.0) first / second else Double.NaN
            "%"    -> first % second
            "//"   -> if (second != 0.0) floor(first / second) else Double.NaN
            "AND"  -> (first.toLong() and  second.toLong()).toDouble()
            "OR"   -> (first.toLong() or   second.toLong()).toDouble()
            "XOR"  -> (first.toLong() xor  second.toLong()).toDouble()
            "NAND" -> (first.toLong() and  second.toLong()).inv().toDouble()
            "NOR"  -> (first.toLong() or   second.toLong()).inv().toDouble()
            "XNOR" -> (first.toLong() xor  second.toLong()).inv().toDouble()
            "SHL"  -> (first.toLong() shl  second.toInt()).toDouble()
            "SHR"  -> (first.toLong() shr  second.toInt()).toDouble()
            else   -> second
        }
        expression   = "$expression $display ="
        // Show result in current inputBase
        val resultLong = result.toLong()
        display = if (result.isNaN() || result.isInfinite()) "Error"
        else longToDisplay(resultLong)
        firstOperand = null; operator = null; justCalculated = true
    }

    fun onClear() {
        display = "0"; expression = ""; firstOperand = null
        operator = null; waitingForSecond = false; justCalculated = false
    }

    fun onBackspace() {
        if (justCalculated) { onClear(); return }
        display = if (display.length > 1) display.dropLast(1) else "0"
    }

    fun onToggleSign() {
        if (mode == CalcMode.PROGRAMMER) {
            val v = currentLong() ?: return
            display = longToDisplay(-v)
        } else {
            val v = display.toDoubleOrNull() ?: return
            display = formatResult(-v)
        }
    }

    fun onNot() {
        val v = currentLong() ?: return
        expression = "NOT($display) ="
        display = longToDisplay(v.inv())
        justCalculated = true
    }

    // Switch input base: re-display current value in new base
    fun switchBase(newBase: Int) {
        val current = currentLong() ?: 0L
        inputBase = newBase
        display = longToDisplay(current)
    }

    // ── Theme ─────────────────────────────────────────────────────────────
    val bgColor    = MaterialTheme.colorScheme.background
    val cardColor  = MaterialTheme.colorScheme.surface
    val primary    = MaterialTheme.colorScheme.primary
    val primaryDim = MaterialTheme.colorScheme.primaryContainer
    val onBg       = MaterialTheme.colorScheme.onBackground
    val onCard     = MaterialTheme.colorScheme.onSurface
    val subtle     = MaterialTheme.colorScheme.onSurfaceVariant
    val specialBg  = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CALCULATOR",
                        color = onBg,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onBg)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(specialBg),
                    ) {
                        CalcMode.entries.forEach { m ->
                            val active = mode == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) primary else specialBg),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = { if (!active) { mode = m; onClear(); if (m != CalcMode.PROGRAMMER) inputBase = 10 } },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (m) {
                                            CalcMode.STANDARD   -> "STD"
                                            CalcMode.PROGRAMMER -> "PROG"
                                            CalcMode.LOGIC      -> "LOGIC"
                                        },
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else subtle,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Display ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(cardColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // PROG: base readouts — each label is a tap target
                    if (mode == CalcMode.PROGRAMMER) {
                        val currentVal = currentLong()
                        listOf(16 to "HEX", 8 to "OCT", 2 to "BIN", 10 to "DEC").forEach { (base, label) ->
                            val isActive = inputBase == base
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tappable base label
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isActive) primary
                                            else subtle.copy(alpha = 0.08f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    TextButton(
                                        onClick = { switchBase(base) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Text(
                                            label,
                                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else subtle,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                                // Value in that base (shows main display if active, conversion otherwise)
                                // FIX: active row now uses same adaptive sizing as STD/LOGIC display
                                val activeText = if (isActive) display else toBase(currentVal, base)
                                Text(
                                    text = activeText,
                                    color = if (isActive) onCard else subtle,
                                    fontSize = if (isActive) {
                                        if (activeText.length > 10) 28.sp else 44.sp
                                    } else 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isActive) FontWeight.Light else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    // Expression
                    if (expression.isNotEmpty()) {
                        Text(
                            expression, color = subtle, fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace, textAlign = TextAlign.End,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Main number (STD / LOGIC modes only — PROG shows it inline above)
                    if (mode != CalcMode.PROGRAMMER) {
                        Text(
                            text = display,
                            color = if (display == "Error") MaterialTheme.colorScheme.error else onCard,
                            fontSize = if (display.length > 10) 28.sp else 44.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── PROG: HEX digit row (A–F) — only shown when inputBase == 16 ──
            if (mode == CalcMode.PROGRAMMER && inputBase == 16) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("A", "B", "C", "D", "E", "F").forEach { hex ->
                        Button(
                            onClick = { onNumber(hex) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryDim,
                                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(hex, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── PROG extra row ────────────────────────────────────────────
            if (mode == CalcMode.PROGRAMMER) {
                ModeRow(
                    labels  = listOf("SHL", "SHR", "//", "%"),
                    bgColor = specialBg,
                    fgColor = subtle,
                    height  = 40.dp,
                    onClick = { onOperator(it) }
                )
            }

            // ── LOGIC rows ────────────────────────────────────────────────
            if (mode == CalcMode.LOGIC) {
                Button(
                    onClick = { onNot() },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryDim,
                        contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("NOT", fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                ModeRow(listOf("AND", "OR", "XOR"),  primaryDim, MaterialTheme.colorScheme.onPrimaryContainer, 40.dp) { onOperator(it) }
                ModeRow(listOf("NAND", "NOR", "XNOR"), specialBg, subtle, 40.dp) { onOperator(it) }
            }

            // ── Standard button grid ──────────────────────────────────────
            val buttonRows = listOf(
                listOf("C", "+/-", "%", "÷"),
                listOf("7", "8",   "9", "×"),
                listOf("4", "5",   "6", "-"),
                listOf("1", "2",   "3", "+"),
                listOf("⌫", "0",   ".", "=")
            )

            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { label ->
                        val isOperator = label in listOf("÷", "×", "-", "+")
                        val isSpecial  = label in listOf("C", "+/-", "%", "⌫")

                        // Hide "." in PROG / LOGIC
                        if (label == "." && mode != CalcMode.STANDARD) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            return@forEach
                        }

                        // In PROG mode, grey out digits that are invalid for the current base
                        val digitValue = label.toIntOrNull()
                        val disabledInBase = mode == CalcMode.PROGRAMMER && digitValue != null &&
                                digitValue >= inputBase

                        Button(
                            onClick = {
                                if (disabledInBase) return@Button
                                when (label) {
                                    "C"   -> onClear()
                                    "⌫"   -> onBackspace()
                                    "+/-" -> onToggleSign()
                                    "."   -> onDecimal()
                                    "="   -> calculate()
                                    in listOf("+", "-", "×", "÷", "%") -> onOperator(label)
                                    else  -> onNumber(label)
                                }
                            },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    disabledInBase -> specialBg.copy(alpha = 0.4f)
                                    label == "="   -> primary
                                    isOperator     -> primaryDim
                                    isSpecial      -> specialBg
                                    else           -> cardColor
                                },
                                contentColor = when {
                                    disabledInBase -> subtle.copy(alpha = 0.3f)
                                    label == "="   -> MaterialTheme.colorScheme.onPrimary
                                    isOperator     -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isSpecial      -> subtle
                                    else           -> onCard
                                }
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(label, fontSize = 22.sp, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    labels:  List<String>,
    bgColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
    height:  androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { label ->
            Button(
                onClick = { onClick(label) },
                modifier = Modifier.weight(1f).height(height),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = fgColor),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(label, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}