package com.sabrina.fibis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()
    private var lastPosition = -1

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun clearMessages() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
                UserMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
                BotMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is BotMessageViewHolder -> holder.bind(message)
        }

        // Анимация для новых сообщений
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(
                holder.itemView.context,
                if (message.isUser) R.anim.slide_in_right else R.anim.slide_in_left
            )
            holder.itemView.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount() = messages.size

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContainer: View = itemView.findViewById(R.id.user_message_container)
        private val messageText: TextView = itemView.findViewById(R.id.user_message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.user_message_time)

        fun bind(message: ChatMessage) {
            messageContainer.visibility = View.VISIBLE
            messageText.text = message.text
            messageTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp)
        }
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContainer: View = itemView.findViewById(R.id.bot_message_container)
        private val messageText: TextView = itemView.findViewById(R.id.bot_message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.bot_message_time)

        fun bind(message: ChatMessage) {
            messageContainer.visibility = View.VISIBLE
            messageText.text = message.text
            messageTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp)
        }
    }
}