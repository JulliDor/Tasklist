package tasklist
import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class Task(var date:String, var time:String, var priority:String, var teg:String, var task:MutableList<String>)

fun addTask(tasks:MutableList<Task>){
    val priority = setPriority()
    val date = setDate()
    val time = setTime()
    val teg = defineDueTeg(date)
    val task = addText()
    if (task.isNotEmpty()) tasks.add(Task(date, time, priority, teg, task)) else println("The task is blank")
}

fun addText():MutableList<String> {
    val taskList = mutableListOf<String>()
    val taskListResult =  mutableListOf<String>()
    println("Input a new task (enter a blank line to end):")
    while(true) {
        val str = readln().trim()
        if (str.isNotBlank()) taskList.add(str)
        else break
    }
    taskList.forEach { it1 -> if (it1.length > 44) it1.chunked(44).map { taskListResult.add(it.padEnd(44,' ')) }  else taskListResult.add(it1.padEnd(44, ' ')) }
    return taskListResult
}

fun setDate():String {
    while(true) {
        println("Input the date (yyyy-mm-dd):")
        try {
            val (y, m, d) = readln().split("-").map { it.toInt() }
            return LocalDate(y, m, d).toString()
        } catch (e: Exception) {
            println("The input date is invalid")
        }
    }
}

fun setTime():String {
    while(true){
        println("Input the time (hh:mm):")
        try {
            val (h,m) = readln().split(":").map { it.toInt() }
            LocalDateTime(2000, 12, 12, h, m)
            return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}"
        } catch(e:Exception) { println("The input time is invalid") }
    }
}

fun setPriority():String {
    while (true) {
        println("Input the task priority (C, H, N, L):").run { readln().uppercase() }.let { if (it in "CHNL") return it }
    }
}

fun defineDueTeg(date: String): String {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
    return when {
        currentDate.daysUntil(date.toLocalDate()) == 0 -> "T"
        currentDate.daysUntil(date.toLocalDate()) > 0 -> "I"
        else -> "O"
    }
}

fun checkNumberOfTasks(tasks: MutableList<Task>):Int {
    while (true) {
        println("Input the task number (1-${tasks.lastIndex + 1}):").run { readln() }.apply {
            if (this.matches(Regex("""\d+""")) && this.toInt() in 1..tasks.lastIndex + 1) return this.toInt()
            else println("Invalid task number") }
    }
}

fun editTask(tasks: MutableList<Task>){
    val n = checkNumberOfTasks(tasks)
    var field:String
    while (true) {
        field = println("Input a field to edit (priority, date, time, task):").run { readln().lowercase() }
        if (!field.matches(Regex("""priority|date|time|task"""))) println("Invalid field") else break
    }
    when(field){
        "priority" -> tasks[n-1].priority = setPriority()
        "date" -> { tasks[n-1].date = setDate(); tasks[n-1].teg = defineDueTeg(tasks[n-1].date) }
        "time" -> tasks[n-1].time = setTime()
        "task" -> tasks[n-1].task = addText()
    }
    println("The task is changed")
}

fun printTasks(tasks: MutableList<Task>) {
    if (tasks.isEmpty()) println("No tasks have been input")
    else {
        println("+----+------------+-------+---+---+--------------------------------------------+\n" +
                "| N  |    Date    | Time  | P | D |                   Task                     |\n" +
                "+----+------------+-------+---+---+--------------------------------------------+")
        for (t in tasks.indices) {
            val (date, time, priority, teg, task) = tasks[t]
            println(if (t + 1 < 9) "| ${t + 1}  |" else { "| ${t + 1} |" } + " $date | $time | ${setColorPriorityAndTag(priority)} | ${setColorPriorityAndTag(teg)} ${task.joinToString("|\n|    |            |       |   |   |", "|", "|")}")
            println("+----+------------+-------+---+---+--------------------------------------------+")
        }
    }
}

fun setColorPriorityAndTag(label:String):String {
    return when (label) {
        "C", "O" -> "\u001B[101m \u001B[0m"
        "H", "T" -> "\u001B[103m \u001B[0m"
        "N", "I" -> "\u001B[102m \u001B[0m"
        else -> "\u001B[104m \u001B[0m"
    }
}

fun main() {
    val tasks = mutableListOf<Task>()
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    val taskAdapter = moshi.adapter<MutableList<Task>>(type)
    val jsonFile = File("tasklist.json")
    if (jsonFile.exists()) {
        val lastTask = taskAdapter.fromJson(jsonFile.readText())
        lastTask!!.forEach { tasks.add(it) }
    }
    while (true) {
        when(println("Input an action (add, print, edit, delete, end):").run {readln().lowercase()}){
            "end" -> {
                println("Tasklist exiting!")
                jsonFile.writeText(taskAdapter.toJson(tasks))
                break
            }
            "add" -> addTask(tasks)
            "print" -> printTasks(tasks)
            "edit" -> {
                printTasks(tasks)
                if(tasks.isNotEmpty()) editTask(tasks)
            }
            "delete" -> {
                printTasks(tasks)
                if(tasks.isEmpty()) continue
                val n = checkNumberOfTasks(tasks)
                tasks.removeAt(n-1)
                println("The task is deleted")
            }
            else -> println("The input action is invalid")
        }
    }
}