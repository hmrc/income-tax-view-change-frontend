package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import testUtils.UnitSpec

class NextTaxYearOptOutSpec extends UnitSpec {

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(2024)
  val nextTaxYear: TaxYear = currentTaxYear.nextYear

  val anyItsaStatusCurrentTaxYearOptOut: CurrentTaxYearOptOut = CurrentTaxYearOptOut(ITSAStatus.Voluntary, currentTaxYear)

  "NextTaxYearOptOut" should {
    "offer opt-out" when {
      s"next year is ${ITSAStatus.Voluntary}" when {
        "with any ITSA status for current year" in {
          NextTaxYearOptOut(ITSAStatus.Voluntary, nextTaxYear, anyItsaStatusCurrentTaxYearOptOut).canOptOut shouldBe true
        }
      }

      s"next year is ${ITSAStatus.NoStatus}" when {
        s"with current year ${ITSAStatus.Voluntary}" in {
          val currentYear = CurrentTaxYearOptOut(ITSAStatus.Voluntary, currentTaxYear)
          NextTaxYearOptOut(ITSAStatus.NoStatus, nextTaxYear, currentYear).canOptOut shouldBe true
        }
      }
    }

    "not offer opt-out" when {

      s"current year status any ITSAStatus" when {

        s"next year is ${ITSAStatus.Mandated}" in {
          val currentTaxYearOptOut = CurrentTaxYearOptOut(ITSAStatus.Voluntary, currentTaxYear)
          NextTaxYearOptOut(ITSAStatus.Mandated, nextTaxYear, currentTaxYearOptOut).canOptOut shouldBe false
        }

        s"next year is ${ITSAStatus.Annual}" in {
          val currentTaxYearOptOut = CurrentTaxYearOptOut(ITSAStatus.Voluntary, currentTaxYear)
          NextTaxYearOptOut(ITSAStatus.Annual, nextTaxYear, currentTaxYearOptOut).canOptOut shouldBe false
        }
      }

      s"current year status ${ITSAStatus.NoStatus}" when {
        s"next year is ${ITSAStatus.NoStatus}" in {
          val currentTaxYearOptOut = CurrentTaxYearOptOut(ITSAStatus.NoStatus, currentTaxYear)
          NextTaxYearOptOut(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut).canOptOut shouldBe false
        }
      }

    }
  }

}