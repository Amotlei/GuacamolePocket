package com.example.guacamolepocket

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.guacamolepocket.ui.theme.GuacamolePocketTheme

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URISyntaxException

// --- PERSISTENCIA LOCAL (DataStore) ---
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_stats")

class GameRepository(private val context: Context) {
    private val WINS_KEY = intPreferencesKey("total_wins")

    val totalWins: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[WINS_KEY] ?: 0 }

    suspend fun incrementWins() {
        context.dataStore.edit { preferences ->
            val current = preferences[WINS_KEY] ?: 0
            preferences[WINS_KEY] = current + 1
        }
    }
}

// --- MODELOS ---
data class Player(val id: String, val name: String, val score: Int)
data class GameTarget(val id: String, val x: Float, val y: Float)

class MainActivity : ComponentActivity() {

    private lateinit var socket: Socket
    private lateinit var repo: GameRepository

    // Estados
    private var gameTarget by mutableStateOf<GameTarget?>(null)
    private var playersList by mutableStateOf<List<Player>>(emptyList())
    private var round by mutableStateOf(0)
    private var maxRounds by mutableStateOf(10)
    private var winnerMessage by mutableStateOf<String?>(null)
    private var mySocketId by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        repo = GameRepository(this)
        setupSocket()

        setContent {
            GuacamolePocketTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF8B4513),
                                    Color(0xFFD2691E)
                                )
                            )
                        )
                ) {
                    AppNavigation(
                        repo = repo,
                        winnerMessage = winnerMessage,
                        players = playersList,
                        round = round,
                        maxRounds = maxRounds,
                        gameTarget = gameTarget,
                        mySocketId = mySocketId,
                        onJoin = { name ->
                            socket.emit("player:join", name)
                        },
                        onStartGame = { socket.emit("game:start") },
                        onTargetClick = { id ->
                            socket.emit("game:hit", id)
                            gameTarget = null
                        }
                    )
                }
            }
        }
    }

    private fun setupSocket() {
        try {
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            opts.transports = arrayOf("websocket", "polling")

            socket = IO.socket("https://tentacled-unreliably-elina.ngrok-free.dev", opts)

        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }

        socket.on(Socket.EVENT_CONNECT) {
            Log.d("SOCKET", "Conectado")
            mySocketId = socket.id()
        }

        socket.on("game:updatePlayers") { args ->
            val data = args[0] as JSONObject
            val list = mutableListOf<Player>()
            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = data.getJSONObject(key)
                list.add(Player(key, obj.getString("name"), obj.getInt("score")))
            }
            runOnUiThread { playersList = list.sortedByDescending { it.score } }
        }

        socket.on("game:spawn") { args ->
            val data = args[0] as JSONObject
            val tObj = data.getJSONObject("target")
            val newTarget = GameTarget(
                id = tObj.getString("id"),
                x = tObj.getDouble("x").toFloat(),
                y = tObj.getDouble("y").toFloat()
            )
            val r = data.getInt("round")
            val mx = data.getInt("maxRounds")
            runOnUiThread {
                gameTarget = newTarget
                round = r
                maxRounds = mx
                winnerMessage = null
            }
        }

        socket.on("game:end") { args ->
            val data = args[0] as JSONObject
            val wName = data.getString("winnerName")
            val wId = data.optString("winnerId")

            runOnUiThread {
                gameTarget = null
                if (wId == mySocketId) {
                    winnerMessage = "¡GANASTE! 🏆"
                    kotlinx.coroutines.GlobalScope.launch {
                        repo.incrementWins()
                    }
                } else {
                    winnerMessage = "Ganó: $wName 🥇"
                }
            }
        }

        socket.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }
}

// --- NAVEGACIÓN SIMPLE (LOGIN -> JUEGO) ---
@Composable
fun AppNavigation(
    repo: GameRepository,
    winnerMessage: String?,
    players: List<Player>,
    round: Int,
    maxRounds: Int,
    gameTarget: GameTarget?,
    mySocketId: String,
    onJoin: (String) -> Unit,
    onStartGame: () -> Unit,
    onTargetClick: (String) -> Unit
) {
    var hasJoined by remember { mutableStateOf(false) }
    val totalWins by repo.totalWins.collectAsState(initial = 0)

    if (!hasJoined) {
        LoginScreen(
            totalWins = totalWins,
            onJoin = { name ->
                onJoin(name)
                hasJoined = true
            }
        )
    } else {
        GameScreen(
            players = players,
            round = round,
            maxRounds = maxRounds,
            gameTarget = gameTarget,
            winnerMessage = winnerMessage,
            onStartGame = onStartGame,
            onTargetClick = onTargetClick,
            mySocketId = mySocketId
        )
    }
}

