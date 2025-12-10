package com.example.guacamolepocket.repository

//import com.parse.ParseCloud
//import com.parse.FunctionCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repositorio encargado de las llamadas a Back4App (Parse) y wrappers suspend para Kotlin Coroutines.
 *
 * NOTAS:
 * - No inicializar liveQueryClient hasta que Parse esté inicializado en Application.onCreate().
 */

data class SpawnData(
    val spawnId: String?,
    val cx: Float?,
    val cy: Float?,
    val r: Float?,
    val ttlMs: Long?
)

data class RoomState(
    val score: Map<String, Int>,
    val round: Int,
    val maxRounds: Int,
    val lastSpawn: SpawnData?
)

class GameRepository {

    suspend fun joinRoom(code: String): String =
        suspendCancellableCoroutine { cont ->
            val params = mapOf("code" to code)
//            ParseCloud.callFunctionInBackground(
//                "joinRoom",
//                params,
//                FunctionCallback<Any?> { result, e ->
//                    if (e != null) {
//                        cont.resumeWithException(e)
//                        return@FunctionCallback
//                    }
//
//                    try {
//                        val map = result as Map<*, *>
//                        val roomId = map["roomId"] as? String
//                        if (roomId != null) cont.resume(roomId)
//                        else cont.resumeWithException(
//                            RuntimeException("roomId no recibido")
//                        )
//                    } catch (ex: Exception) {
//                        cont.resumeWithException(ex)
//                    }
//                }
//            )
        }

    suspend fun startGame(roomId: String): Boolean =
        suspendCancellableCoroutine { cont ->
//            ParseCloud.callFunctionInBackground(
//                "startGame",
//                mapOf("roomId" to roomId),
//                FunctionCallback<Any?> { _, e ->
//                    if (e != null) cont.resumeWithException(e)
//                    else cont.resume(true)
//                }
//            )
        }

    suspend fun hitTarget(roomId: String, spawnId: String, player: String): Boolean =
        suspendCancellableCoroutine { cont ->
            val params = mapOf(
                "roomId" to roomId,
                "spawnId" to spawnId,
                "player" to player           // <-- NECESARIO O EL BACKEND FALLA
            )

//            ParseCloud.callFunctionInBackground(
//                "hitTarget",
//                params,
//                FunctionCallback<Any?> { _, e ->
//                    if (e != null) cont.resumeWithException(e)
//                    else cont.resume(true)
//                }
//            )
        }

    suspend fun getRoomState(roomId: String): RoomState =
        suspendCancellableCoroutine { cont ->
//            ParseCloud.callFunctionInBackground(
//                "getRoomState",
//                mapOf("roomId" to roomId),
//                FunctionCallback<Any?> { result, e ->
//                    if (e != null) {
//                        cont.resumeWithException(e)
//                        return@FunctionCallback
//                    }
//
//                    try {
//                        val map = result as Map<*, *>
//
//                        val score = map["score"] as? Map<String, Int> ?: emptyMap()
//                        val round = (map["round"] as? Number)?.toInt() ?: 0
//                        val maxRounds = (map["maxRounds"] as? Number)?.toInt() ?: 5
//
//                        val spawn = map["lastSpawn"] as? Map<*, *>
//
//                        val lastSpawn = if (spawn != null) {
//                            SpawnData(
//                                spawnId = spawn["spawnId"] as? String,
//                                cx = (spawn["cx"] as? Number)?.toFloat(),
//                                cy = (spawn["cy"] as? Number)?.toFloat(),
//                                r = (spawn["r"] as? Number)?.toFloat(),
//                                ttlMs = (spawn["ttlMs"] as? Number)?.toLong()
//                            )
//                        } else null
//
//                        cont.resume(RoomState(score, round, maxRounds, lastSpawn))
//
//                    } catch (ex: Exception) {
//                        cont.resumeWithException(ex)
//                    }
//                }
//            )
        }
}
