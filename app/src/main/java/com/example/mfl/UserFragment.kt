package com.example.mfl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mfl.databinding.FragmentUserBinding
import com.example.mfl.model.User

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserBinding.inflate(inflater, container, false)

        // Пример пользователей
        userList.add(User(id = "1", firstName = "Иван Иванов"))
        userList.add(User(id = "2", firstName = "Мария Петрова"))
        userList.add(User(id = "3", firstName = "Александр Смирнов"))

        // Настройка RecyclerView
        adapter = UserAdapter(userList) { user ->
            openChatWithUser(user)
        }
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewUsers.adapter = adapter

        return binding.root
    }

    private fun openChatWithUser(user: User) {
        val chatFragment = ChatFragment.newInstance("YOUR_USER_ID", user.id)  // Передаем user.id
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
