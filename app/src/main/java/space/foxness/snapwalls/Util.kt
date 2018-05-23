package space.foxness.snapwalls

import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder

object Util {
    private val periodformatter = PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(2) // gives the '01'
            .appendHours()
            .appendSeparator(":")
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter()

    fun Duration.toNice(): String = periodformatter.print(toPeriod())
}