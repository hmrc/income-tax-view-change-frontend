/*
 * Copyright 2021 HM Revenue & Customs
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

package models.financialDetails

import assets.FinancialDetailsTestConstants.fullDocumentDetailModel
import uk.gov.hmrc.play.test.UnitSpec

class DocumentDetailSpec extends UnitSpec {

  "DocumentDetail" when {

    "calling predicate hasAccruingInterest" should {

      "return true" when {
        "interestOutstandingAmount exists and latePaymentInterestAmount is a non-negative amount or does not exist" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = Some(0))
            .hasAccruingInterest shouldBe true

          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = Some(-1))
            .hasAccruingInterest shouldBe true

          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = None)
            .hasAccruingInterest shouldBe true
        }
      }

      "return false" when {
        "interestOutstandingAmount exists and latePaymentInterestAmount is a positive amount" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = Some(1), latePaymentInterestAmount = Some(2))
            .hasAccruingInterest shouldBe false
        }

        "interestOutstandingAmount does not exist and latePaymentInterestAmount is a non-negative amount or does not exist" in {
          fullDocumentDetailModel.copy(interestOutstandingAmount = None, latePaymentInterestAmount = Some(0))
            .hasAccruingInterest shouldBe false

          fullDocumentDetailModel.copy(interestOutstandingAmount = None, latePaymentInterestAmount = Some(-1))
            .hasAccruingInterest shouldBe false

          fullDocumentDetailModel.copy(interestOutstandingAmount = None, latePaymentInterestAmount = None)
            .hasAccruingInterest shouldBe false
        }
      }

    }

  }

}
