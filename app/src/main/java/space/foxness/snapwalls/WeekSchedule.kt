package space.foxness.snapwalls

import org.joda.time.DateTime
import org.joda.time.LocalTime

class WeekSchedule(private val schedule: HashMap<DayOfWeek, List<LocalTime>>) {
    
    enum class DayOfWeek { 
        Mon, Tue, Wed, Thu, Fri, Sat, Sun;
        
        val next get() = DayOfWeek.from((ordinal + 1) % values().size)
        
        companion object {
            // -1 because DateTimeConstants has monday at index 1
            fun from(value: Int) = values()[value - 1]
        }
    }
    
    init {
        if (schedule.values.all { it.isEmpty() })
            throw IllegalArgumentException("Schedule can't be empty")

        for (dow in DayOfWeek.values()) // make sure schedule contains all days of week
            if (!schedule.containsKey(dow))
                schedule[dow] = emptyList()
    }
    
    fun getScheduleTimes(startingDate: DateTime, count: Int): List<DateTime> {

        val startTime: LocalTime = startingDate.toLocalTime()
        
        var date = startingDate.toLocalDate()
        var startTimeIndex: Int? = null

        var startDateChanged = false
        while (schedule[DayOfWeek.from(date.dayOfWeek)]!!.isEmpty()) {
            date = date.plusDays(1)
            startDateChanged = true
        }

        if (startDateChanged) {
            startTimeIndex = 0
        } else {
            for ((index, time) in schedule[DayOfWeek.from(date.dayOfWeek)]!!.withIndex()) {
                if (time < startTime) {
                    startTimeIndex = index
                    break
                }
            }
        }

        // startTimeIndex must not be null at this point
        // if it is then there's a logic error and everything before this should be rewritten
        val timeIndex = startTimeIndex!!
        val list = mutableListOf<DateTime>()
        outer@ while (true) {
            val times = schedule[DayOfWeek.from(date.dayOfWeek)]!!
            if (list.isEmpty())
                times.drop(timeIndex)
            
            for (time in times) {
                list.add(date.toDateTime(time))
                
                if (list.size == count)
                    break@outer
            }
            
            date = date.plusDays(1)
        }
        
        return list
    }
    
    companion object {
        fun everyDay(dayTimes: List<LocalTime>) 
                = WeekSchedule(HashMap(DayOfWeek.values().map { it to dayTimes }.toMap()))
        
        fun everyDayBestTimes() 
                = everyDay(listOf(LocalTime(17, 0)))
    }
}