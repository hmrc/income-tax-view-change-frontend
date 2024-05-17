package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import testUtils.UnitSpec

class PreviousTaxYearOptOutSpec extends UnitSpec {

  val previousTaxYear = TaxYear.forYearEnd(2024).previousYear

  "PreviousTaxYearOptOut" should {
    "offer opt-out" when {
      "previous year is not crystallised" when {
        s"previous year is ${ITSAStatus.Voluntary}" in {
          PreviousTaxYearOptOut(ITSAStatus.Voluntary, previousTaxYear, crystallised = false).canOptOut shouldBe true
        }
      }
    }

    "not offer opt-out" when {
      "previous year is crystallised" when {
        s"previous year is ${ITSAStatus.Voluntary}" in {
          PreviousTaxYearOptOut(ITSAStatus.Voluntary, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.Annual}" in {
          PreviousTaxYearOptOut(ITSAStatus.Annual, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.Mandated}" in {
          PreviousTaxYearOptOut(ITSAStatus.Mandated, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.NoStatus}" in {
          PreviousTaxYearOptOut(ITSAStatus.NoStatus, previousTaxYear, crystallised = true).canOptOut shouldBe false
        }
      }

      "previous year is not crystallised" when {
        s"previous year is ${ITSAStatus.Mandated}" in {
          PreviousTaxYearOptOut(ITSAStatus.Mandated, previousTaxYear, crystallised = false).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.Annual}" in {
          PreviousTaxYearOptOut(ITSAStatus.Annual, previousTaxYear, crystallised = false).canOptOut shouldBe false
        }

        s"previous year is ${ITSAStatus.NoStatus}" in {
          PreviousTaxYearOptOut(ITSAStatus.NoStatus, previousTaxYear, crystallised = false).canOptOut shouldBe false
        }
      }
    }
  }
}
