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

package services

import auth.MtdItUser
import config.featureswitch.{CutOverCredits, FeatureSwitching, MFACreditsAndDebits}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.financialDetails.FinancialDetailsErrorModel
import play.api.test.FakeRequest
import services.CreditHistoryService.CreditHistoryError
import services.helpers.CreditHistoryDataHelper
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.oldUserDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

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
    userType = Some(Individual),
    None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object TestCreditHistoryService extends CreditHistoryService(mockIncomeTaxViewChangeConnector, appConfig)

  "getCreditHistory" when {

    "an error is returned from the connector" should {

      "return a credit history error (~getFinancialDetails failed)" in {
        setupMockGetFinancialDetails(taxYear, nino)(FinancialDetailsErrorModel(500, "ERROR"))
        setupMockGetFinancialDetails(taxYear + 1, nino)(FinancialDetailsErrorModel(500, "ERROR"))
        TestCreditHistoryService.getCreditsHistory(taxYear, nino, false, false).futureValue shouldBe Left(CreditHistoryError)
      }
    }

    "a successful response returned from the connector" when {
      "feature switches of both MFACreditsAndDebits and CutOverCredits are enabled" should {
        "return a list of MFA/BC/CutOver credits" in {
          enable(MFACreditsAndDebits)
          enable(CutOverCredits)
          setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
          setupMockGetFinancialDetails(taxYear + 1, nino)(taxYearFinancialDetails_PlusOneYear)
          val futureResult = TestCreditHistoryService.getCreditsHistory(taxYear, nino, true, true)
          whenReady(futureResult) { result =>
            result shouldBe Right(List(creditDetailModelasCutOver, creditDetailModelasMfa, creditDetailModelasBCC))
          }
        }
      }
      "feature switch of MFACreditsAndDebits is enabled and CutOverCredits is disabled" should {
        "return a list of MFA and BC credits" in {
          enable(MFACreditsAndDebits)
          disable(CutOverCredits)
          setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
          setupMockGetFinancialDetails(taxYear + 1, nino)(taxYearFinancialDetails_PlusOneYear)
          val futureResult = TestCreditHistoryService.getCreditsHistory(taxYear, nino, true, false)
          whenReady(futureResult) { result =>
            result shouldBe Right(List(creditDetailModelasMfa, creditDetailModelasBCC))
          }
        }
      }
      "feature switch of MFACreditsAndDebits is disabled and CutOverCredits is enabled" should {
        "return a list of Cutover and BC credits" in {
          disable(MFACreditsAndDebits)
          enable(CutOverCredits)
          setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
          setupMockGetFinancialDetails(taxYear + 1, nino)(taxYearFinancialDetails_PlusOneYear)
          val futureResult = TestCreditHistoryService.getCreditsHistory(taxYear, nino, false, true)
          whenReady(futureResult) { result =>
            result shouldBe Right(List(creditDetailModelasCutOver, creditDetailModelasBCC))
          }
        }
      }
      "feature switches of both MFACreditsAndDebits and CutOverCredits are disabled" should {
        "return a list of BC credits" in {
          disable(MFACreditsAndDebits)
          disable(CutOverCredits)
          setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
          setupMockGetFinancialDetails(taxYear + 1, nino)(taxYearFinancialDetails_PlusOneYear)
          val futureResult = TestCreditHistoryService.getCreditsHistory(taxYear, nino, false, false)
          whenReady(futureResult) { result =>
            result shouldBe Right(List(creditDetailModelasBCC))
          }
        }
      }
    }
  }
}
