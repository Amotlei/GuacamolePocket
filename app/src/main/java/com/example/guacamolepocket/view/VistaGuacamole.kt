package com.example.guacamolepocket.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

/**
 * VistaGuacamole: dibuja un objetivo (círculo café) y detecta toques dentro del radio.
 * La UI que la use debe llamar setOnHitListener(...) para recibir hits (spawnId).
 */
class VistaGuacamole @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // color café por defecto
        color = 0xFF8B5A2B.toInt() // IMPORTANTE: color en ARGB
    }

    private var cx: Float = -1f
    private var cy: Float = -1f
    private var r: Float = 0f
    private var spawnId: String = ""

    private var visible: Boolean = false

    // Listener que notifica cuando el jugador toca el objetivo
    private var onHitListener: ((spawnId: String) -> Unit)? = null

    fun setOnHitListener(listener: (String) -> Unit) {
        onHitListener = listener
    }

    /**
     * Actualiza el spawn a dibujar. Si spawnId == "" entonces se oculta.
     */
    fun showSpawn(x: Float, y: Float, radio: Float, spawnId: String) {
        cx = x
        cy = y
        r = radio
        this.spawnId = spawnId
        visible = true
        invalidate()
    }

    fun hideSpawn() {
        visible = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!visible) return
        canvas.drawCircle(cx, cy, r, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!visible) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            val dx = event.x - cx
            val dy = event.y - cy
            val distSq = dx * dx + dy * dy
            if (distSq <= r * r) {
                // tocó el objetivo
                // opcional: animación simple (cambia color brevemente)
                paint.color = 0xFFFFA500.toInt() // naranja breve
                invalidate()
                onHitListener?.invoke(spawnId)
                // ocultar objetivo localmente; servidor decidirá el siguiente spawn
                visible = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
