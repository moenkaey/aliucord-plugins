package com.aliucord.plugins

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import com.aliucord.Utils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentContainerView
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.InputDialog
import com.aliucord.patcher.PinePatchFn
import com.discord.databinding.WidgetChatListActionsBinding
import com.discord.models.domain.emoji.Emoji
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.lytefast.flexinput.R
import top.canyie.pine.Pine.CallFrame
import java.lang.reflect.InvocationTargetException


@AliucordPlugin
class TextReact : Plugin() {
    private fun showDialog(callback: (text: String) -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(Utils.appActivity.applicationContext)
        builder.setTitle("Title")

        // Set up the input
        val input = EditText(Utils.appActivity.applicationContext)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.hint = "Enter Text"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            // Here you get get input text from the Edittext
            val text = input.text.toString()
            dialog.dismiss()
            callback(text)
        })
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

        builder.show()
    }
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
                    val linearLayout = (callFrame.args[0] as NestedScrollView).getChildAt(0) as FragmentContainerView
                    val binding = getBinding.invoke(callFrame.thisObject) as WidgetChatListActionsBinding
                    val quickStar = binding.a.findViewById<TextView>(quickStarId).apply {
                        visibility = if ((callFrame.args[0] as WidgetChatListActions.Model).manageMessageContext.canAddReactions) View.VISIBLE else View.GONE
                    }

                    if (!quickStar.hasOnClickListeners()) quickStar.setOnClickListener {
                        try {
                            (callFrame.thisObject as WidgetChatListActions).dismiss()
                            val inDialog = InputDialog()
                            inDialog.setTitle("Text react!")
                            inDialog.setDescription("Enter some text to send as reactions.")
                            inDialog.setPlaceholderText("Enter text...")
                            inDialog.setOnOkListener {
                                Utils.showToast(context, inDialog.input.toString())
                            }
                            linearLayout.addView(inDialog.view)

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
