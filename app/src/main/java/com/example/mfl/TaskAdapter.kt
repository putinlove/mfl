import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mfl.R
import com.example.mfl.model.Task

class TaskAdapter(
    private val tasks: MutableList<Task>, // Сделаем список изменяемым
    private val onTaskClick: (Task) -> Unit // Lambda-функция для обработки кликов
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        val taskStatus: TextView = itemView.findViewById(R.id.task_status)
        val taskTime: TextView = itemView.findViewById(R.id.task_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.taskTitle.text = task.title
        holder.taskDescription.text = task.description
        holder.taskStatus.text = if (task.isCompleted) "Выполнена" else "Не выполнена"
        holder.taskTime.text = task.time

        holder.itemView.setOnClickListener {
            onTaskClick(task) // Вызываем функцию при клике
        }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }
}
