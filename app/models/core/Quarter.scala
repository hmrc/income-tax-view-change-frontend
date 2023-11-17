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

package models.core

import models.core.Quarter._
import models.core.TaxYearId.{mkTaxYear, mkTaxYearId}

import java.time.LocalDate
import scala.util.Try

// https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax
// This type to be create only with association to specific TaxYear
// See smart constructors section
final case class Quarter private(taxYear: TaxYearId, number: Int)  {
  import Quarter._
  def start: LocalDate = ??? // Define quarter start/end date based on taxYear and quarter
  def end: LocalDate = ???

  def next : Quarter = {
    number match {
      case 4 => taxYear.next.quarters(firstQuarter)
      case _ => taxYear.quarters(number + 1)
    }
  }

  def prev: Quarter = {
    number match {
      case 1 => taxYear.prev.quarters(lastQuarter)
      case _ => taxYear.quarters(number - 1)
    }
  }

  def deadline: LocalDate = ???
  def contains(date: LocalDate) : Boolean = {
    // TODO: apply proper test coverage for this method to deal with edge cases
    date.toEpochDay >= start.toEpochDay && date.toEpochDay <= end.toEpochDay
  }
}

object Quarter {

  private val firstQuarter : Int = 1
  private val lastQuarter : Int = 4

  def mkQuarter(taxYear: TaxYearId, number: Int) : Either[Throwable, Quarter] = Try {
    require(number > 0 && number < 5, "Quarter number must be between 1 and 4")
    taxYear.quarters(number)
  }.toEither

  def mkQuarter(date: LocalDate) : Quarter = {
    val taxYear = mkTaxYear(date)
    // as taxYear created based on the date we should be find relevant quarter
    taxYear.quarters.find(_.contains(date)).get
  }
}

