package models.core

import models.core.TaxYearId.{mkTaxYear, mkTaxYearId}

import java.time.LocalDate
import scala.util.Try

// https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax
// This type to be create only with association to specific TaxYear
// See smart contsturctors section
class Quarter private(val number: Int) extends AnyVal {
  def start: LocalDate = ??? // Define quarter start date
  def end: LocalDate = ???   // Define quarter end date
  def deadline: LocalDate = ???
  def contains(date: LocalDate) : Boolean = {
    // TODO: apply proper test coverage for this method to deal with edge cases
    date.toEpochDay >= start.toEpochDay && date.toEpochDay <= end.toEpochDay
  }
}

object Quarter {
  def mkQuarter(number: Int, taxYear: TaxYearId) : Either[Throwable, Quarter] = Try {
    require(number > 0 && number < 5, "Quarter number must be between 1 and 4")
    taxYear.quarters(number)
  }.toEither

  def mkQuarter(date: LocalDate) : Either[Throwable, Quarter] = {
    val taxYear = mkTaxYear(date)
    taxYear.quarters.find(_.contains(date)) match {
      case Some(quarter) => Right(quarter)
      case None => {
        Left(new Error(s"TaxYear: ${taxYear.firstYear}-${taxYear.secondYear} does not contain date: $date"))
      }
    }

  }
}

