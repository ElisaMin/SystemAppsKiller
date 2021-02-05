package me.heizi.box.package_manager.custom_view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import me.heizi.box.package_manager.R


/**
 * 下次一定
 */
class IconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr:Int = 0,
    @StyleRes defStyleRes:Int = 0
) : FrameLayout(context,attrs,defStyleAttr,defStyleRes) {





    var icon = ImageView(context)

    init {
        ViewCompat.setBackground(this,ContextCompat.getDrawable(context,R.drawable.circle))

        context.theme.obtainStyledAttributes(attrs,R.styleable.IconButton,0,0).run {
            runCatching { getDrawable(R.styleable.IconButton_android_icon) }
                .getOrDefault(ContextCompat.getDrawable(context, R.drawable.ic_outline_help_outline_24))
                .let(icon::setImageDrawable)
            kotlin.runCatching { getColor(R.styleable.IconButton_tint,Color.WHITE) }
                .getOrDefault(Color.WHITE)
                .let(icon::setColorFilter)
            recycle()
        }
        addViewInLayout(
            icon,0,
            LayoutParams(LayoutParams.WRAP_CONTENT, WRAP_CONTENT).also {
                it.gravity = Gravity.CENTER
            }
        )

        isClickable = true
        isFocusable = true

    }


}
