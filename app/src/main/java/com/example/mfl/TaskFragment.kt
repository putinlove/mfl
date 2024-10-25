import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mfl.R
import com.example.mfl.databinding.FragmentTaskBinding
import com.example.mfl.model.Task
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private val tasks: MutableList<Task> = mutableListOf() // Список задач
    private val db = FirebaseFirestore.getInstance() // Инициализация Firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка RecyclerView
        taskAdapter = TaskAdapter(tasks) { task -> showAddTaskDialog(task) }
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = taskAdapter

        // Загрузка задач из Firebase
        loadTasksFromFirebase()

        // Кнопка добавления задачи
        binding.buttonAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun loadTasksFromFirebase() {
        db.collection("tasks").get().addOnSuccessListener { result ->
            tasks.clear()
            for (document in result) {
                val task = document.toObject(Task::class.java)
                tasks.add(task)
            }
            taskAdapter.notifyDataSetChanged() // Обновляем список задач
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Ошибка загрузки задач", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddTaskDialog(task: Task? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val titleInput: EditText = dialogView.findViewById(R.id.editTextTaskTitle)
        val descriptionInput: EditText = dialogView.findViewById(R.id.editTextTaskDescription)
        val statusCheckbox: CheckBox = dialogView.findViewById(R.id.checkBoxTaskStatus)

        // Если редактируем задачу, заполняем поля
        if (task != null) {
            titleInput.setText(task.title)
            descriptionInput.setText(task.description)
            statusCheckbox.isChecked = task.isCompleted
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle(if (task == null) "Добавить задачу" else "Редактировать задачу")
            .setView(dialogView)
            .setPositiveButton(if (task == null) "Добавить" else "Сохранить") { _, _ ->
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val isCompleted = statusCheckbox.isChecked

                if (title.isNotEmpty() && description.isNotEmpty()) {
                    if (task == null) {
                        // Добавляем новую задачу
                        val newTask = Task(UUID.randomUUID().toString(), title, description, isCompleted, "00:00")
                        saveTaskToFirebase(newTask)
                    } else {
                        // Обновляем существующую задачу
                        task.title = title
                        task.description = description
                        task.isCompleted = isCompleted
                        updateTaskInFirebase(task)
                    }
                } else {
                    Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)

        dialogBuilder.create().show()
    }

    private fun saveTaskToFirebase(task: Task) {
        db.collection("tasks").document(task.id).set(task)
            .addOnSuccessListener {
                tasks.add(task)
                taskAdapter.notifyItemInserted(tasks.size - 1)
                Toast.makeText(requireContext(), "Задача добавлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка добавления задачи", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTaskInFirebase(task: Task) {
        db.collection("tasks").document(task.id).set(task)
            .addOnSuccessListener {
                taskAdapter.notifyDataSetChanged() // Обновляем весь список
                Toast.makeText(requireContext(), "Задача обновлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка обновления задачи", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Освобождение ресурсов binding
    }
}
