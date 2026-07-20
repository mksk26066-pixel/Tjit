package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

data class GridPos(val x: Int, val y: Int)

enum class Dir(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0)
}

data class ArrowShape(
    val id: Int,
    val path: List<GridPos>,
    val dir: Dir,
    val color: Color
)

class ShapeState(val shape: ArrowShape) {
    val offset = Animatable(0f)
    var isEscaping by mutableStateOf(false)
    var isHinted by mutableStateOf(false)
}

val Level1 = listOf(
    ArrowShape(1, listOf(GridPos(2, 2), GridPos(3, 2)), Dir.RIGHT, Color(0xFF1D2951)),
    ArrowShape(2, listOf(GridPos(4, 3), GridPos(4, 2), GridPos(4, 1)), Dir.UP, Color(0xFF1D2951)),
    ArrowShape(3, listOf(GridPos(4, 4), GridPos(3, 4), GridPos(2, 4)), Dir.LEFT, Color(0xFF1D2951))
)

val Level2 = listOf(
    ArrowShape(1, listOf(GridPos(2, 5), GridPos(2, 4), GridPos(2, 3), GridPos(2, 2)), Dir.UP, Color(0xFF1D2951)),
    ArrowShape(2, listOf(GridPos(3, 2), GridPos(4, 2), GridPos(5, 2)), Dir.RIGHT, Color(0xFF1D2951)),
    ArrowShape(3, listOf(GridPos(5, 3), GridPos(5, 4), GridPos(5, 5)), Dir.DOWN, Color(0xFF1D2951)),
    ArrowShape(4, listOf(GridPos(4, 5), GridPos(3, 5)), Dir.LEFT, Color(0xFF1D2951)),
    ArrowShape(5, listOf(GridPos(3, 3), GridPos(3, 4), GridPos(4, 4), GridPos(4, 3)), Dir.UP, Color(0xFF1D2951))
)

val Level3 = listOf(
    ArrowShape(1, listOf(GridPos(3, 2), GridPos(3, 1), GridPos(2, 1)), Dir.LEFT, Color(0xFF1D2951)),
    ArrowShape(2, listOf(GridPos(2, 2), GridPos(2, 3), GridPos(2, 4), GridPos(2, 5)), Dir.DOWN, Color(0xFF1D2951)),
    ArrowShape(3, listOf(GridPos(3, 5), GridPos(4, 5), GridPos(5, 5)), Dir.RIGHT, Color(0xFF1D2951)),
    ArrowShape(4, listOf(GridPos(5, 4), GridPos(5, 3), GridPos(5, 2), GridPos(5, 1)), Dir.UP, Color(0xFF1D2951)),
    ArrowShape(5, listOf(GridPos(4, 2), GridPos(4, 3), GridPos(4, 4), GridPos(3, 4)), Dir.LEFT, Color(0xFF1D2951)),
    ArrowShape(6, listOf(GridPos(3, 3)), Dir.UP, Color(0xFF1D2951))
)

val levels = listOf(Level1, Level2, Level3)

