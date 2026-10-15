package com.karamdev.tpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Forest = Color(0xFF173F35)
private val Ivory = Color(0xFFF5F0E6)
private val Gold = Color(0xFFC5A15B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TplApp() }
    }
}

@Composable
private fun TplApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("tpl_preferences", 0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedBranch by remember {
        mutableStateOf(preferences.getString("student_branch", "العلمي") ?: "العلمي")
    }
    val tabs = listOf("الرئيسية", "المواد", "الاختبارات", "أدواتي")
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Gold, background = Ivory, surface = Color.White)) {
        Scaffold(
            containerColor = Ivory,
            topBar = {
                Row(Modifier.fillMaxWidth().background(Forest).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TPL", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Tawjihi Palestine Library", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                    }
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) { Text("الفرع: $selectedBranch", color = Color.White) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("العلمي", "الأدبي", "الصناعي").forEach { branch ->
                                DropdownMenuItem(text = { Text(branch) }, onClick = {
                                    selectedBranch = branch
                                    preferences.edit().putString("student_branch", branch).apply()
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    val icons = listOf(Icons.Default.Home, Icons.Default.MenuBook, Icons.Default.Quiz, Icons.Default.Timer)
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index }, icon = { Icon(icons[index], contentDescription = label) }, label = { Text(label, fontSize = 10.sp) })
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding), selectedBranch, onBrowseSubjects = { selectedTab = 1 })
                1 -> SubjectsScreen(Modifier.padding(padding), selectedBranch)
                2 -> PlaceholderScreen(Modifier.padding(padding), "الاختبارات", "سيظهر هنا بنك الأسئلة والاختبارات بعد ربط قاعدة البيانات. لن تُعرض الإجابات الصحيحة قبل إنهاء المحاولة.", Icons.Default.Quiz)
                else -> PomodoroScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, branch: String, onBrowseSubjects: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(22.dp)).padding(20.dp)) {
                Text("خطوتك القادمة تبدأ من هنا", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("مكتبتك الدراسية للفرع $branch، بترتيب أوضح ووقت أقل ضائع في البحث.", color = Color.White.copy(alpha = .88f), lineHeight = 22.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Gold, shape = RoundedCornerShape(12.dp), modifier = Modifier.clickable(onClick = onBrowseSubjects)) {
                        Text("تصفح المواد", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Forest, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                        Text("مجاني للطلاب", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color.White)
                    }
                }
            }
        }
        item { SectionTitle("أقسام المكتبة", "ابدأ بالمادة التي تدرسها الآن") }
        items(listOf("الكتب والمقررات", "الملازم والملخصات", "أوراق العمل", "الامتحانات السابقة")) { title ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).clickable(onClick = onBrowseSubjects).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Forest, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, Modifier.weight(1f), color = Forest, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Gold)
            }
        }
        item { Text("المحتوى المعروض هنا تمهيدي. ستظهر الموارد الحقيقية عند ربط Firebase ولوحة الإدارة.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
    }
}

@Composable
private fun SubjectsScreen(modifier: Modifier = Modifier, branch: String) {
    val subjects = if (branch == "العلمي") listOf("الرياضيات", "الفيزياء", "الكيمياء", "الأحياء", "اللغة العربية", "اللغة الإنجليزية")
    else if (branch == "الأدبي") listOf("اللغة العربية", "اللغة الإنجليزية", "التاريخ", "الجغرافيا", "الرياضيات", "التربية الإسلامية")
    else listOf("اللغة العربية", "اللغة الإنجليزية", "الرياضيات", "التخصص المهني", "الثقافة العلمية")
    var search by remember { mutableStateOf("") }
    val filtered = subjects.filter { it.contains(search.trim(), ignoreCase = true) }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("مواد الفرع $branch", "ابحث عن مادة. القائمة الحالية تجريبية وليست منهجًا رسميًا كاملًا.") }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن مادة...") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp)
            )
        }
        if (filtered.isEmpty()) {
            item { Text("لا توجد مادة بهذا الاسم في القائمة التجريبية.", color = Color.Gray, modifier = Modifier.padding(12.dp)) }
        }
        items(filtered) { subject ->
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Ivory, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.MenuBook, contentDescription = null, tint = Forest) }
                Spacer(Modifier.width(12.dp))
                Text(subject, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Forest)
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Gold)
            }
        }
    }
}

@Composable
private fun PomodoroScreen(modifier: Modifier) {
    var focusMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isBreak by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (remainingSeconds == 0) isRunning = false
    }
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = Forest, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("مؤقت بومودورو", color = Forest, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(if (isBreak) "وقت استراحة قصيرة" else "جلسة تركيز", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Text(String.format("%02d:%02d", minutes, seconds), color = Forest, fontSize = 54.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        if (!isRunning) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45).forEach { value ->
                    FilterChip(selected = focusMinutes == value && !isBreak, onClick = {
                        focusMinutes = value
                        isBreak = false
                        remainingSeconds = value * 60
                    }, label = { Text("$value دقيقة") })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { if (remainingSeconds == 0) remainingSeconds = (if (isBreak) 5 else focusMinutes) * 60; isRunning = !isRunning }) {
                Text(if (isRunning) "إيقاف مؤقت" else "ابدأ")
            }
            OutlinedButton(onClick = { isRunning = false; isBreak = false; remainingSeconds = focusMinutes * 60 }) { Text("إعادة ضبط") }
        }
        if (remainingSeconds == 0) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = { isBreak = !isBreak; remainingSeconds = (if (isBreak) focusMinutes else 5) * 60 }) {
                Text(if (isBreak) "ابدأ جلسة تركيز" else "ابدأ استراحة 5 دقائق")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("المؤقت يعمل محليًا على الجهاز ولا يحتاج إلى حساب.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlaceholderScreen(modifier: Modifier, title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Forest, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Color.DarkGray, textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(title, color = Forest, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Color.Gray, fontSize = 12.sp)
    }
}