// --- PANTALLA 1: LOGIN ---
@Composable
fun LoginScreen(totalWins: Int, onJoin: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título con estilo divertido
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6347)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🥑 GUACAMOLE GAME 🥑",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "¡Golpea los topos!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Yellow
                )
            }
        }

        // Estadísticas en un cartel de madera
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFA0522D)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏆 VICTORIAS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = totalWins.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LISTO PARA JUGAR",
                        color = Color(0xFF90EE90),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Campo de nombre
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5DEB3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🎮 TU NOMBRE DE JUGADOR",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF8B4513),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            "Ej: TopoMaster",
                            color = Color(0xFF8B4513).copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF8B4513),
                        unfocusedTextColor = Color(0xFF8B4513),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { if (name.isNotBlank()) onJoin(name) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4500),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = if (name.isNotBlank()) "🎯 ¡ENTRAR A LA ARENA! 🎯" else "✍️ ESCRIBE TU NOMBRE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Instrucciones
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡El jugador que más topos golpee gana!",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// --- PANTALLA 2: JUEGO ---
@Composable
fun GameScreen(
    players: List<Player>,
    round: Int,
    maxRounds: Int,
    gameTarget: GameTarget?,
    winnerMessage: String?,
    mySocketId: String,
    onStartGame: () -> Unit,
    onTargetClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // --- MARCADOR CON ESTILO DE TABLERO DE MADERA ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B4513)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Encabezado del marcador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6347)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "RONDA $round/$maxRounds",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "🏆 CLASIFICACIÓN",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de jugadores con estilo de arcade
                LazyColumn(
                    modifier = Modifier.heightIn(max = 150.dp)
                ) {
                    items(players) { player ->
                        val isMe = player.id == mySocketId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) Color(0xFFFFD700) else Color(0xFFF5DEB3)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Ícono según posición
                                    val icon = when (players.indexOf(player)) {
                                        0 -> "🥇"
                                        1 -> "🥈"
                                        2 -> "🥉"
                                        else -> "👤"
                                    }
                                    Text(
                                        text = icon,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    Text(
                                        text = if (isMe) "⭐ ${player.name} (TÚ) ⭐" else player.name,
                                        color = if (isMe) Color(0xFF8B0000) else Color(0xFF8B4513),
                                        fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF32CD32)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = "${player.score} pts",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- CAMPO DE JUEGO CON AGUJEROS DE TOPO ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        colors = listOf(
                            Color(0xFF556B2F),
                            Color(0xFF6B8E23)
                        ),
                        radius = 1000f
                    )
                )
                .border(4.dp, Color(0xFF8B4513))
        ) {
            val w = maxWidth
            val h = maxHeight

            // Patrón de agujeros de topo en el fondo
            if (winnerMessage == null && round > 0) {
                // Dibujar agujeros en el fondo
                val holePositions = listOf(
                    0.2f to 0.2f, 0.5f to 0.2f, 0.8f to 0.2f,
                    0.2f to 0.5f, 0.5f to 0.5f, 0.8f to 0.5f,
                    0.2f to 0.8f, 0.5f to 0.8f, 0.8f to 0.8f
                )

                holePositions.forEach { (x, y) ->
                    Box(
                        modifier = Modifier
                            .offset(x = w * x - 40.dp, y = h * y - 40.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF8B4513).copy(alpha = 0.8f),
                                        Color.Black.copy(alpha = 0.9f)
                                    )
                                )
                            )
                            .border(3.dp, Color(0xFFA0522D), CircleShape)
                    )
                }
            }

            if (winnerMessage != null) {
                // Pantalla de fin con confeti virtual
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 ¡FIN DEL JUEGO! 🎉",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.Red,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = winnerMessage,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color(0xFF8B0000),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                Button(
                                    onClick = onStartGame,
                                    modifier = Modifier
                                        .height(60.dp)
                                        .fillMaxWidth(0.8f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF32CD32)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 10.dp,
                                        pressedElevation = 5.dp
                                    )
                                ) {
                                    Text(
                                        "🔄 JUGAR OTRA VEZ",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (round == 0) {
                // Lobby de espera
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B4513)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "👥 ESPERANDO JUGADORES",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Preparados para golpear topos...",
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            Button(
                                onClick = onStartGame,
                                modifier = Modifier
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4500)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "🎮 INICIAR PARTIDA",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Topo que aparece
                gameTarget?.let { target ->
                    val size = 120.dp
                    Box(
                        modifier = Modifier
                            .offset(x = w * target.x - (size / 2), y = h * target.y - (size / 2))
                            .size(size)
                            .clickable { onTargetClick(target.id) }
                    ) {
                        // Cuerpo del topo
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF8B4513),
                                            Color(0xFFA0522D),
                                            Color(0xFFD2691E)
                                        )
                                    )
                                )
                                .border(4.dp, Color(0xFF654321), CircleShape)
                                .shadow(8.dp, shape = CircleShape)
                        )

                        // Ojos del topo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Ojo izquierdo
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .align(Alignment.Center)
                                )
                            }

                            // Ojo derecho
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        // Nariz
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B0000))
                                .align(Alignment.BottomCenter)
                                .offset(y = (-24).dp)
                        )

                        // Orejas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .offset(y = (-8).dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Oreja izquierda
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA0522D))
                                    .rotate(15f)
                            )

                            // Oreja derecha
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA0522D))
                                    .rotate(-15f)
                            )
                        }

                        // Efecto de "¡GOLPÉAME!" animado
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-40).dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "¡GOLPÉAME!",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}