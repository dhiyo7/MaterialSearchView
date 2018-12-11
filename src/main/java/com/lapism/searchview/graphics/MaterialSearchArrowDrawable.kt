package com.lapism.searchview.graphics

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ColorFilter
import android.util.Property
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.content.ContextCompat
import com.lapism.searchview.MaterialUtils
import com.lapism.searchview.R


class MaterialSearchArrowDrawable(context: Context) : DrawerArrowDrawable(context) {

    var position: Float
        get() = progress
        set(position) {
            if (position == STATE_ARROW) {
                setVerticalMirror(true)
            } else if (position == STATE_HAMBURGER) {
                setVerticalMirror(false)
            }
            progress = position
        }

    fun animate(state: Float, duration: Long) {
        val anim: ObjectAnimator = if (state == STATE_ARROW) {
            ObjectAnimator.ofFloat(this, PROGRESS, STATE_HAMBURGER, state)
        } else {
            ObjectAnimator.ofFloat(this, PROGRESS, STATE_ARROW, state)
        }
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.duration = duration
        anim.start()
    }

    companion object {

        const val STATE_HAMBURGER = 0.0f
        const val STATE_ARROW = 1.0f

        private val PROGRESS = object : Property<MaterialSearchArrowDrawable, Float>(Float::class.java, "progress") {
            override fun set(obj: MaterialSearchArrowDrawable, value: Float?) {
                obj.progress = value!!
            }

            override fun get(property: MaterialSearchArrowDrawable): Float {
                return property.progress
            }
        }
    }

}
