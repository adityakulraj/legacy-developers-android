package com.legacydevelopers.app

import android.os.Bundle
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private val Navy = Color(0xFF14283F)
private val Gold = Color(0xFFC69B52)
private val Page = Color(0xFFF4F6F9)
private val Green = Color(0xFF21845B)
private val Red = Color(0xFFB64A4A)

data class GstEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val receiptNo: String,
    val flatNo: String,
    val customer: String,
    val amountReceived: Double,
    val gstAmount: Double,
    val supplyType: String
) {
    val taxableComponent: Double get() = (amountReceived - gstAmount).coerceAtLeast(0.0)
    val cgst: Double get() = if (supplyType == "Intra-state") gstAmount / 2 else 0.0
    val sgst: Double get() = if (supplyType == "Intra-state") gstAmount / 2 else 0.0
    val igst: Double get() = if (supplyType == "Inter-state") gstAmount else 0.0
}

data class FlatRecord(
    val id: String = UUID.randomUUID().toString(),
    val flatNo: String,
    val area: Double,
    val status: String,
    val consideration: Double,
    val paid: Double
) {
    val due: Double get() = (consideration - paid).coerceAtLeast(0.0)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Gold, background = Page)) {
                LegacyApp(
                    load = { readFlats() },
                    save = { writeFlats(it) },
                    loadGst = { readGstEntries() },
                    saveGst = { writeGstEntries(it) },
                    sharePdf = { month, entries -> shareGstPdf(month, entries) }
                )
            }
        }
    }

    private fun readFlats(): List<FlatRecord> {
        val raw = getSharedPreferences("legacy_inventory", MODE_PRIVATE).getString("flats", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                FlatRecord(o.getString("id"), o.getString("flatNo"), o.getDouble("area"), o.getString("status"), o.optDouble("consideration", 0.0), o.optDouble("paid", 0.0))
            }.getOrNull()
        }
    }

    private fun readGstEntries(): List<GstEntry> {
        val raw = getSharedPreferences("legacy_inventory", MODE_PRIVATE).getString("gst_entries", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i -> runCatching {
            val o = arr.getJSONObject(i)
            GstEntry(o.getString("id"), o.getString("date"), o.getString("receiptNo"), o.getString("flatNo"), o.optString("customer"), o.getDouble("amountReceived"), o.getDouble("gstAmount"), o.getString("supplyType"))
        }.getOrNull() }
    }

    private fun writeGstEntries(entries: List<GstEntry>) {
        val arr = JSONArray()
        entries.forEach { e -> arr.put(JSONObject().apply {
            put("id", e.id); put("date", e.date); put("receiptNo", e.receiptNo); put("flatNo", e.flatNo)
            put("customer", e.customer); put("amountReceived", e.amountReceived); put("gstAmount", e.gstAmount); put("supplyType", e.supplyType)
        }) }
        getSharedPreferences("legacy_inventory", MODE_PRIVATE).edit().putString("gst_entries", arr.toString()).apply()
    }

    private fun shareGstPdf(month: String, entries: List<GstEntry>) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val navy = android.graphics.Color.rgb(20, 40, 63)
        val gold = android.graphics.Color.rgb(198, 155, 82)
        val black = android.graphics.Color.rgb(40, 45, 52)
        val grey = android.graphics.Color.rgb(105, 112, 120)
        val cgst = entries.sumOf { it.cgst }; val sgst = entries.sumOf { it.sgst }; val igst = entries.sumOf { it.igst }
        val totalReceived = entries.sumOf { it.amountReceived }; val totalTax = cgst + sgst + igst
        var pageNo = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        var canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun drawHeader() {
            paint.color = navy; canvas.drawRect(0f, 0f, pageWidth.toFloat(), 105f, paint)
            paint.color = gold; paint.textSize = 20f; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("LEGACY DEVELOPERS", margin, 42f, paint)
            paint.color = android.graphics.Color.WHITE; paint.textSize = 10f; paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText("CUSTOMER PAYMENT GST COLLECTION REPORT", margin, 63f, paint)
            paint.textSize = 11f; canvas.drawText("Reporting month: $month", margin, 84f, paint)
        }
        drawHeader()
        var y = 132f
        fun line(label: String, value: String, bold: Boolean = false) {
            paint.color = black; paint.textSize = 10f; paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText(label, margin, y, paint); canvas.drawText(value, pageWidth - margin - paint.measureText(value), y, paint); y += 18f
        }
        paint.color = grey; paint.textSize = 9f; canvas.drawText("PAYMENT SUMMARY", margin, y, paint); y += 16f
        line("Number of receipts", entries.size.toString(), true)
        line("Total customer payments received", money(totalReceived), true)
        line("Taxable component recorded", money(entries.sumOf { it.taxableComponent }))
        line("CGST", money(cgst)); line("SGST", money(sgst)); line("IGST", money(igst))
        line("Total GST component recorded", money(totalTax), true)
        y += 12f
        fun tableHeader() {
            paint.color = navy; canvas.drawRect(margin, y - 13f, pageWidth - margin, y + 8f, paint)
            paint.color = android.graphics.Color.WHITE; paint.textSize = 8f; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("DATE", margin + 5f, y, paint); canvas.drawText("RECEIPT", 103f, y, paint)
            canvas.drawText("FLAT / CUSTOMER", 180f, y, paint); canvas.drawText("RECEIVED", 365f, y, paint)
            canvas.drawText("GST", 470f, y, paint); y += 22f
        }
        paint.color = grey; paint.textSize = 9f; canvas.drawText("PAYMENT-WISE DETAILS", margin, y, paint); y += 16f
        tableHeader()
        entries.sortedBy { it.date }.forEach { e ->
            if (y > 775f) {
                pdf.finishPage(page); pageNo++; page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create()); canvas = page.canvas; drawHeader(); y = 135f; tableHeader()
            }
            paint.color = black; paint.textSize = 8f; paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText(e.date, margin + 5f, y, paint)
            canvas.drawText(e.receiptNo.take(13), 103f, y, paint)
            canvas.drawText((e.flatNo + " / " + e.customer).take(30), 180f, y, paint)
            canvas.drawText(money(e.amountReceived), 365f, y, paint)
            canvas.drawText(money(e.gstAmount), 470f, y, paint)
            y += 18f
            paint.color = android.graphics.Color.LTGRAY; canvas.drawLine(margin, y - 8f, pageWidth - margin, y - 8f, paint)
        }
        y = minOf(y + 22f, 800f)
        paint.color = grey; paint.textSize = 8f
        canvas.drawText("Prepared from customer payment receipts recorded in the Legacy Developers app.", margin, y, paint)
        canvas.drawText("For accounts-team review and manual GST filing. Verify tax treatment with books and applicable GST rules.", margin, y + 13f, paint)
        canvas.drawText("This is a payment collection statement, not a statutory GST return.", margin, y + 26f, paint)
        pdf.finishPage(page)
        val outDir = File(cacheDir, "shared_reports").apply { mkdirs() }
        val outFile = File(outDir, "Legacy_Developers_GST_Payments_$month.pdf")
        outFile.outputStream().use { pdf.writeTo(it) }; pdf.close()
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", outFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(Intent.EXTRA_SUBJECT, "Legacy Developers - GST Payment Report - $month")
            putExtra(Intent.EXTRA_TEXT, "Monthly customer payment and GST collection statement for $month.")
            putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share monthly GST PDF"))
    }

    private fun writeFlats(flats: List<FlatRecord>) {
        val arr = JSONArray()
        flats.forEach { f -> arr.put(JSONObject().apply {
            put("id", f.id); put("flatNo", f.flatNo); put("area", f.area); put("status", f.status)
            put("consideration", f.consideration); put("paid", f.paid)
        }) }
        getSharedPreferences("legacy_inventory", MODE_PRIVATE).edit().putString("flats", arr.toString()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyApp(load: () -> List<FlatRecord>, save: (List<FlatRecord>) -> Unit,
                      loadGst: () -> List<GstEntry>, saveGst: (List<GstEntry>) -> Unit, sharePdf: (String, List<GstEntry>) -> Unit) {
    var flats by remember { mutableStateOf(load()) }
    var gstEntries by remember { mutableStateOf(loadGst()) }
    var tab by remember { mutableStateOf(0) }
    var gstEditorOpen by remember { mutableStateOf(false) }
    fun commitGst(next: List<GstEntry>) { gstEntries = next; saveGst(next) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FlatRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<FlatRecord?>(null) }
    fun commit(next: List<FlatRecord>) { flats = next; save(next) }

    Scaffold(
        containerColor = Page,
        topBar = {
            Column(Modifier.fillMaxWidth().background(Navy).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(width = 72.dp, height = 60.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Image(
                            painter = painterResource(R.drawable.legacy_developers_logo),
                            contentDescription = "Legacy Developers logo",
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("LEGACY DEVELOPERS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text("RESIDENTIAL PROJECT MANAGEMENT", color = Color(0xFFD5DCE5), fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Project Overview", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("Flat inventory & payment tracker", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }
        },
        floatingActionButton = {
            if (tab == 3) FloatingActionButton(onClick = { gstEditorOpen = true }, containerColor = Gold, contentColor = Navy) {
                Icon(Icons.Default.Add, contentDescription = "Add customer payment")
            } else FloatingActionButton(onClick = { editing = null; editorOpen = true }, containerColor = Gold, contentColor = Navy) {
                Icon(Icons.Default.Add, contentDescription = "Add flat")
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Overview") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Apartment, null) }, label = { Text("All Flats") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Payments, null) }, label = { Text("Bookings") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("GST Report") })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> Overview(flats, onAll = { tab = 1 }, onBooked = { tab = 2 }, onUnsold = { tab = 1 })
                1 -> FlatListScreen(flats, filter = "All", onEdit = { editing = it; editorOpen = true }, onDelete = { deleteTarget = it })
                2 -> BookingsScreen(flats, onEdit = { editing = it; editorOpen = true }, onDelete = { deleteTarget = it })
                else -> GstReportScreen(gstEntries, onAdd = { gstEditorOpen = true }, onExport = { month, rows -> sharePdf(month, rows) })
            }
        }
    }

    if (gstEditorOpen) GstEntryDialog(onDismiss = { gstEditorOpen = false }, onSave = { entry -> commitGst(gstEntries + entry); gstEditorOpen = false })
    if (editorOpen) FlatEditorDialog(editing, onDismiss = { editorOpen = false }, onSave = { record ->
        val next = if (editing == null) flats + record else flats.map { if (it.id == record.id) record else it }
        commit(next); editorOpen = false
    })
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete flat ${target.flatNo}?") },
            text = { Text("This will remove this flat and its payment details from this device.") },
            confirmButton = { TextButton(onClick = { commit(flats.filterNot { it.id == target.id }); deleteTarget = null }) { Text("Delete", color = Red) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } })
    }
}

@Composable
private fun Overview(flats: List<FlatRecord>, onAll: () -> Unit, onBooked: () -> Unit, onUnsold: () -> Unit) {
    val booked = flats.filter { it.status == "Booked" }
    val unsold = flats.filter { it.status == "Unsold" }
    val totalValue = booked.sumOf { it.consideration }
    val paid = booked.sumOf { it.paid }
    val due = booked.sumOf { it.due }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("TOTAL FLATS", flats.size.toString(), "Inventory", Modifier.weight(1f), Navy)
            MetricCard("BOOKED", booked.size.toString(), "${percent(booked.size, flats.size)}% of inventory", Modifier.weight(1f), Green)
            MetricCard("UNSOLD", unsold.size.toString(), "Available", Modifier.weight(1f), Gold)
        } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("BOOKING COLLECTIONS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    FinanceLine("Total consideration", money(totalValue), Navy)
                    FinanceLine("Amount received", money(paid), Green)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Page)
                    FinanceLine("Amount outstanding", money(due), Red, true)
                }
            }
        }
        item { Text("QUICK ACCESS", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp) }
        item { QuickCard("All flats inventory", "View flat numbers and super built-up areas", "${flats.size} flats", onAll) }
        item { QuickCard("Booked flats", "Track consideration, payments and dues", "${booked.size} booked", onBooked) }
        item { QuickCard("Unsold flats", "View available flats and their areas", "${unsold.size} available", onUnsold) }
        if (flats.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E9)), shape = RoundedCornerShape(14.dp)) {
                Text("No flats added yet. Tap the + button to enter your project's inventory. All figures on this dashboard will update automatically.", Modifier.padding(16.dp), color = Navy, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, note: String, modifier: Modifier, accent: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
            Text(value, fontSize = 27.sp, color = accent, fontWeight = FontWeight.Bold)
            Text(note, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun FinanceLine(label: String, value: String, color: Color, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (strong) Navy else Color.DarkGray, fontSize = 13.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = color, fontSize = if (strong) 17.sp else 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickCard(title: String, subtitle: String, count: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(subtitle, color = Color.Gray, fontSize = 11.sp) }
            Text(count, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FlatListScreen(flats: List<FlatRecord>, filter: String, onEdit: (FlatRecord) -> Unit, onDelete: (FlatRecord) -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = flats.filter { it.flatNo.contains(search, true) }.sortedBy { it.flatNo }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("All Flats", fontSize = 22.sp, color = Navy, fontWeight = FontWeight.Bold)
        Text("Flat number and super built-up area", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search flat number") }, singleLine = true, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) EmptyState("No flats to display", "Add your first flat using the + button.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
            items(filtered, key = { it.id }) { FlatCard(it, onEdit, onDelete, showPayment = it.status == "Booked") }
        }
    }
}

@Composable
private fun BookingsScreen(flats: List<FlatRecord>, onEdit: (FlatRecord) -> Unit, onDelete: (FlatRecord) -> Unit) {
    var mode by remember { mutableStateOf("Booked") }
    val records = flats.filter { it.status == mode }.sortedBy { it.flatNo }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Flat Status", fontSize = 22.sp, color = Navy, fontWeight = FontWeight.Bold)
        Text("Booked payment details and unsold inventory", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Booked", "Unsold").forEachIndexed { index, item ->
                SegmentedButton(selected = mode == item, onClick = { mode = item }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(item) }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (records.isEmpty()) EmptyState("No ${mode.lowercase()} flats", "Add a flat or change its status to $mode.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
            items(records, key = { it.id }) { FlatCard(it, onEdit, onDelete, showPayment = mode == "Booked") }
        }
    }
}

@Composable
private fun FlatCard(flat: FlatRecord, onEdit: (FlatRecord) -> Unit, onDelete: (FlatRecord) -> Unit, showPayment: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FLAT NO. ${flat.flatNo}", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${area(flat.area)} sq ft super built-up", color = Color.Gray, fontSize = 12.sp)
                }
                Surface(color = if (flat.status == "Booked") Color(0xFFE6F4EC) else Color(0xFFFFF3D9), shape = RoundedCornerShape(20.dp)) {
                    Text(flat.status, Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = if (flat.status == "Booked") Green else Color(0xFF946B1D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (showPayment) {
                HorizontalDivider(Modifier.padding(vertical = 11.dp), color = Page)
                FinanceLine("Total consideration", money(flat.consideration), Navy)
                Spacer(Modifier.height(6.dp)); FinanceLine("Amount paid", money(flat.paid), Green)
                Spacer(Modifier.height(6.dp)); FinanceLine("Amount due", money(flat.due), Red, true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onEdit(flat) }) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
                TextButton(onClick = { onDelete(flat) }) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = Red); Spacer(Modifier.width(4.dp)); Text("Delete", color = Red) }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Apartment, null, tint = Gold, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp)); Text(title, color = Navy, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.Gray, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlatEditorDialog(existing: FlatRecord?, onDismiss: () -> Unit, onSave: (FlatRecord) -> Unit) {
    var flatNo by remember { mutableStateOf(existing?.flatNo ?: "") }
    var areaText by remember { mutableStateOf(existing?.area?.toString() ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "Unsold") }
    var considerationText by remember { mutableStateOf(existing?.consideration?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var paidText by remember { mutableStateOf(existing?.paid?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var error by remember { mutableStateOf("") }
    val consideration = considerationText.toDoubleOrNull() ?: 0.0
    val paid = paidText.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Flat" else "Edit Flat", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(flatNo, { flatNo = it; error = "" }, label = { Text("Flat number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(areaText, { areaText = it }, label = { Text("Super built-up area (sq ft)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Flat status", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = status == "Unsold", onClick = { status = "Unsold" }, label = { Text("Unsold") })
                    FilterChip(selected = status == "Booked", onClick = { status = "Booked" }, label = { Text("Booked") })
                }
                if (status == "Booked") {
                    OutlinedTextField(considerationText, { considerationText = it }, label = { Text("Total consideration (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(paidText, { paidText = it }, label = { Text("Amount paid (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Amount due: ${money((consideration - paid).coerceAtLeast(0.0))}", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (error.isNotBlank()) Text(error, color = Red, fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = {
            val a = areaText.toDoubleOrNull()
            if (flatNo.isBlank() || a == null || a <= 0) { error = "Enter a flat number and a valid area."; return@Button }
            if (status == "Booked" && (consideration <= 0 || paid < 0 || paid > consideration)) { error = "Enter valid consideration and paid amount (paid cannot exceed consideration)."; return@Button }
            onSave(FlatRecord(existing?.id ?: UUID.randomUUID().toString(), flatNo.trim(), a, status, if (status == "Booked") consideration else 0.0, if (status == "Booked") paid else 0.0))
        }) { Text("Save Flat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
private fun GstReportScreen(entries: List<GstEntry>, onAdd: () -> Unit, onExport: (String, List<GstEntry>) -> Unit) {
    var month by remember { mutableStateOf(java.time.LocalDate.now().toString().take(7)) }
    val monthEntries = entries.filter { it.date.startsWith(month) }.sortedBy { it.date }
    val cgst = monthEntries.sumOf { it.cgst }; val sgst = monthEntries.sumOf { it.sgst }; val igst = monthEntries.sumOf { it.igst }
    val totalTax = cgst + sgst + igst
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Monthly GST Payment Report", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("GST component of customer payments received during the selected month", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(month, { month = it }, Modifier.fillMaxWidth(), label = { Text("Reporting month (YYYY-MM)") }, singleLine = true)
            Text("Enter month as YYYY-MM, e.g. 2026-11", color = Color.Gray, fontSize = 10.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MONTHLY COLLECTION SUMMARY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    FinanceLine("Receipts recorded", monthEntries.size.toString(), Navy)
                    FinanceLine("Total customer payments received", money(monthEntries.sumOf { it.amountReceived }), Green, true)
                    FinanceLine("Taxable component recorded", money(monthEntries.sumOf { it.taxableComponent }), Navy)
                    FinanceLine("CGST", money(cgst), Navy); FinanceLine("SGST", money(sgst), Navy); FinanceLine("IGST", money(igst), Navy)
                    HorizontalDivider(color = Page)
                    FinanceLine("Total GST component recorded", money(totalTax), Red, true)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("Add payment") }
                OutlinedButton(onClick = { onExport(month, monthEntries) }, enabled = monthEntries.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.ReceiptLong, null); Text("Share PDF") }
            }
        }
        item { Text("PAYMENT DETAILS · ${monthEntries.size}", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        if (monthEntries.isEmpty()) item { EmptyState("No customer payments for this month", "Add each customer receipt to include it in the monthly PDF report.") }
        items(monthEntries, key = { it.id }) { e ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Receipt ${e.receiptNo} · Flat ${e.flatNo}", color = Navy, fontWeight = FontWeight.Bold); Text("${e.date} · ${e.customer}", color = Color.Gray, fontSize = 11.sp) }
                        Text(e.supplyType, color = Gold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    FinanceLine("Payment received", money(e.amountReceived), Green, true)
                    FinanceLine("Taxable component", money(e.taxableComponent), Navy)
                    FinanceLine("GST component", money(e.gstAmount), Red)
                    FinanceLine(if (e.supplyType == "Intra-state") "CGST / SGST" else "IGST", money(e.gstAmount), Navy)
                }
            }
        }
        item { Text("Report basis: payment date and GST amount recorded against each customer receipt. This PDF is a supporting payment collection statement for accounts-team review and manual filing, not a statutory GST return. Confirm applicable time-of-supply and tax treatment with your accountant.", color = Color.Gray, fontSize = 11.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GstEntryDialog(onDismiss: () -> Unit, onSave: (GstEntry) -> Unit) {
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var receipt by remember { mutableStateOf("") }; var flat by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }; var received by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }; var supply by remember { mutableStateOf("Intra-state") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record Customer Payment", color = Navy, fontWeight = FontWeight.Bold) },
        text = { Column(Modifier.heightIn(max = 540.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                item { OutlinedTextField(date, { date = it }, label = { Text("Payment received date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(receipt, { receipt = it }, label = { Text("Receipt number") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(flat, { flat = it }, label = { Text("Flat number") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(customer, { customer = it }, label = { Text("Customer name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(received, { received = it }, label = { Text("Total payment received (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(gst, { gst = it }, label = { Text("GST component in this receipt (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { Text("Supply type", color = Color.Gray, fontSize = 12.sp) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = supply == "Intra-state", onClick = { supply = "Intra-state" }, label = { Text("Intra-state") })
                    FilterChip(selected = supply == "Inter-state", onClick = { supply = "Inter-state" }, label = { Text("Inter-state") })
                } }
                item { Text(if (supply == "Intra-state") "GST component will be split equally into CGST and SGST in the report." else "GST component will be shown as IGST in the report.", color = Color.Gray, fontSize = 11.sp) }
                if (error.isNotBlank()) item { Text(error, color = Red, fontSize = 12.sp) }
            }
        } },
        confirmButton = { Button(onClick = {
            val d = runCatching { java.time.LocalDate.parse(date) }.getOrNull()
            val amount = received.toDoubleOrNull(); val tax = gst.toDoubleOrNull()
            if (d == null || receipt.isBlank() || flat.isBlank() || amount == null || amount <= 0 || tax == null || tax < 0 || tax > amount) { error = "Enter a valid date, receipt, flat, payment amount and GST component (not greater than payment)."; return@Button }
            onSave(GstEntry(date = date, receiptNo = receipt.trim(), flatNo = flat.trim(), customer = customer.trim(), amountReceived = amount, gstAmount = tax, supplyType = supply))
        }) { Text("Save Payment") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
private fun area(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
private fun percent(part: Int, total: Int): Int = if (total == 0) 0 else (part * 100.0 / total).toInt()
