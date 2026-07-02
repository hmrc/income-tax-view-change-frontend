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

package financials.services

import common.auth.MtdItUser
import common.auth.actions.AuthActionsTestData.defaultMTDITUser
import common.config.featureswitch.FeatureSwitching
import common.testConstants.IncomeSourceDetailsTestConstants.oldUserDetails
import common.testUtils.TestSupport
import financials.mocks.connectors.MockFinancialDetailsConnector
import financials.models.FinancialDetailsErrorModel
import financials.services.CreditHistoryService.CreditHistoryError
import financials.services.helpers.CreditHistoryDataHelper
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class CreditHistoryServiceSpec extends TestSupport with MockFinancialDetailsConnector
  with FeatureSwitching with CreditHistoryDataHelper {

  val user: MtdItUser[_] = defaultMTDITUser(Some(Individual), oldUserDetails)

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object TestCreditHistoryService extends CreditHistoryService(mockFinancialDetailsConnector, appConfig)

  "getCreditHistory" when {

    "an error is returned from the connector" should {

      "return a credit history error (~getFinancialDetails failed)" in {
        setupMockGetFinancialDetails(taxYear, nino)(FinancialDetailsErrorModel(500, "ERROR"))
        setupMockGetFinancialDetails(taxYear + 1, nino)(FinancialDetailsErrorModel(500, "ERROR"))
        TestCreditHistoryService.getCreditsHistory(taxYear, nino).futureValue shouldBe Left(CreditHistoryError)
      }
    }

    "a successful response returned from the connector" when {
      "entering the service" should {
        "return a list of MFA/BC/CutOver credits" in {
          setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetails)
          setupMockGetFinancialDetails(taxYear + 1, nino)(taxYearFinancialDetails_PlusOneYear)
          val futureResult = TestCreditHistoryService.getCreditsHistory(taxYear, nino)
          whenReady(futureResult) { result =>
            result shouldBe Right(List(creditDetailModelasCutOver, creditDetailModelasMfa, creditDetailModelasBCC))
          }
        }
        "return a list of all credits" in {
          setupMockGetFinancialDetails(taxYear, nino)(taxYearFinancialDetailsAllCredits)
          setupMockGetFinancialDetails(taxYear + 1, nino)(taxYearFinancialDetailsAllCreditsPlusOneYear)
          val futureResult = TestCreditHistoryService.getCreditsHistory(taxYear, nino)
          whenReady(futureResult) { result =>
            result shouldBe Right(List(
              creditDetailModelasSetInterest, creditDetailModelasCutOver, creditDetailModelasMfa, creditDetailModelasBCC))
          }
        }
      }
    }
  }

}
