/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms.validation

import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, ResolverStyle}
import scala.util.Try

trait Constraints {

  val sixthAprilTwentyFifteen: LocalDate = LocalDate.of(2015, 4, 6)

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def optMaxLength(max: Int, err: Invalid): Constraint[Option[String]] = Constraint {
    case Some(x) if x.length > max => err
    case _ => Valid
  }

  private def tupleToDate(dateTuple: (String, String, String)) = {
    LocalDate.parse(s"${dateTuple._1}-${dateTuple._2}-${dateTuple._3}", DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT))
  }

  protected def validDate(errKey: String, args: Seq[String] = Seq()): Constraint[(String, String, String)] = Constraint {
    input: (String, String, String) =>
      val date = Try {
        tupleToDate(input)
      }.toOption
      date match {
        case Some(_) => Valid
        case None => Invalid(errKey, args: _*)
      }
  }

  protected def nonEmptyDate(errKey: String, args: Seq[String] = Seq()): Constraint[(String, String, String)] = Constraint {
    case (_, _, "") | ("", _, _) | (_, "", _) => Invalid(errKey, args: _*)
    case _ => Valid
  }

  protected def nonEmptyDateFields(errKey: String, args: Seq[String] = Seq()): Constraint[(String, String, String)] = Constraint {
    case ("", "", "") => Invalid(errKey, args: _*)
    case ("", "", _) => Invalid(errKey + "DayMonth", args: _*)
    case ("", _, "") => Invalid(errKey + "DayYear", args: _*)
    case (_, "", "") => Invalid(errKey + "MonthYear", args: _*)
    case ("", _, _) => Invalid(errKey + "Day", args: _*)
    case (_, "", _) => Invalid(errKey + "Month", args: _*)
    case (_, _, "") => Invalid(errKey + "Year", args: _*)
    case _ => Valid
  }

  protected def dateCheck(errKey: DatePartErrorMessageKeys, args: Seq[String] = Seq()): Constraint[(String, String, String)] = Constraint {
    case ("", "", "") => Invalid(errKey.containsNothing, args: _*)
    case (_, "", "") => Invalid(errKey.containsOnlyDay, args: _*)
    case ("", _, "") => Invalid(errKey.containsOnlyMonth, args: _*)
    case ("", "", _) => Invalid(errKey.containsOnlyYear, args: _*)
    case ("", _, _) => Invalid(errKey.containsOnlyMonthYear, args: _*)
    case (_, "", _) => Invalid(errKey.containsOnlyDayYear, args: _*)
    case (_, _, "") => Invalid(errKey.containsOnlyDayMonth, args: _*)
    case _ => Valid
  }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def minDateWithSixAprilTwentyFifteenMinimum(startDate: LocalDate, errorKey: String, errorKey6April2015: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(startDate) && startDate.isAfter(sixthAprilTwentyFifteen) =>
        Invalid(errorKey, args: _*) // case where start date is after 6-4-2015
      case date if date.isBefore(sixthAprilTwentyFifteen) && startDate.isBefore(sixthAprilTwentyFifteen) =>
        Invalid(errorKey6April2015, args: _*) // case where start date is before 6-4-2015
      case _ =>
        Valid
    }

  case class DatePartErrorMessageKeys(containsNothing: String,
                                      containsOnlyDay: String,
                                      containsOnlyMonth: String,
                                      containsOnlyYear: String,
                                      containsOnlyDayMonth: String,
                                      containsOnlyDayYear: String,
                                      containsOnlyMonthYear: String)
}