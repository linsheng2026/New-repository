package com.cleansweep.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Ink = Color(0xFF18211D)
private val Moss = Color(0xFF1E7B5B)
private val Mint = Color(0xFFD8F3E8)
private val CanvasColor = Color(0xFFF4F7F5)
private val Subtle = Color(0xFF6B7771)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CleanSweepTheme { CleanSweepApp() } }
    }
}

@Composable
private fun CleanSweepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Moss,
            onPrimary = Color.White,
            background = CanvasColor,
            surface = Color.White,
            onSurface = Ink
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}

private enum class ScanState { READY, SCANNING, RESULT, CLEAN }

@Composable
private fun CleanSweepApp() {
    var state by remember { mutableStateOf(ScanState.READY) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = CanvasColor,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                NavItem(Icons.Outlined.Home, "首页", selectedTab == 0) { selectedTab = 0 }
                NavItem(Icons.Outlined.TipsAndUpdates, "建议", selectedTab == 1) { selectedTab = 1 }
                NavItem(Icons.Outlined.Settings, "设置", selectedTab == 2) { selectedTab = 2 }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(padding),
                state = state,
                bytes = cacheBytes,
                onScan = {
                    scope.launch {
                        state = ScanState.SCANNING
                        delay(650)
                        cacheBytes = scanCache(context)
                        state = ScanState.RESULT
                    }
                },
                onClean = {
                    scope.launch {
                        clearCache(context)
                        cacheBytes = 0
                        state = ScanState.CLEAN
                    }
                },
                onSystemStorage = { openSystemStorage(context) }
            )
            1 -> TipsScreen(Modifier.padding(padding), onSystemStorage = { openSystemStorage(context) })
            else -> SettingsScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 12.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Moss,
            selectedTextColor = Moss,
            indicatorColor = Mint,
            unselectedIconColor = Subtle
        )
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    state: ScanState,
    bytes: Long,
    onScan: () -> Unit,
    onClean: () -> Unit,
    onSystemStorage: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(Ink, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("净空", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = Ink)
                Text("让手机轻一点", fontSize = 12.sp, color = Subtle)
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(Color(0xFF45B881), CircleShape))
                    Spacer(Modifier.width(6.dp)); Text("状态良好", fontSize = 12.sp, color = Ink)
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        CleanerCard(state, bytes, onScan, onClean)
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("清理建议", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            Text("安全 · 透明", fontSize = 12.sp, color = Subtle)
        }
        Spacer(Modifier.height(12.dp))
        SuggestionCard(
            icon = Icons.Outlined.FolderDelete,
            color = Color(0xFFFFEFE1),
            title = "应用缓存",
            detail = if (state == ScanState.READY) "扫描后查看可清理内容" else formatBytes(bytes),
            action = if (state == ScanState.RESULT && bytes > 0) "可清理" else "已检查"
        )
        Spacer(Modifier.height(10.dp))
        SuggestionCard(
            icon = Icons.Outlined.Storage,
            color = Color(0xFFE7ECFF),
            title = "系统存储",
            detail = "管理下载、图片与其他应用",
            action = "去管理",
            onClick = onSystemStorage
        )
        Spacer(Modifier.weight(1f))
        Text(
            "净空仅删除你确认的内容，不会触碰照片和聊天记录。",
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            color = Subtle,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CleanerCard(state: ScanState, bytes: Long, onScan: () -> Unit, onClean: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = when (state) { ScanState.READY -> .72f; ScanState.SCANNING -> .92f; ScanState.RESULT -> .84f; ScanState.CLEAN -> 1f },
        label = "clean-progress"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) {
                Text("存储空间", color = Color.White.copy(alpha = .72f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.Shield, null, tint = Color.White.copy(alpha = .7f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp)); Text("安全清理", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.size(178.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 13.dp.toPx()
                    drawArc(Color.White.copy(alpha = .10f), -220f, 260f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(Mint, -220f, 260f * progress, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawCircle(Mint.copy(alpha = .08f), radius = size.minDimension * .32f, center = Offset(size.width / 2, size.height / 2))
                }
                AnimatedContent(state, label = "status") { current ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (current) {
                                ScanState.READY -> "轻盈"
                                ScanState.SCANNING -> "扫描中"
                                ScanState.RESULT -> formatBytes(bytes)
                                ScanState.CLEAN -> "已净空"
                            },
                            color = Color.White, fontSize = if (current == ScanState.RESULT) 26.sp else 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (current) {
                                ScanState.READY -> "点击开始检查"
                                ScanState.SCANNING -> "正在检查缓存"
                                ScanState.RESULT -> if (bytes > 0) "发现可清理缓存" else "没有发现缓存"
                                ScanState.CLEAN -> "缓存已安全清理"
                            }, color = Color.White.copy(alpha = .58f), fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = if (state == ScanState.RESULT && bytes > 0) onClean else onScan,
                enabled = state != ScanState.SCANNING,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink, disabledContainerColor = Mint.copy(alpha = .55f))
            ) {
                if (state == ScanState.SCANNING) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Ink)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    when { state == ScanState.SCANNING -> "正在扫描"; state == ScanState.RESULT && bytes > 0 -> "立即清理"; state == ScanState.CLEAN -> "再次扫描"; else -> "开始扫描" },
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(icon: ImageVector, color: Color, title: String, detail: String, action: String, onClick: (() -> Unit)? = null) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(color, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Ink, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(3.dp)); Text(detail, color = Subtle, fontSize = 12.sp) }
            Text(action, color = Moss, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            if (onClick != null) { Spacer(Modifier.width(3.dp)); Icon(Icons.Outlined.ChevronRight, null, tint = Moss, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun TipsScreen(modifier: Modifier, onSystemStorage: () -> Unit) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Spacer(Modifier.height(20.dp)); Text("空间建议", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("从最有效的地方开始整理", color = Subtle, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
        Tip(Icons.Outlined.Download, "整理下载目录", "旧安装包和重复文件通常藏在这里")
        Tip(Icons.Outlined.PhotoLibrary, "检查大图与视频", "按大小排序，快速释放更多空间")
        Tip(Icons.Outlined.Apps, "管理不常用应用", "Android 会显示应用占用和使用频率")
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onSystemStorage, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("打开系统存储管理") }
    }
}

@Composable private fun Tip(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(42.dp).background(Mint, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Moss, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(13.dp)); Column { Text(title, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(4.dp)); Text(text, color = Subtle, fontSize = 13.sp) }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Spacer(Modifier.height(20.dp)); Text("设置", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(26.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(horizontal = 17.dp)) {
                SettingRow(Icons.Outlined.PrivacyTip, "隐私说明", "所有扫描均在本机完成")
                HorizontalDivider(color = CanvasColor)
                SettingRow(Icons.Outlined.Info, "关于净空", "版本 1.0")
            }
        }
        Spacer(Modifier.height(18.dp)); Text("净空不会上传、分析或出售你的文件信息。受 Android 系统限制，其他应用的缓存需在系统存储页中管理。", color = Subtle, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable private fun SettingRow(icon: ImageVector, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Moss); Spacer(Modifier.width(13.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Text(detail, color = Subtle, fontSize = 12.sp) }
    }
}

private fun cacheRoots(context: Context): List<File> = sequenceOf(context.cacheDir, context.externalCacheDir)
    .filterNotNull()
    .distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }
    .toList()

private suspend fun scanCache(context: Context): Long = withContext(Dispatchers.IO) {
    cacheRoots(context)
        .sumOf { dir -> safeDirSize(dir) }
}

private fun safeDirSize(dir: File): Long = runCatching {
    dir.walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }
}.getOrDefault(0L)

private suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
    cacheRoots(context).forEach { dir ->
        runCatching { deleteDirContents(dir) }
    }
}

private fun deleteDirContents(dir: File) {
    if (!dir.exists() || !dir.isDirectory) return
    dir.listFiles()?.forEach { child ->
        runCatching { child.deleteRecursively() }
    }
}

private fun openSystemStorage(context: Context) {
    val storageIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { context.startActivity(storageIntent) }
        .onFailure {
            runCatching { context.startActivity(fallbackIntent) }
        }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
