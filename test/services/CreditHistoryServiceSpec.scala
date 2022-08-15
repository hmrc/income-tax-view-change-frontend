/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.CreditDetailsModel
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel, Payment, Payments, PaymentsError, SubItem}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.test.FakeRequest
import services.CreditHistoryService.CreditHistoryError
import services.helpers.CreditHistoryDataHelper
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.oldUserDetails
import testUtils.TestSupport

import java.time.LocalDate

class CreditHistoryServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector
  with FeatureSwitching with CreditHistoryDataHelper {

  val user: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = oldUserDetails,
    btaNavPartial = None,
    saUtr = Some("saUtr"),
    credId = Some("credId"),
    userType = Some("Individual"),
    None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object TestCreditHistoryService extends CreditHistoryService(mockIncomeTaxViewChangeConnector, appConfig)

  "getCreditHistory" when {

    "an error is returned from the connector" should {
      "return a credit history error" in {
        setupMockGetFinancialDetails(taxYear, nino)(FinancialDetailsErrorModel(500, "ERROR"))
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Left(CreditHistoryError)
      }
    }

    "a successful Payment/Credit History response is returned from the connector" should {
      "return a list of MFA credits only" in {
        setupGetPayments(taxYear)(Payments(paymentsForTheGivenTaxYear))
        setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Right(creditModels)
      }

      "return a list of MFA and CutOver credits" in {
        setupGetPayments(taxYear)(Payments(creditsForTheGivenTaxYear))
        setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
        setupGetPaymentAllocationCharges(nino, documentId)(cutOverCreditsAsFinancialDocument)
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Right(cutOverCredit +: creditModels)
      }

    }

  }

}
