package com.example.lendmark.ui.chatbot

import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lendmark.R
import com.example.lendmark.data.model.ChatMessage

class ChatBotAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onRoomClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_USER = 1
    private val TYPE_AI = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]

        if (holder is UserViewHolder) {
            holder.userMsg.text = msg.message
        } else if (holder is AiViewHolder) {
            bindAiMessage(holder, msg.message)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    // -------------------- AI 메시지 처리 --------------------
    private fun bindAiMessage(holder: AiViewHolder, rawText: String) {

        //  파싱: <room id="101">101호 보기</room>
        val pattern = Regex("<room id=\"(.*?)\">(.*?)</room>")
        var spannable = SpannableString(rawText.replace(pattern, "$2"))

        val matches = pattern.findAll(rawText).toList()

        var offset = 0
        matches.forEachIndexed { index, match ->

            val roomId = match.groupValues[1]
            val displayText = match.groupValues[2]

            val startIndex = spannable.indexOf(displayText, offset)
            val endIndex = startIndex + displayText.length
            offset = endIndex

            if (startIndex != -1) {
                val clickable = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onRoomClick(roomId)
                    }
                }

                spannable.setSpan(
                    clickable,
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        holder.aiMsg.text = spannable
        holder.aiMsg.movementMethod = LinkMovementMethod.getInstance()
    }


    // -------------------- ViewHolders --------------------
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userMsg: TextView = itemView.findViewById(R.id.tvUserMessage)
    }

    class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val aiMsg: TextView = itemView.findViewById(R.id.tvAiMessage)
    }
}
