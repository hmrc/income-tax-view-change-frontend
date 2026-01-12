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

package models

import exceptions.MissingFieldException
import models.financialDetails.{BalanceDetails, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.scalatest.matchers.should.Matchers
import services.DateService
import testConstants.BaseTestConstants.app
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._
import testUtils.UnitSpec

import java.time.LocalDate

class WhatYouOweChargesListModelSpec extends UnitSpec with Matchers with ChargeConstants {

  implicit val dateService: DateService = app.injector.instanceOf[DateService]
  lazy val fixedDate : LocalDate = LocalDate.of(2023, 12, 15)

  val outstandingCharges: OutstandingChargesModel = outstandingChargesModel(fixedDate.minusMonths(13))

  def whatYouOweAllData(dunningLock: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLock)
      ++ financialDetailsDueInMoreThan30DaysCi(dunningLock)
      ++ financialDetailsOverdueDataCi(dunningLock),
    outstandingChargesModel = Some(outstandingCharges)
  )

  def whatYouOweFinancialDataWithoutOutstandingCharges(dunningLock: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    chargesList = financialDetailsDueIn30DaysCi(dunningLock)
      ++ financialDetailsDueInMoreThan30DaysCi(dunningLock)
      ++ financialDetailsOverdueDataCi(dunningLock)
  )


  "The WhatYouOweChargesList model" when {

    "getRelevantDueDate" when {

      "successfully gets relevantDueDate" in {

        val whatYouOweChargesList = whatYouOweAllData()

        whatYouOweChargesList.getRelevantDueDate shouldBe LocalDate.of(2022, 11, 15)
      }

      "throws MissingFieldException when relevantDueDate is not found" in {

        val outstandingChargeWithoutRelevantDueDate = OutstandingChargeModel(
          chargeName = "BCD",
          relevantDueDate = None,
          chargeAmount = 100.00,
          tieBreaker = 1
        )

        val whatYouOweChargesList = whatYouOweAllData().copy(
          outstandingChargesModel = Some(OutstandingChargesModel(List(outstandingChargeWithoutRelevantDueDate)))
        )

        val exception = intercept[MissingFieldException] {
          whatYouOweChargesList.getRelevantDueDate
        }
        exception shouldBe MissingFieldException("documentRelevantDueDate")
      }

      "throws MissingFieldException when there is no bcdChargeType" in {

        val whatYouOweChargesList = whatYouOweFinancialDataWithoutOutstandingCharges()

        val exception = intercept[MissingFieldException] {
          whatYouOweChargesList.getRelevantDueDate
        }

        exception shouldBe MissingFieldException("documentBcdChargeType")
      }

    }

    "getAciChargeWithTieBreakerChargeAmount" when {

      "successfully gets aciChargeWithTieBreakerChargeAmount" in {

        val whatYouOweChargesList = whatYouOweAllData()

        whatYouOweChargesList.getAciChargeWithTieBreakerChargeAmount shouldBe 12.67
      }

      "throws MissingFieldException when aciChargeWithTieBreakerChargeAmount is not found" in {

        val aciChargeWithoutTieBreaker = OutstandingChargeModel(
          chargeName = "ACI",
          relevantDueDate = Some(LocalDate.of(2023, 12, 15)),
          chargeAmount = 100.00,
          tieBreaker = 1
        )

        val whatYouOweChargesList = whatYouOweAllData().copy(
          outstandingChargesModel = Some(OutstandingChargesModel(List(aciChargeWithoutTieBreaker)))
        )

        val exception = intercept[MissingFieldException] {
          whatYouOweChargesList.getAciChargeWithTieBreakerChargeAmount
        }
        exception shouldBe MissingFieldException("documentAciChargeWithTieBreaker")
      }

      "throws MissingFieldException when there is no aciChargeType" in {

        val whatYouOweChargesList = whatYouOweFinancialDataWithoutOutstandingCharges()

        val exception = intercept[MissingFieldException] {
          whatYouOweChargesList.getAciChargeWithTieBreakerChargeAmount
        }
        exception shouldBe MissingFieldException("documentAciChargeType")
      }


    }

    "all values in model exists with tie breaker matching in OutstandingCharges Model" should {
      "bcdChargeTypeDefinedAndGreaterThanZero is true" in {
        whatYouOweAllData().bcdChargeTypeDefinedAndGreaterThanZero shouldBe true
      }
      "isChargesListEmpty is false" in {
        whatYouOweAllData().isChargesListEmpty shouldBe false
      }
      "getDefaultPaymentAmount should have correct values" in {
        whatYouOweAllData().getDefaultPaymentAmount shouldBe Some(123456.67)
      }
      "hasDunningLock should return false if there are no dunningLocks" in {
        whatYouOweAllData().hasDunningLock shouldBe false
      }
      "hasDunningLock should return true if there are is one dunningLock" in {
        whatYouOweAllData(oneDunningLock).hasDunningLock shouldBe true
      }
      "hasDunningLock should return true if there are multiple dunningLocks" in {
        whatYouOweAllData(twoDunningLocks).hasDunningLock shouldBe true
      }
    }

    "all values in model exists except outstanding charges" should {
      "bcdChargeTypeDefinedAndGreaterThanZero is false" in {
        whatYouOweFinancialDataWithoutOutstandingCharges().bcdChargeTypeDefinedAndGreaterThanZero shouldBe false
      }
      "isChargesListEmpty is false" in {
        whatYouOweFinancialDataWithoutOutstandingCharges().isChargesListEmpty shouldBe false
      }
      "getDefaultPaymentAmount should have correct values" in {
        whatYouOweFinancialDataWithoutOutstandingCharges()
          .getDefaultPaymentAmount.get shouldBe fixedDate.minusDays(10).getYear
        whatYouOweFinancialDataWithoutOutstandingCharges().getDefaultPaymentAmount.get shouldBe 50.0
      }
    }
  }
}