class GameRepository(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("game_prefs", android.content.Context.MODE_PRIVATE)

    var highestUnlockedLevel: Int
        get() = prefs.getInt("highest_unlocked_level", 0)
        set(value) = prefs.edit().putInt("highest_unlocked_level", value).apply()

    var coins: Int
        get() = prefs.getInt("coins", 0)
        set(value) = prefs.edit().putInt("coins", value).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current
    val gameRepo = remember { GameRepository(context) }


    Scaffold(
        bottomBar = {
            if (currentRoute == "home" || currentRoute == "daily" || currentRoute == "profile") {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf("Main" to Icons.Default.Home, "Daily" to Icons.Default.CalendarToday, "Me" to Icons.Default.Person)
                    items.forEachIndexed { index, item ->
                        val route = if (index == 0) "home" else if (index == 1) "daily" else "profile"
                        NavigationBarItem(
                            icon = { Icon(item.second, contentDescription = item.first) },
                            label = { Text(item.first) },
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2A85FF),
                                selectedTextColor = Color(0xFF2A85FF),
                                indicatorColor = Color(0xFFE8F2FF),
                                unselectedIconColor = Color(0xFF8F9BB3),
                                unselectedTextColor = Color(0xFF8F9BB3)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    gameRepo = gameRepo,
                    onPlay = { navController.navigate("game") }
                )
            }
            composable("daily") {
                DailyChallengeScreen(
                    onPlay = { navController.navigate("game") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("game") {
                GameScreen(
                    gameRepo = gameRepo,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(gameRepo: GameRepository, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
            // Background Graphics
            val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
            val offset1 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 60f,
                animationSpec = infiniteRepeatable(
                    animation = tween(25000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset1"
            )
            val offset2 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -80f,
                animationSpec = infiniteRepeatable(
                    animation = tween(35000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset2"
            )
            val offset3 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 50f,
                animationSpec = infiniteRepeatable(
                    animation = tween(30000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset3"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val tint = Color(0xFF2A85FF).copy(alpha = 0.04f)
                val strokeStyle = Stroke(
                    width = size.width * 0.35f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                // Top-left ribbon/arrow shape
                withTransform({
                    translate(left = -size.width * 0.1f, top = size.height * 0.1f + offset1)
                    rotate(-20f)
                }) {
                    val path = Path().apply {
                        moveTo(-size.width * 0.2f, 0f)
                        lineTo(size.width * 0.6f, 0f)
                        lineTo(size.width * 0.9f, size.height * 0.2f)
                    }
                    drawPath(path = path, color = tint, style = strokeStyle)
                }

                // Middle-right shape
                withTransform({
                    translate(left = size.width * 0.2f, top = size.height * 0.5f + offset2)
                    rotate(15f)
                }) {
                    val path = Path().apply {
                        moveTo(size.width * 1.2f, 0f)
                        lineTo(size.width * 0.4f, 0f)
                        lineTo(size.width * 0.1f, size.height * 0.25f)
                    }
                    drawPath(path = path, color = tint, style = strokeStyle)
                }

                // Bottom-left shape
                withTransform({
                    translate(left = -size.width * 0.2f, top = size.height * 0.85f + offset3)
                    rotate(-10f)
                }) {
                    val path = Path().apply {
                        moveTo(-size.width * 0.2f, 0f)
                        lineTo(size.width * 0.5f, 0f)
                        lineTo(size.width * 0.8f, -size.height * 0.15f)
                    }
                    drawPath(path = path, color = tint, style = strokeStyle)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Top Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Daily Challenge Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF5BA4FF), Color(0xFF1E70F6)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF1E70F6))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "DAILY CHALLENGE",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("July 20", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = onPlay,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                                ) {
                                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // Tournament Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFB347), Color(0xFFFF7B00)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, end = 12.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("⏱ 5h 05m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "TOURNAMENT",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("434", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = onPlay,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                                ) {
                                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Game Title
                Text(
                    text = "Arrow Puzzle",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E3349),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                // Play Button
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = tween(150), label = "")

                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(72.dp)
                        .scale(scale)
                        .shadow(16.dp, RoundedCornerShape(36.dp), spotColor = Color(0xFF1E70F6)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(36.dp),
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF5BA4FF), Color(0xFF1E70F6)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("New Game", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Level ${gameRepo.highestUnlockedLevel + 1}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
}

@Composable
fun GameScreen(gameRepo: GameRepository, onBack: () -> Unit) {
    var currentLevelIndex by remember { mutableStateOf(gameRepo.highestUnlockedLevel) }
    var lives by remember { mutableStateOf(3) }
    var coins by remember { mutableStateOf(gameRepo.coins) }
    var shapes by remember { mutableStateOf(listOf<ShapeState>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var isLevelComplete by remember { mutableStateOf(false) }
    
    var moves by remember { mutableStateOf(0) }
    var wrongMoves by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    fun loadLevel(index: Int) {
        val lvl = if (index < levels.size) index else levels.size - 1
        shapes = levels[lvl].map { ShapeState(it) }
        isLevelComplete = false
        isGameOver = false
        lives = 3
        moves = 0
        wrongMoves = 0
    }

    LaunchedEffect(currentLevelIndex) {
        loadLevel(currentLevelIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF2A85FF))
                }
                Text(
                    text = "Level ${currentLevelIndex + 1}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D2951)
                )
                IconButton(onClick = { /* Settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF2A85FF))
                }
            }
            
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Coins", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(coins.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }
                }
                
                // Hearts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Life",
                            tint = if (i < lives) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                            modifier = Modifier.size(28.dp).padding(horizontal = 2.dp)
                        )
                    }
                }
                
                // Difficulty Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        "Hard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            ) {
                val gridCellsX = 8
                
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (isGameOver || isLevelComplete) return@detectTapGestures
                            
                            val cellSize = size.width / gridCellsX
                            val x = (offset.x / cellSize).toInt()
                            val y = (offset.y / cellSize).toInt()
                            
                            val tappedShape = shapes.find { state ->
                                !state.isEscaping && state.shape.path.any { it.x == x && it.y == y }
                            }
                            
                            if (tappedShape != null) {
                                shapes.forEach { it.isHinted = false }
                                moves++
                                val otherCells = shapes
                                    .filter { it != tappedShape && !it.isEscaping }
                                    .flatMap { it.shape.path }
                                    .toSet()
                                    
                                var canEscape = true
                                val dx = tappedShape.shape.dir.dx
                                val dy = tappedShape.shape.dir.dy
                                var currentCells = tappedShape.shape.path.toSet()
                                
                                for (step in 1..gridCellsX * 3) {
                                    currentCells = currentCells.map { GridPos(it.x + dx, it.y + dy) }.toSet()
                                    if (currentCells.intersect(otherCells).isNotEmpty()) {
                                        canEscape = false
                                        break
                                    }
                                }
                                
                                if (canEscape) {
                                    tappedShape.isEscaping = true
                                    coroutineScope.launch {
                                        tappedShape.offset.animateTo(
                                            targetValue = gridCellsX * 3f,
                                            animationSpec = tween(durationMillis = 800)
                                        )
                                        shapes = shapes - tappedShape
                                        if (shapes.isEmpty() && !isLevelComplete) {
                                            isLevelComplete = true
                                            coins += 10
                                            gameRepo.coins = coins
                                            if (currentLevelIndex >= gameRepo.highestUnlockedLevel) {
                                                gameRepo.highestUnlockedLevel = currentLevelIndex + 1
                                            }
                                        }
                                    }
                                } else {
                                    wrongMoves++
                                    lives -= 1
                                    if (lives <= 0) {
                                        isGameOver = true
                                    }
                                }
                            }
                        }
                    }
                ) {
                    val cellSize = size.width / gridCellsX
                    val halfCell = cellSize / 2f
                    val strokeWidth = cellSize * 0.25f

                    for (state in shapes) {
                        val s = state.shape
                        val dx = s.dir.dx * state.offset.value * cellSize
                        val dy = s.dir.dy * state.offset.value * cellSize
                        val shapeColor = if (state.isHinted) Color(0xFFF59E0B) else s.color

                        if (s.path.size > 1) {
                            val path = Path()
                            val startPos = s.path.first()
                            path.moveTo(startPos.x * cellSize + halfCell + dx, startPos.y * cellSize + halfCell + dy)
                            for (i in 1 until s.path.size) {
                                val pos = s.path[i]
                                path.lineTo(pos.x * cellSize + halfCell + dx, pos.y * cellSize + halfCell + dy)
                            }
                            drawPath(
                                path = path,
                                color = shapeColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        } else if (s.path.size == 1) {
                            val pos = s.path.first()
                            drawCircle(
                                color = shapeColor,
                                radius = strokeWidth / 2f,
                                center = Offset(pos.x * cellSize + halfCell + dx, pos.y * cellSize + halfCell + dy)
                            )
                        }

                        val headPos = s.path.last()
                        val hx = headPos.x * cellSize + halfCell + dx
                        val hy = headPos.y * cellSize + halfCell + dy
                        
                        val arrowLength = strokeWidth * 2f
                        val arrowWidth = strokeWidth * 1.8f
                        
                        val tipX = hx + s.dir.dx * arrowLength * 0.7f
                        val tipY = hy + s.dir.dy * arrowLength * 0.7f
                        val backX = hx + s.dir.dx * arrowLength * 0.1f
                        val backY = hy + s.dir.dy * arrowLength * 0.1f
                        
                        val sideDx = if (s.dir.dx == 0) arrowWidth / 2 else 0f
                        val sideDy = if (s.dir.dy == 0) arrowWidth / 2 else 0f
                        
                        val headPath = Path()
                        headPath.moveTo(tipX, tipY)
                        headPath.lineTo(backX + sideDx, backY + sideDy)
                        headPath.lineTo(backX - sideDx, backY - sideDy)
                        headPath.close()
                        
                        drawPath(
                            path = headPath,
                            color = shapeColor
                        )
                    }
                }
            }
            
            // Bottom Hint Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(64.dp).clickable {
                        // Find one movable shape and hint it
                        shapes.forEach { it.isHinted = false }
                        val hintable = shapes.find { state ->
                            !state.isEscaping && run {
                                val otherCells = shapes
                                    .filter { it != state && !it.isEscaping }
                                    .flatMap { it.shape.path }
                                    .toSet()
                                var canEscape = true
                                val dx = state.shape.dir.dx
                                val dy = state.shape.dir.dy
                                var currentCells = state.shape.path.toSet()
                                for (step in 1..8 * 3) {
                                    currentCells = currentCells.map { GridPos(it.x + dx, it.y + dy) }.toSet()
                                    if (currentCells.intersect(otherCells).isNotEmpty()) {
                                        canEscape = false
                                        break
                                    }
                                }
                                canEscape
                            }
                        }
                        hintable?.isHinted = true
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("💡", fontSize = 28.sp)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2A85FF),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 24.dp, y = (-8).dp)
                ) {
                    Text(
                        "Ad",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Dialogs
        if (isLevelComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.width(320.dp).padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("Level Completed!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D2951))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB347), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+10 Coins", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Moves: $moves", fontSize = 16.sp, color = Color(0xFF64748B))
                        val acc = if (moves == 0) 100 else ((moves - wrongMoves) * 100 / moves)
                        Text("Accuracy: $acc%", fontSize = 16.sp, color = Color(0xFF64748B))
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                currentLevelIndex += 1
                                loadLevel(currentLevelIndex)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A85FF)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) {
                                Text("Home", color = Color(0xFF64748B))
                            }
                            TextButton(onClick = { loadLevel(currentLevelIndex) }) {
                                Text("Replay", color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }

        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.width(320.dp).padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("Out of Moves", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { loadLevel(currentLevelIndex) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A85FF)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Retry Level", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { lives = 1; isGameOver = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Watch Ad for +1 Life", color = Color(0xFF2A85FF))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (coins >= 50) {
                                    coins -= 50
                                    gameRepo.coins = coins
                                    lives = 1
                                    isGameOver = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Buy +1 Life (50 Coins)", color = Color(0xFF2A85FF))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = onBack) {
                                Text("Home", color = Color(0xFF64748B))
                            }
                            TextButton(onClick = {
                                lives = 1
                                isGameOver = false
                                shapes.forEach { it.isHinted = false }
                                val hintable = shapes.find { state ->
                                    !state.isEscaping && run {
                                        val otherCells = shapes
                                            .filter { it != state && !it.isEscaping }
                                            .flatMap { it.shape.path }
                                            .toSet()
                                        var canEscape = true
                                        val dx = state.shape.dir.dx
                                        val dy = state.shape.dir.dy
                                        var currentCells = state.shape.path.toSet()
                                        for (step in 1..8 * 3) {
                                            currentCells = currentCells.map { GridPos(it.x + dx, it.y + dy) }.toSet()
                                            if (currentCells.intersect(otherCells).isNotEmpty()) {
                                                canEscape = false
                                                break
                                            }
                                        }
                                        canEscape
                                    }
                                }
                                hintable?.isHinted = true
                            }) {
                                Text("Hint", color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyChallengeScreen(onPlay: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF5BA4FF), Color(0xFF1E70F6))))) {
        // Floating particles and glow
        val infiniteTransition = rememberInfiniteTransition(label = "daily_anim")
        val floatAnim by infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "float"
        )
        val alphaAnim by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
            label = "alpha"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width / 2, size.height * 0.25f),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width / 2, size.height * 0.25f),
                radius = size.width * 0.6f
            )
            // Particles
            val random = java.util.Random(42)
            for (i in 0 until 15) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * (size.height * 0.5f)
                val r = random.nextFloat() * 10f + 5f
                drawCircle(
                    color = Color.White.copy(alpha = alphaAnim * (random.nextFloat() * 0.5f + 0.5f)),
                    radius = r,
                    center = Offset(x, y + floatAnim * random.nextFloat() * 2)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().weight(0.45f)) {
                IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).align(Alignment.CenterStart)) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Daily Challenges",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                )
                
                // Trophy placeholder
                Icon(
                    androidx.compose.material.icons.Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center)
                        .offset(y = floatAnim.dp)
                )
            }

            // Calendar Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("July 2026", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB347), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("19/31", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Calendar Grid
                    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        daysOfWeek.forEach { day ->
                            Text(day, color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Days
                    val days = (1..31).toList()
                    val startOffset = 3 // Wed
                    
                    var dayIndex = 0
                    for (row in 0 until 5) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (col in 0 until 7) {
                                if (row == 0 && col < startOffset) {
                                    Box(modifier = Modifier.size(40.dp))
                                } else if (dayIndex < days.size) {
                                    val day = days[dayIndex]
                                    val isCurrent = day == 20
                                    val isPast = day < 20
                                    
                                    val currentGlow by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = if (isCurrent) 8f else 0f,
                                        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                                        label = "day_glow"
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .then(
                                                if (isCurrent) Modifier.shadow(currentGlow.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF1E70F6))
                                                else Modifier
                                            )
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isCurrent) Color(0xFF2A85FF) else if (isPast) Color(0xFFF1F5F9) else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPast) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                                contentDescription = "Completed",
                                                tint = Color(0xFFFFB347),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = day.toString(),
                                                color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    dayIndex++
                                } else {
                                    Box(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = tween(150), label = "")

                    Button(
                        onClick = onPlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .scale(scale)
                            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF1E70F6)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(32.dp),
                        interactionSource = interactionSource
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF5BA4FF), Color(0xFF1E70F6)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Play", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEF2F6))
    ) {
        // App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Me",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Awards Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileMenuItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE8F2FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp))
                        }
                    },
                    text = "Awards",
                    onClick = { /* Open Awards */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileMenuItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF4285F4), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    },
                    text = "Settings",
                    onClick = { /* Open Settings */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grouped Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF34A853), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        text = "Help",
                        onClick = { /* Open Help */ }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(start = 68.dp))
                    ProfileMenuItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF4285F4), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        text = "About Game",
                        onClick = { /* Open About */ }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(start = 68.dp))
                    ProfileMenuItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF8A2BE2), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        text = "Privacy Rights",
                        onClick = { /* Open Privacy Rights */ }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(start = 68.dp))
                    ProfileMenuItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF00BCD4), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        },
                        text = "Privacy Preferences",
                        onClick = { /* Open Privacy Prefs */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remove Ads Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileMenuItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEA4335), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoneyOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    },
                    text = "Remove Ads",
                    onClick = { /* Open Remove Ads */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 18.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(24.dp)
        )
    }
}