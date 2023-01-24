import scala.util.matching.Regex

val msg = "you’ve claimed £56,000 in Property Income Allowance but this is more than turnover for your UK property"

val amtMsg = "my samplw £34,333 sampoke"
val dateMsg ="the update must align to the accounting period start date of 06/04/2019."

val regex1 ="\\d{2}/\\d{2}/\\d{4}|£\\d+,\\d+".r

val res = regex1.findFirstIn(amtMsg)
val res2 = regex1.findFirstIn(dateMsg)
