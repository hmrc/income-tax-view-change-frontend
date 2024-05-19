package services.optout

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OptOutPropositionTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  "OptOutData" when {

    "only previous year is available for opt-out" should {

      "return true for isOneYearOptOut " in {
        val data = OptOutTestSupport.buildOneYearOptOutDataForPreviousYear()
        data.isOneYearOptOut shouldBe true
        data.isMultiYearOptOut shouldBe false
        data.isNoOptOutAvailable shouldBe false
      }
    }

    "only current year is available for opt-out" should {

      "return true for isOneYearOptOut " in {
        val data = OptOutTestSupport.buildOneYearOptOutDataForCurrentYear()
        data.isOneYearOptOut shouldBe true
        data.isMultiYearOptOut shouldBe false
        data.isNoOptOutAvailable shouldBe false
      }
    }

    "only next year is available for opt-out" should {

      "return true for isOneYearOptOut " in {
        val data = OptOutTestSupport.buildOneYearOptOutDataForNextYear()
        data.isOneYearOptOut shouldBe true
        data.isMultiYearOptOut shouldBe false
        data.isNoOptOutAvailable shouldBe false
      }
    }
  }

}