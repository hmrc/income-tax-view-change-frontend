package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}

object OptOutTestSupport {

  def buildOneYearOptOutDataForPreviousYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildOneYearOptOutDataForCurrentYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Mandated, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildOneYearOptOutDataForNextYear(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = true)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Mandated, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

  def buildMultiYearOptOutData(chosenCurrentYear: Int = 2024): OptOutProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val previousTaxYearOptOut = PreviousOptOutTaxYear(Voluntary, previousYear,  crystallised = false)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(Voluntary, currentYear)
    val extTaxYearOptOut = NextOptOutTaxYear(Voluntary, nextYear, currentTaxYearOptOut)

    OptOutProposition(
      previousTaxYearOptOut,
      currentTaxYearOptOut,
      extTaxYearOptOut
    )
  }

}