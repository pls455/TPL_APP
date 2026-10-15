package com.karamdev.tpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay

private val Forest = Color(0xFF173F35)
private val Ivory = Color(0xFFF5F0E6)
private val Gold = Color(0xFFC5A15B)

private data class Branch(val id: String, val name: String, val sortOrder: Int)
private data class Subject(val id: String, val name: String, val branchIds: List<String>, val sortOrder: Int)
private data class LibraryResource(val id: String, val title: String, val description: String, val type: String, val url: String)
private data class PublishedTest(
    val id: String, val title: String, val description: String, val subjectId: String,
    val subjectName: String, val type: String, val durationMinutes: Int, val branchIds: List<String>
)
private data class StudentQuestion(
    val id: String, val questionText: String, val type: String,
    val options: List<String>, val points: Double
)

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
    val db = remember { FirebaseFirestore.getInstance() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val branches = remember { mutableStateListOf<Branch>() }
    var branchesLoading by remember { mutableStateOf(true) }
    var branchesError by remember { mutableStateOf<String?>(null) }
    var selectedBranchId by remember {
        mutableStateOf(preferences.getString("student_branch_id", "") ?: "")
    }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedTest by remember { mutableStateOf<PublishedTest?>(null) }

    DisposableEffect(db) {
        val registration: ListenerRegistration = db.collection("branches")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    branchesLoading = false
                    branchesError = "تعذّر تحميل الفروع من السحابة. تحقق من الإنترنت وقواعد Firestore."
                    return@addSnapshotListener
                }
                val updated = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val name = document.getString("name")?.trim().orEmpty()
                    if (name.isBlank()) null
                    else Branch(document.id, name, (document.get("order") as? Number)?.toInt() ?: 9999)
                }.sortedWith(compareBy<Branch> { it.sortOrder }.thenBy { it.name })
                branches.clear()
                branches.addAll(updated)
                branchesLoading = false
                branchesError = null
                if (branches.none { it.id == selectedBranchId }) {
                    selectedBranchId = branches.firstOrNull()?.id.orEmpty()
                    selectedSubject = null
                    preferences.edit().putString("student_branch_id", selectedBranchId).apply()
                }
            }
        onDispose { registration.remove() }
    }

    val selectedBranch = branches.firstOrNull { it.id == selectedBranchId }
    val tabs = listOf("الرئيسية", "المواد", "الاختبارات", "أدواتي")
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Gold, background = Ivory, surface = Color.White)) {
        Scaffold(
            containerColor = Ivory,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().background(Forest).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("TPL", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Text("مكتبة التوجيهي الفلسطيني", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                    }
                    Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                            Text("تعلم بترتيب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    val icons = listOf(Icons.Default.Home, Icons.Default.MenuBook, Icons.Default.Quiz, Icons.Default.Timer)
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) +
                        slideInHorizontally(animationSpec = tween(180)) { it / 18 }) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "tpl-screen-transition"
            ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    branches = branches,
                    selectedBranchId = selectedBranchId,
                    branchName = selectedBranch?.name,
                    branchesLoading = branchesLoading,
                    branchesError = branchesError,
                    onBranchSelected = { branch ->
                        selectedBranchId = branch.id
                        selectedSubject = null
                        selectedTest = null
                        preferences.edit().putString("student_branch_id", branch.id).apply()
                    },
                    onBrowseSubjects = { selectedTab = 1 }
                )
                1 -> if (selectedSubject == null) {
                    SubjectsScreen(
                        modifier = Modifier.padding(padding),
                        branch = selectedBranch,
                        branches = branches,
                        selectedBranchId = selectedBranchId,
                        onBranchSelected = { branch ->
                            selectedBranchId = branch.id
                            selectedSubject = null
                            selectedTest = null
                            preferences.edit().putString("student_branch_id", branch.id).apply()
                        },
                        onSubjectSelected = { selectedSubject = it }
                    )
                } else {
                    SubjectResourcesScreen(Modifier.padding(padding), selectedBranch, selectedSubject!!, onBack = { selectedSubject = null })
                }
                2 -> if (selectedTest == null) {
                    PublishedTestsScreen(
                        modifier = Modifier.padding(padding),
                        branch = selectedBranch,
                        onTestSelected = { selectedTest = it }
                    )
                } else {
                    StudentTestScreen(
                        modifier = Modifier.padding(padding),
                        test = selectedTest!!,
                        onBack = { selectedTest = null }
                    )
                }
                else -> PomodoroScreen(Modifier.padding(padding))
            }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    branches: List<Branch>,
    selectedBranchId: String,
    branchName: String?,
    branchesLoading: Boolean,
    branchesError: String?,
    onBranchSelected: (Branch) -> Unit,
    onBrowseSubjects: () -> Unit
) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Forest, RoundedCornerShape(22.dp)).padding(20.dp)) {
                Text("خطوتك القادمة تبدأ من هنا", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (branchName != null) "مكتبتك الدراسية للفرع " + branchName + "، والبيانات المنشورة تُحمّل من السحابة."
                    else "اختر فرعك بعد أن تضيف الإدارة الفروع وتنشرها من لوحة TPL.",
                    color = Color.White.copy(alpha = .88f), lineHeight = 22.sp
                )
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
        item {
            BranchPickerCard(
                branches = branches,
                selectedBranchId = selectedBranchId,
                loading = branchesLoading,
                onBranchSelected = onBranchSelected
            )
        }
        if (branchesError != null) {
            item { Text(branchesError, color = Color(0xFF9B3737), fontSize = 12.sp) }
        } else if (branchName == null && !branchesLoading) {
            item {
                InfoCard(
                    title = "بانتظار إعداد المكتبة",
                    body = "لم تنشر الإدارة أي فرع بعد. عند إضافة فرع وتفعيله من لوحة الإدارة سيظهر هنا تلقائيًا.",
                    icon = Icons.Default.Cloud
                )
            }
        }
        item { SectionTitle("كيف تبدأ؟", "ثلاث خطوات واضحة للوصول إلى محتوى فرعك") }
        items(listOf(
            "١. اختر فرعك الدراسي من البطاقة أعلاه",
            "٢. افتح تبويب المواد واختر المادة المطلوبة",
            "٣. افتح المورد المنشور وابدأ الدراسة"
        )) { step ->
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp))
                    .padding(horizontal = 15.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(11.dp))
                Text(step, Modifier.weight(1f), color = Forest, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BranchPickerCard(
    branches: List<Branch>,
    selectedBranchId: String,
    loading: Boolean,
    onBranchSelected: (Branch) -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(42.dp).background(Ivory, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("فرعك الدراسي", color = Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        loading -> "جارٍ تحميل الفروع من السحابة..."
                        branches.isEmpty() -> "لا توجد فروع منشورة حاليًا"
                        else -> "اختر الفرع لعرض مواده المناسبة"
                    },
                    color = Color.Gray, fontSize = 11.sp
                )
            }
            Icon(Icons.Default.Tune, contentDescription = null, tint = Gold)
        }
        AnimatedVisibility(visible = branches.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                branches.forEach { branch ->
                    val selected = branch.id == selectedBranchId
                    val container by animateColorAsState(
                        targetValue = if (selected) Forest else Ivory,
                        label = "branch-chip-color"
                    )
                    val content by animateColorAsState(
                        targetValue = if (selected) Color.White else Forest,
                        label = "branch-chip-content"
                    )
                    Surface(
                        color = container,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.animateContentSize().clickable { onBranchSelected(branch) }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                            Text(branch.name, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectsScreen(
    modifier: Modifier = Modifier,
    branch: Branch?,
    branches: List<Branch>,
    selectedBranchId: String,
    onBranchSelected: (Branch) -> Unit,
    onSubjectSelected: (Subject) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val subjectItems = remember(branch?.id) { mutableStateListOf<Subject>() }
    var loading by remember(branch?.id) { mutableStateOf(branch != null) }
    var error by remember(branch?.id) { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    DisposableEffect(db, branch?.id) {
        subjectItems.clear()
        if (branch == null) {
            loading = false
            error = null
            onDispose { }
        } else {
            loading = true
            val registration = db.collection("subjects")
                .whereEqualTo("active", true)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        loading = false
                        error = "تعذّر تحميل المواد. تحقق من اتصال Firebase وقواعد القراءة."
                        return@addSnapshotListener
                    }
                    val updated = snapshot?.documents.orEmpty().mapNotNull { document ->
                        val name = document.getString("name")?.trim().orEmpty()
                        val branchIds = (document.get("branchIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                        if (name.isBlank() || branch.id !in branchIds) null
                        else Subject(document.id, name, branchIds, (document.get("order") as? Number)?.toInt() ?: 9999)
                    }.sortedWith(compareBy<Subject> { it.sortOrder }.thenBy { it.name })
                    subjectItems.clear()
                    subjectItems.addAll(updated)
                    loading = false
                    error = null
                }
            onDispose { registration.remove() }
        }
    }

    val filtered = subjectItems.filter { it.name.contains(search.trim(), ignoreCase = true) }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BranchPickerCard(
                branches = branches,
                selectedBranchId = selectedBranchId,
                loading = false,
                onBranchSelected = onBranchSelected
            )
        }
        item {
            SectionTitle(
                if (branch != null) "مواد " + branch.name else "المواد الدراسية",
                "اختر المادة لعرض مواردها المنشورة."
            )
        }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن مادة...") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp), enabled = branch != null
            )
        }
        if (branch == null) {
            item {
                InfoCard(
                    "لا يوجد فرع متاح",
                    "أضف فرعًا واجعله نشطًا من لوحة الإدارة، وسيظهر في قائمة الفروع تلقائيًا.",
                    Icons.Default.CloudOff
                )
            }
        } else if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest) }
            item { Text("جارٍ تحميل المواد من السحابة...", color = Color.Gray, fontSize = 13.sp) }
        } else if (error != null) {
            item { Text(error!!, color = Color(0xFF9B3737), fontSize = 13.sp) }
        } else if (filtered.isEmpty()) {
            item {
                InfoCard(
                    if (search.isBlank()) "لم تُضف مواد لهذا الفرع بعد" else "لا توجد نتائج",
                    if (search.isBlank()) "تضيف الإدارة المواد وتربط كل مادة بفرع أو أكثر من لوحة TPL."
                    else "جرّب اسم مادة آخر.",
                    Icons.Default.MenuBook
                )
            }
        }
        items(filtered, key = { it.id }) { subject ->
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).clickable { onSubjectSelected(subject) }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).background(Ivory, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Forest)
                }
                Spacer(Modifier.width(12.dp))
                Text(subject.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Forest)
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Gold)
            }
        }
    }
}


