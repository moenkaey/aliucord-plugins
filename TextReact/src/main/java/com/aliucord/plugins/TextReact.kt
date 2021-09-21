package com.aliucord.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.aliucord.Utils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.InputDialog
import com.aliucord.fragments.AppFragmentProxy
import com.aliucord.patcher.PinePatchFn
import com.discord.databinding.WidgetChatListActionsBinding
import com.discord.models.domain.emoji.Emoji
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.lytefast.flexinput.R
import top.canyie.pine.Pine.CallFrame
import java.lang.reflect.InvocationTargetException


@AliucordPlugin
class TextReact : Plugin() {
    @SuppressLint("SetTextI18n")
    override fun start(context: Context) {
        Toast.makeText(context, "opened the context menu", Toast.LENGTH_SHORT).show()
        val icon = ContextCompat.getDrawable(context, R.d.ic_keyboard_black_24dp)
        val quickStarId = View.generateViewId()

        with(WidgetChatListActions::class.java, {
            val getBinding = getDeclaredMethod("getBinding").apply { isAccessible = true }
            val addReaction = getDeclaredMethod("addReaction", Emoji::class.java).apply { isAccessible = true }

            patcher.patch(getDeclaredMethod("configureUI", WidgetChatListActions.Model::class.java), PinePatchFn { callFrame: CallFrame ->
                try {
                    Utils.showToast(context.applicationContext, "added dialog")
                    val message = (callFrame.args[0] as WidgetChatListActions.Model).message

                    val binding = getBinding.invoke(callFrame.thisObject) as WidgetChatListActionsBinding
                    val quickStar = binding.a.findViewById<TextView>(quickStarId).apply {
                        visibility = if ((callFrame.args[0] as WidgetChatListActions.Model).manageMessageContext.canAddReactions) View.VISIBLE else View.GONE
                    }

                    if (!quickStar.hasOnClickListeners()) quickStar.setOnClickListener {
                        try {
                            val inDialog = InputDialog()
                                .setTitle("Text react!")
                                .setDescription("Enter some text to send as reactions.")
                                .setPlaceholderText("Enter text...")
                            inDialog.setOnOkListener {
                                Utils.showToast(context, inDialog.input.toString())
                                (callFrame.thisObject as WidgetChatListActions).dismiss()
                            }
                            inDialog.show(AppFragmentProxy().getmFragment().parentFragmentManager, "")

                            // addReaction.invoke(callFrame.thisObject, StoreStream.getEmojis().unicodeEmojisNamesMap["star"])
                        } catch (e: IllegalAccessException) {
                            e.printStackTrace()
                        } catch (e: InvocationTargetException) {
                            e.printStackTrace()
                        }
                    }
                } catch (ignored: Throwable) {
                }
            })

            patcher.patch(getDeclaredMethod("onViewCreated", View::class.java, Bundle::class.java), PinePatchFn { callFrame: CallFrame ->
                val linearLayout = (callFrame.args[0] as NestedScrollView).getChildAt(0) as LinearLayout
                val ctx = linearLayout.context

                icon?.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))

                val quickStar = TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Icon).apply {
                    text = "Text react"
                    id = quickStarId
                    setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
                }

                linearLayout.addView(quickStar, 1)
            })
        })
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}
