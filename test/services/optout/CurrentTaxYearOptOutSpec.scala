package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import testUtils.UnitSpec

class CurrentTaxYearOptOutSpec extends UnitSpec {

  "CurrentTaxYearOptOut" should {
    "offer opt-out" when {
      s"current year is ${ITSAStatus.Voluntary}" in {
        CurrentTaxYearOptOut(ITSAStatus.Voluntary, TaxYear.forYearEnd(2024)).canOptOut shouldBe true
      }
    }

    "not offer opt-out" when {
      s"current year is ${ITSAStatus.Mandated}" in {
        CurrentTaxYearOptOut(ITSAStatus.Mandated, TaxYear.forYearEnd(2024)).canOptOut shouldBe false
      }

      s"current year is ${ITSAStatus.Annual}" in {
        CurrentTaxYearOptOut(ITSAStatus.Annual, TaxYear.forYearEnd(2024)).canOptOut shouldBe false
      }

      s"current year is ${ITSAStatus.NoStatus}" in {
        CurrentTaxYearOptOut(ITSAStatus.NoStatus, TaxYear.forYearEnd(2024)).canOptOut shouldBe false
      }
    }
  }
}