@Composable
private fun SubjectResourcesScreen(
    modifier: Modifier,
    branch: Branch?,
    subject: Subject,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uriHandler = LocalUriHandler.current
    val resourceItems = remember(subject.id, branch?.id) { mutableStateListOf<LibraryResource>() }
    var loading by remember(subject.id, branch?.id) { mutableStateOf(branch != null) }
    var error by remember(subject.id, branch?.id) { mutableStateOf<String?>(null) }

    DisposableEffect(db, subject.id, branch?.id) {
        resourceItems.clear()
        if (branch == null) {
            loading = false
            error = null
            onDispose { }
        } else {
            loading = true
            val registration = db.collection("resources")
                .whereEqualTo("status", "published")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        loading = false
                        error = "تعذّر تحميل موارد المادة. تحقق من اتصال Firebase وقواعد القراءة."
                        return@addSnapshotListener
                    }
                    val updated = snapshot?.documents.orEmpty().mapNotNull { document ->
                        val title = document.getString("title")?.trim().orEmpty()
                        val subjectId = document.getString("subjectId").orEmpty()
                        val branchIds = (document.get("branchIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                        val url = (document.getString("externalUrl") ?: document.getString("sourceUrl")).orEmpty()
                        if (title.isBlank() || subjectId != subject.id || branch.id !in branchIds || url.isBlank()) null
                        else LibraryResource(
                            id = document.id,
                            title = title,
                            description = document.getString("description").orEmpty(),
                            type = document.getString("type").orEmpty(),
                            url = url
                        )
                    }.sortedBy { it.title }
                    resourceItems.clear()
                    resourceItems.addAll(updated)
                    loading = false
                    error = null
                }
            onDispose { registration.remove() }
        }
    }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("العودة إلى المواد")
            }
            SectionTitle(subject.name, "الموارد المنشورة لهذه المادة تظهر هنا من السحابة.")
        }
        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest) }
            item { Text("جارٍ تحميل الموارد...", color = Color.Gray, fontSize = 13.sp) }
        } else if (error != null) {
            item { Text(error!!, color = Color(0xFF9B3737), fontSize = 13.sp) }
        } else if (resourceItems.isEmpty()) {
            item {
                InfoCard(
                    "لا توجد موارد منشورة بعد",
                    "يمكن للإدارة رفع ملف أو إضافة رابط للمادة من لوحة TPL. لن يظهر المورد هنا حتى يتم اعتماده ونشره.",
                    Icons.Default.FolderOpen
                )
            }
        }
        items(resourceItems, key = { it.id }) { resource ->
            val uri = android.net.Uri.parse(resource.url)
            val validUrl = (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
            Column(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(resource.title, color = Forest, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    when (resource.type) {
                        "pdf" -> "ملف PDF"
                        "video" -> "فيديو"
                        "exam" -> "امتحان سابق"
                        "summary" -> "ملخص"
                        "image" -> "صورة"
                        "document" -> "مستند Word"
                        "presentation" -> "عرض تقديمي"
                        "spreadsheet" -> "ملف Excel"
                        "audio" -> "ملف صوتي"
                        "archive" -> "ملف مضغوط"
                        else -> "رابط خارجي"
                    },
                    color = Color(0xFF8D713B), fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
                if (resource.description.isNotBlank()) {
                    Text(resource.description, color = Color.DarkGray, fontSize = 13.sp, lineHeight = 20.sp)
                }
                Button(
                    enabled = validUrl,
                    onClick = { uriHandler.openUri(resource.url) },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("فتح المورد")
                }
            }
        }
    }
}

