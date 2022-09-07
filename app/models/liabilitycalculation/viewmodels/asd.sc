import org.joda.time.format.DateTimeFormatter

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
//import java.time.DateFormatter

def getInYearCalculationRunDate(calculationTimestamp: String, taxYear: Int): Option[String] = {
  //val dateFormatter = DateFormatter
  val firstQuarterstart = LocalDate.parse(s"${taxYear - 1}-04-05")
  val firstQuarterEnd = LocalDate.parse(s"${taxYear - 1}-07-05")
  val SecondQuarterEnd = LocalDate.parse(s"${taxYear - 1}-10-05")
  val thirdQuarterEnd = LocalDate.parse(s"${taxYear}-01-05")
  val fourthQuarterEnd = LocalDate.parse(s"${taxYear}-04-05")
  //2019-02-15T09:35:15.094Z
   val df = java.time.format.DateTimeFormatter.ofPattern("yyyy-mm")
  val formatter = java.time.Instant.parse(calculationTimestamp)
  val date = LocalDateTime.ofInstant(formatter,ZoneId.of(ZoneOffset.UTC.getId)).toLocalDate
  println(thirdQuarterEnd.getMonthValue+"!!!" +date.getMonthValue)
  println(SecondQuarterEnd.getMonthValue+"111" +date)
  println(firstQuarterEnd.getMonthValue+"22" +date)
  println(fourthQuarterEnd.getMonthValue+"33" +date)
  //println( "11111" + date.isBefore(thirdQuarterEnd))
  //println("2222" + date.isEqual(thirdQuarterEnd))

  date match {
    case date: LocalDate if (date.isBefore(firstQuarterEnd) || date.isEqual(firstQuarterEnd)) && date.isAfter(firstQuarterstart) => None
    case date: LocalDate if (date.isBefore(SecondQuarterEnd) || date.isEqual(SecondQuarterEnd)) && date.isAfter(firstQuarterEnd) => Some("5th July")
    case date: LocalDate if (date.isBefore(thirdQuarterEnd) || date.isEqual(thirdQuarterEnd)) && date.isAfter(SecondQuarterEnd) => Some("5th October")
    case date: LocalDate if (date.isBefore(fourthQuarterEnd) || date.isEqual(fourthQuarterEnd)) && date.isAfter(thirdQuarterEnd) => Some("5th January")
    case date: LocalDate if date.isAfter(fourthQuarterEnd)  => Some("5th April")
    case _ => None
  }
}

val asdf = getInYearCalculationRunDate("2019-02-15T09:35:15.094Z",2019)

println(asdf)


