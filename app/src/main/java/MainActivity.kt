package com.example.lab_3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    // --- МОДЕЛІ ДАНИХ ---
    data class Course(val id: Int, val title: String, val desc: String, var status: String = "Не придбано", var rating: Double = 0.0, val reviews: MutableList<String> = mutableListOf())
    data class Program(val id: Int, val title: String, val courseIds: List<Int>)
    data class DisplayItem(val title: String, val subtitle: String, val info: String, val onClick: () -> Unit)

    // --- МОК-БАЗА ДАНИХ ---
    private val courses = listOf(
        Course(1, "Основи Node.js", "Вивчення базового бекенду на JS"),
        Course(2, "React для початківців", "Створення інтерактивних SPA"),
        Course(3, "Python Backend", "Розробка мікросервісів на FastAPI та SQLAlchemy"),
        Course(4, "DevOps бази", "Основи Linux, Docker та деплоймент")
    )
    private val programs = listOf(
        Program(1, "Fullstack Розробник", listOf(1, 2)),
        Program(2, "Backend Майстер", listOf(1, 3, 4))
    )
    private var currentUser: String? = null

    // --- UI ЕЛЕМЕНТИ ---
    private lateinit var rv: RecyclerView
    private lateinit var tvUser: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv = findViewById(R.id.recyclerView)
        tvUser = findViewById(R.id.tvUserInfo)
        rv.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnCatalog).setOnClickListener { loadCatalog() }
        findViewById<Button>(R.id.btnMyCourses).setOnClickListener { loadMyCourses() }
        findViewById<Button>(R.id.btnPrograms).setOnClickListener { loadPrograms() }

        showAuthDialog()
    }

    // --- РІВЕНЬ 3: АВТОРИЗАЦІЯ ---
    private fun showAuthDialog() {
        val input = EditText(this).apply { hint = "Введіть логін" }
        AlertDialog.Builder(this)
            .setTitle("Авторизація")
            .setMessage("Будь ласка, увійдіть у систему")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Увійти") { _, _ ->
                currentUser = input.text.toString().ifBlank { "Студент" }
                tvUser.text = "Користувач: $currentUser"
                loadCatalog()
            }.show()
    }

    // --- РІВЕНЬ 1: КАТАЛОГ ТА МОЇ КУРСИ ---
    private fun loadCatalog() {
        val items = courses.map { course ->
            DisplayItem(course.title, "Статус: ${course.status}", "Рейтинг: ${course.rating}/5.0 (Відгуків: ${course.reviews.size})") {
                showCourseActionDialog(course)
            }
        }
        rv.adapter = GenericAdapter(items)
    }

    private fun loadMyCourses() {
        val items = courses.filter { it.status != "Не придбано" }.map { course ->
            DisplayItem(course.title, "Статус: ${course.status}", "Ваша статистика зберігається") {
                showCourseActionDialog(course)
            }
        }
        rv.adapter = GenericAdapter(items)
        if (items.isEmpty()) Toast.makeText(this, "У вас ще немає курсів", Toast.LENGTH_SHORT).show()
    }

    // --- РІВЕНЬ 3: НАВЧАЛЬНІ ПРОГРАМИ ---
    private fun loadPrograms() {
        val items = programs.map { prog ->
            val includedCourses = courses.filter { it.id in prog.courseIds }
            val completed = includedCourses.count { it.status == "Завершено" }
            DisplayItem(prog.title, "Курсів у програмі: ${prog.courseIds.size}", "Пройдено: $completed / ${prog.courseIds.size}") {
                val names = includedCourses.joinToString("\n") { "- ${it.title} (${it.status})" }
                AlertDialog.Builder(this).setTitle(prog.title).setMessage(names).setPositiveButton("ОК", null).show()
            }
        }
        rv.adapter = GenericAdapter(items)
    }

    // --- РІВЕНЬ 2: ЗАПИС ТА ВІДГУКИ ---
    private fun showCourseActionDialog(course: Course) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(course.title)

        val message = StringBuilder("${course.desc}\n\nОстанні відгуки:\n")
        if (course.reviews.isEmpty()) message.append("Немає відгуків")
        else course.reviews.takeLast(3).forEach { message.append("• $it\n") }
        builder.setMessage(message.toString())

        if (course.status == "Не придбано") {
            builder.setPositiveButton("Записатись") { _, _ ->
                course.status = "В процесі"
                Toast.makeText(this, "Ви записані на курс!", Toast.LENGTH_SHORT).show()
                loadCatalog()
            }
        } else {
            if (course.status == "В процесі") {
                builder.setNeutralButton("Завершити курс") { _, _ ->
                    course.status = "Завершено"
                    Toast.makeText(this, "Курс пройдено!", Toast.LENGTH_SHORT).show()
                    loadMyCourses()
                }
            }
            builder.setPositiveButton("Залишити відгук") { _, _ -> showReviewDialog(course) }
        }
        builder.setNegativeButton("Закрити", null)
        builder.show()
    }

    private fun showReviewDialog(course: Course) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val ratingInput = EditText(this).apply { hint = "Оцінка (1-5)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val reviewInput = EditText(this).apply { hint = "Ваш відгук" }
        layout.addView(ratingInput)
        layout.addView(reviewInput)

        AlertDialog.Builder(this)
            .setTitle("Оцінка курсу")
            .setView(layout)
            .setPositiveButton("Зберегти") { _, _ ->
                val rating = ratingInput.text.toString().toDoubleOrNull() ?: 5.0
                course.rating = ((course.rating * course.reviews.size) + rating) / (course.reviews.size + 1)
                val cleanRating = String.format("%.1f", course.rating).replace(",", ".").toDouble()
                course.rating = cleanRating
                course.reviews.add(reviewInput.text.toString().ifBlank { "Хороший курс" })
                loadCatalog()
            }.show()
    }

    // --- УНІВЕРСАЛЬНИЙ АДАПТЕР ---
    class GenericAdapter(private val items: List<DisplayItem>) : RecyclerView.Adapter<GenericAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
            val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvSubtitle.text = item.subtitle
            holder.tvInfo.text = item.info
            holder.itemView.setOnClickListener { item.onClick() }
        }
        override fun getItemCount() = items.size
    }
}