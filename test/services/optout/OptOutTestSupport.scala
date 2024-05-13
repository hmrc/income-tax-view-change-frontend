package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}

object OptOutTestSupport {

  def buildOneYearOptOutDataForPreviousYear(chosenCurrentYear: Int = 2024): OptOutData = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousTaxYearOptOut(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentTaxYearOptOut(Mandated, currentYear)
    val extTaxYearOptOut = NextTaxYearOptOut(Mandated, nextYear, currentTaxYearOptOut)

    OptOutData(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildOneYearOptOutDataForCurrentYear(chosenCurrentYear: Int = 2024): OptOutData = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousTaxYearOptOut(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentTaxYearOptOut(Voluntary, currentYear)
    val extTaxYearOptOut = NextTaxYearOptOut(Mandated, nextYear, currentTaxYearOptOut)

    OptOutData(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildOneYearOptOutDataForNextYear(chosenCurrentYear: Int = 2024): OptOutData = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousTaxYearOptOut(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentTaxYearOptOut(Mandated, currentYear)
    val extTaxYearOptOut = NextTaxYearOptOut(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutData(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildMultiYearOptOutData(chosenCurrentYear: Int = 2024): OptOutData = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousTaxYearOptOut(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentTaxYearOptOut(Voluntary, currentYear)
    val extTaxYearOptOut = NextTaxYearOptOut(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutData(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

}