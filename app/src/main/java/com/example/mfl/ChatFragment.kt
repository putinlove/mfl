package com.example.mfl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mfl.databinding.FragmentChatBinding
import com.example.mfl.model.Message

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var currentUserId: String? = null
    private var chatUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUserId = it.getString(ARG_CURRENT_USER_ID)
            chatUserId = it.getString(ARG_CHAT_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)

        // Пример сообщений между текущим пользователем и другим пользователем
        messages.add(Message(senderId = "1", receiverId = currentUserId!!, content = "Привет! Как дела?"))
        messages.add(Message(senderId = currentUserId!!, receiverId = chatUserId!!, content = "Все хорошо, а у тебя?"))
        messages.add(Message(senderId = "1", receiverId = currentUserId!!, content = "У меня тоже все нормально."))

        adapter = MessageAdapter(messages, currentUserId!!)
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewMessages.adapter = adapter

        // Логика для отправки сообщения
        binding.buttonSend.setOnClickListener {
            val messageContent = binding.editTextMessage.text.toString()
            if (messageContent.isNotEmpty()) {
                val newMessage = Message(senderId = currentUserId!!, receiverId = chatUserId!!, content = messageContent)
                messages.add(newMessage)
                adapter.notifyItemInserted(messages.size - 1)
                binding.editTextMessage.text.clear()
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CURRENT_USER_ID = "current_user_id"
        private const val ARG_CHAT_USER_ID = "chat_user_id"

        fun newInstance(currentUserId: String, chatUserId: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle().apply {
                putString(ARG_CURRENT_USER_ID, currentUserId)
                putString(ARG_CHAT_USER_ID, chatUserId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