@Composable
private fun PublishedTestsScreen(
    modifier: Modifier,
    branch: Branch?,
    onTestSelected: (PublishedTest) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val tests = remember(branch?.id) { mutableStateListOf<PublishedTest>() }
    var loading by remember(branch?.id) { mutableStateOf(branch != null) }
    var error by remember(branch?.id) { mutableStateOf<String?>(null) }

    DisposableEffect(db, branch?.id) {
        tests.clear()
        if (branch == null) {
            loading = false
            error = null
            onDispose { }
        } else {
            loading = true
            val registration = db.collection("tests")
                .whereEqualTo("published", true)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        loading = false
                        error = "تعذّر تحميل الاختبارات. تحقق من الاتصال وقواعد Firestore."
                        return@addSnapshotListener
                    }
                    val updated = snapshot?.documents.orEmpty().mapNotNull { document ->
                        val title = document.getString("title")?.trim().orEmpty()
                        val branchIds = (document.get("branchIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                        val active = document.getBoolean("active") ?: true
                        if (title.isBlank() || !active || branch.id !in branchIds) null
                        else PublishedTest(
                            id = document.id,
                            title = title,
                            description = document.getString("description").orEmpty(),
                            subjectId = document.getString("subjectId").orEmpty(),
                            subjectName = document.getString("subjectName").orEmpty(),
                            type = document.getString("type").orEmpty(),
                            durationMinutes = (document.getLong("durationMinutes") ?: 0L).toInt(),
                            branchIds = branchIds
                        )
                    }.sortedWith(compareBy<PublishedTest> { it.subjectName }.thenBy { it.title })
                    tests.clear()
                    tests.addAll(updated)
                    loading = false
                    error = null
                }
            onDispose { registration.remove() }
        }
    }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle("الاختبارات", if (branch == null) "اختر فرعك أولًا لعرض اختبارته المنشورة." else "اختبارات منشورة لفرع ${branch.name}")
        }
        if (branch == null) {
            item { InfoCard("اختر فرعك الدراسي", "ارجع إلى الرئيسية وحدد الفرع المناسب، ثم افتح الاختبارات.", Icons.Default.School) }
        } else if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest) }
            item { Text("جارٍ تحميل الاختبارات المنشورة...", color = Color.Gray, fontSize = 13.sp) }
        } else if (error != null) {
            item { Text(error!!, color = Color(0xFF9B3737), fontSize = 13.sp) }
        } else if (tests.isEmpty()) {
            item { InfoCard("لا توجد اختبارات منشورة بعد", "الاختبارات التي تنشئها الإدارة لا تظهر هنا إلا بعد إضافة سؤال واحد على الأقل ثم اختيار «نشر للطلاب».", Icons.Default.Quiz) }
        }
        items(tests, key = { it.id }) { test ->
            Column(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Ivory, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = Forest)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(test.title, color = Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(test.subjectName, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Text(
                    when (test.type) {
                        "timed" -> "اختبار بوقت"
                        "past_paper" -> "ورقة امتحان سابقة"
                        else -> "تدريب"
                    } + if (test.durationMinutes > 0) " · ${test.durationMinutes} دقيقة" else " · دون مؤقت",
                    color = Color(0xFF8D713B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
                if (test.description.isNotBlank()) Text(test.description, color = Color.DarkGray, fontSize = 13.sp, lineHeight = 20.sp)
                Button(
                    onClick = { onTestSelected(test) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("فتح الاختبار")
                }
            }
        }
    }
}

@Composable
private fun StudentTestScreen(
    modifier: Modifier,
    test: PublishedTest,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val questions = remember(test.id) { mutableStateListOf<StudentQuestion>() }
    val answers = remember(test.id) { mutableStateMapOf<String, String>() }
    var loading by remember(test.id) { mutableStateOf(true) }
    var error by remember(test.id) { mutableStateOf<String?>(null) }
    var submitted by remember(test.id) { mutableStateOf(false) }

    DisposableEffect(db, test.id) {
        questions.clear()
        loading = true
        val registration = db.collection("studentQuestions")
            .whereEqualTo("testId", test.id)
            .whereEqualTo("published", true)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    loading = false
                    error = "تعذّر تحميل أسئلة الاختبار. تأكد أن الإدارة نشرت نسخة الأسئلة المخصصة للطلاب."
                    return@addSnapshotListener
                }
                val updated = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val text = document.getString("questionText")?.trim().orEmpty()
                    if (text.isBlank()) null
                    else StudentQuestion(
                        id = document.id,
                        questionText = text,
                        type = document.getString("type").orEmpty(),
                        options = (document.get("options") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                        points = document.getDouble("points") ?: 1.0
                    )
                }
                questions.clear()
                questions.addAll(updated)
                loading = false
                error = null
            }
        onDispose { registration.remove() }
    }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("العودة إلى الاختبارات")
            }
            SectionTitle(test.title, test.subjectName + " · " + if (test.durationMinutes > 0) "${test.durationMinutes} دقيقة" else "دون مؤقت")
            Text("حدّد إجاباتك. لن يتم إرسال مفتاح الإجابات الصحيحة إلى جهازك.", color = Color.Gray, fontSize = 12.sp)
        }
        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest) }
        } else if (error != null) {
            item { Text(error!!, color = Color(0xFF9B3737), fontSize = 13.sp) }
        } else if (questions.isEmpty()) {
            item { InfoCard("لا توجد أسئلة متاحة", "أعد نشر الاختبار من لوحة الإدارة بعد التأكد من وجود أسئلة نشطة.", Icons.Default.Info) }
        } else {
            itemsIndexed(questions, key = { _, question -> question.id }) { index, question ->
                Column(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("السؤال ${index + 1} · ${question.points} علامة", color = Color(0xFF8D713B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(question.questionText, color = Forest, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp)
                    if (question.options.isNotEmpty()) {
                        question.options.forEach { option ->
                            Row(
                                Modifier.fillMaxWidth().clickable { answers[question.id] = option; submitted = false }.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = answers[question.id] == option,
                                    onClick = { answers[question.id] = option; submitted = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = Forest)
                                )
                                Text(option, color = Color.DarkGray, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = answers[question.id].orEmpty(),
                            onValueChange = { answers[question.id] = it; submitted = false },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("إجابتك") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { submitted = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest)
                ) {
                    Text("إنهاء تحديد الإجابات")
                }
            }
            if (submitted) {
                item {
                    InfoCard(
                        "تم إنهاء الاختيار",
                        "تم تجهيز إجاباتك داخل الشاشة الحالية. التصحيح الآلي وحفظ المحاولة بالحساب يحتاجان خدمة تحقق آمنة، لذلك لا نعرض علامة غير موثوقة ولا نرسل الإجابات الصحيحة للتطبيق.",
                        Icons.Default.CheckCircle
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Forest, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(body, color = Color.Gray, fontSize = 12.sp, lineHeight = 19.sp)
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
            Button(onClick = {
                if (isBreak) {
                    isBreak = false
                    remainingSeconds = focusMinutes * 60
                } else {
                    isBreak = true
                    remainingSeconds = 5 * 60
                }
            }) {
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
