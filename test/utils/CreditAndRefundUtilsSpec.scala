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

package utils

import models.financialDetails.MfaCreditUtils
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor1
import testConstants.CreditAndRefundConstants.{balanceDetailsModel, documentDetailWithDueDateFinancialDetailListModel}
import testUtils.TestSupport
import utils.CreditAndRefundUtils.UnallocatedCreditType.{UnallocatedCreditFromOnePayment, UnallocatedCreditFromSingleCreditItem, maybeUnallocatedCreditType}

class CreditAndRefundUtilsSpec extends TestSupport {

  val validMfaCreditMainTypes: TableFor1[String] = Table(
    "mfaCreditDescription",
    "ITSA Overpayment Relief",
    "ITSA Standalone Claim",
    "ITSA Averaging Adjustment",
    "ITSA Literary Artistic Spread",
    "ITSA Loss Relief Claim",
    "ITSA Post Cessation Claim",
    "ITSA Pension Relief Claim",
    "ITSA PAYE in year Repayment",
    "ITSA NPS Overpayment",
    "ITSA In year Rept pension schm",
    "ITSA Increase in PAYE Credit",
    "ITSA CIS Non Resident Subbie",
    "ITSA CIS Incorrect Deductions",
    "ITSA Stand Alone Assessment",
    "ITSA Infml Dschrg Cntrct Sett",
    "ITSA Third Party Rept - FIS",
    "ITSA CGT Adjustments",
    "ITSA EIS Carry Back Claims",
    "ITSA Calc Error Correction",
    "ITSA Misc Credit"
  )

  "CreditAndRefundUtils trait" when {

    "maybeUnallocatedCreditType method called with proper values for unallocated credit from one payment" should {
      "return Option[UnallocatedCreditFromOnePayment]" in {
        val unallocatedCreditType = maybeUnallocatedCreditType(
          List(documentDetailWithDueDateFinancialDetailListModel(paymentLot = Some("paymentLot"), paymentLotItem = Some("paymentLot"))),
          Some(balanceDetailsModel(
            firstPendingAmountRequested = None,
            secondPendingAmountRequested = None,
            unallocatedCredit = Some(500.00)))
        )
        unallocatedCreditType shouldBe Some(UnallocatedCreditFromOnePayment)
      }
    }


    "maybeUnallocatedCreditType method called with proper values for unallocated credit from single credit item" should {

      "return Option[UnallocatedCreditFromSingleCreditItem]" in {
        forAll(validMfaCreditMainTypes) { validMfaCreditMainType =>
          val unallocatedCreditType = maybeUnallocatedCreditType(
            List(documentDetailWithDueDateFinancialDetailListModel(mainType = validMfaCreditMainType)),
            Some(balanceDetailsModel(
              firstPendingAmountRequested = None,
              secondPendingAmountRequested = None,
              unallocatedCredit = Some(500.00))
            ), isMFACreditsAndDebitsEnabled = true
          )

          unallocatedCreditType shouldBe Some(UnallocatedCreditFromSingleCreditItem)
        }
      }
    }

    "maybeUnallocatedCreditType method called with proper values for unallocated credit from single credit item as a cut over credit" should {
      "return Option[UnallocatedCreditFromSingleCreditItem]" in {
        val unallocatedCreditType = maybeUnallocatedCreditType(
          List(documentDetailWithDueDateFinancialDetailListModel(mainType = "ITSA Cutover Credits")),
          Some(balanceDetailsModel(
            firstPendingAmountRequested = None,
            secondPendingAmountRequested = None,
            unallocatedCredit = Some(500.00))
          ), isCutOverCreditEnabled = true
        )
        unallocatedCreditType shouldBe Some(UnallocatedCreditFromSingleCreditItem)
      }
    }

    "MFA Credit feature is disabled and maybeUnallocatedCreditType method called with proper values for unallocated credit from single credit item as a MFA Credit" should {
      "return None" in {
        val unallocatedCreditType = maybeUnallocatedCreditType(
          List(documentDetailWithDueDateFinancialDetailListModel(mainType = "ITSA Misc Credit")),
          Some(balanceDetailsModel(
            firstPendingAmountRequested = None,
            secondPendingAmountRequested = None,
            unallocatedCredit = Some(500.00))
          ),
          isCutOverCreditEnabled = true
        )
        unallocatedCreditType shouldBe None
      }
    }

    "Cut Over Credit feature is disabled and maybeUnallocatedCreditType method called with proper values for unallocated credit from single credit item as a Cutover Credits" should {
      "return None" in {
        val unallocatedCreditType = maybeUnallocatedCreditType(
          List(documentDetailWithDueDateFinancialDetailListModel(mainType = "ITSA Cutover Credits")),
          Some(balanceDetailsModel(
            firstPendingAmountRequested = None,
            secondPendingAmountRequested = None,
            unallocatedCredit = Some(500.00))
          ),
          isMFACreditsAndDebitsEnabled = true
        )
        unallocatedCreditType shouldBe None
      }
    }

    "maybeUnallocatedCreditType method called with not matched values should return None" in {
      val unallocatedCreditType = maybeUnallocatedCreditType(
        List(documentDetailWithDueDateFinancialDetailListModel()),
        Some(balanceDetailsModel()),
        isMFACreditsAndDebitsEnabled = true,
        isCutOverCreditEnabled = true
      )
      unallocatedCreditType shouldBe None
    }
  }
}
