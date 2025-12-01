package com.example.guacamolepocket.repository

import android.util.Log
import com.parse.ParseCloud
import com.parse.ParseException
import com.parse.FunctionCallback
import com.parse.ParseQuery
import com.parse.livequery.ParseLiveQueryClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repositorio encargado de las llamadas a Back4App (Parse) y wrappers suspend para Kotlin Coroutines.
 *
 * NOTAS:
 * - No inicializar liveQueryClient hasta que Parse esté inicializado en Application.onCreate().
 */
class GameRepository {

    // Inicialización perezosa del cliente LiveQuery
    private val liveQueryClient: ParseLiveQueryClient by lazy {
        ParseLiveQueryClient.Factory.getClient()
    }

    /**
     * joinRoom: llama a la Cloud Function 'joinRoom' con { code } y retorna roomId (String).
     * Envuelve la llamada callback-style de Parse en una coroutine suspend.
     */
    suspend fun joinRoom(code: String): String = suspendCancellableCoroutine { cont ->
        try {
            val params = mapOf("code" to code)
            // Usamos FunctionCallback<Any?> explícito para que Kotlin pueda inferir tipos
            ParseCloud.callFunctionInBackground("joinRoom", params, FunctionCallback<Any?> { result, e ->
                if (e != null) {
                    // Parse lanza ParseException en caso de error
                    cont.resumeWithException(e)
                    return@FunctionCallback
                }

                try {
                    when (result) {
                        is Map<*, *> -> {
                            val roomId = result["roomId"] as? String
                            if (roomId != null) cont.resume(roomId)
                            else cont.resumeWithException(RuntimeException("roomId no recibido en joinRoom"))
                        }
                        is String -> cont.resume(result)
                        else -> cont.resumeWithException(RuntimeException("Respuesta inesperada de joinRoom: $result"))
                    }
                } catch (ex: Exception) {
                    cont.resumeWithException(ex)
                }
            })
        } catch (ex: Exception) {
            cont.resumeWithException(ex)
        }
    }

    // Ejemplo de otro wrapper suspend (startGame) siguiendo el mismo patrón:
    suspend fun startGame(roomId: String): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val params = mapOf("roomId" to roomId)
            ParseCloud.callFunctionInBackground("startGame", params, FunctionCallback<Any?> { result, e ->
                if (e != null) {
                    cont.resumeWithException(e)
                    return@FunctionCallback
                }
                // Si no hubo excepción, consideramos éxito (Cloud code retorna "ok")
                cont.resume(true)
            })
        } catch (ex: Exception) {
            cont.resumeWithException(ex)
        }
    }
}
