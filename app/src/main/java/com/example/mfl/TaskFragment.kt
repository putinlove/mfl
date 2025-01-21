import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mfl.R
import com.example.mfl.databinding.FragmentTaskBinding
import com.example.mfl.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.Calendar
import java.util.UUID

class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskAdapter: TaskAdapter
    private val tasks: MutableList<Task> = mutableListOf()
    private val db = FirebaseFirestore.getInstance()
    private var isParent: Boolean = false
    private var userRole: String = "parent"  // Временно задаем роль как "parent"
    private val auth = FirebaseAuth.getInstance() // Инициализация FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserRoleFromFirebase()

        taskAdapter = TaskAdapter(tasks) { task ->
            if (isParent) {
                showAddTaskDialog(task)
            } else {
                Toast.makeText(requireContext(), "У вас нет прав для редактирования", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = taskAdapter

        binding.buttonAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun loadUserRoleFromFirebase() {
        // Временно устанавливаем роль как "parent" для тестирования
        userRole = "parent"  // Hardcode роль "parent"
        isParent = (userRole == "parent")

        // Теперь кнопка будет отображаться, если роль пользователя родитель
        binding.buttonAddTask.visibility = if (isParent) View.VISIBLE else View.GONE

        loadTasksFromFirebase()
    }

    private fun loadTasksFromFirebase() {
        db.collection("tasks").get().addOnSuccessListener { result ->
            tasks.clear()
            for (document in result) {
                val task = document.toObject(Task::class.java)
                tasks.add(task)
            }
            taskAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Ошибка загрузки задач", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddTaskDialog(task: Task? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val titleInput: EditText = dialogView.findViewById(R.id.editTextTaskTitle)
        val descriptionInput: EditText = dialogView.findViewById(R.id.editTextTaskDescription)
        val dueTimeInput: EditText = dialogView.findViewById(R.id.editTextTaskDueTime)

        if (task != null) {
            titleInput.setText(task.title)
            descriptionInput.setText(task.description)
            dueTimeInput.setText(task.dueTime)
        }

        dueTimeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(requireContext(), { _, hour, minute ->
                dueTimeInput.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePicker.show()
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle(if (task == null) "Добавить задачу" else "Редактировать задачу")
            .setView(dialogView)
            .setPositiveButton(if (task == null) "Добавить" else "Сохранить") { _, _ ->
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val dueTime = dueTimeInput.text.toString()

                if (title.isNotEmpty() && description.isNotEmpty() && dueTime.isNotEmpty()) {
                    if (task == null) {
                        val newTask = Task(UUID.randomUUID().toString(), title, description, dueTime)
                        saveTaskToFirebase(newTask)
                    } else {
                        task.title = title
                        task.description = description
                        task.dueTime = dueTime
                        updateTaskInFirebase(task)
                    }
                } else {
                    Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)

        // Добавляем кнопку удаления задачи, если задача существует (редактирование)
        if (task != null) {
            dialogBuilder.setNeutralButton("Удалить") { _, _ ->
                deleteTaskFromFirebase(task)
            }
        }

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
                taskAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Задача обновлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка обновления задачи", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteTaskFromFirebase(task: Task) {
        db.collection("tasks").document(task.id).delete()
            .addOnSuccessListener {
                tasks.remove(task)
                taskAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка удаления задачи", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

