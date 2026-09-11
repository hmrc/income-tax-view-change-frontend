/*
 * Copyright 2025 HM Revenue & Customs
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

package businessDetails.controllers.triggeredMigration

import businessDetails.mocks.services.{MockCustomerFactsUpdateService, MockSessionService, MockTriggeredMigrationService}
import businessDetails.services.SessionService
import common.connectors.{ITSAStatusConnector, IncomeTaxCalculationConnector}
import common.enums.MTDIndividual
import common.enums.TriggeredMigration.Channel.HmrcUnconfirmed
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockDateService
import common.models.admin.TriggeredMigration
import common.models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import common.models.itsaStatus.ITSAStatusYearOfMigrationModel
import common.services.{CustomerFactsUpdateService, DateService, DateServiceInterface, ITSAStatusService, YearOfMigrationService}
import common.testConstants.BaseTestConstants.*
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeNoYearOfMigration
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatestplus.mockito.MockitoSugar.mock => sMock
import play.api
import play.api.Application
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}

import scala.concurrent.Future

class CompleteStepsControllerSpec extends MockAuthActions with MockTriggeredMigrationService with MockSessionService with MockDateService with MockCustomerFactsUpdateService {

  lazy val mockYearOfMigrationService = sMock[YearOfMigrationService]
  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[DateService].toInstance(mockDateServiceInjected),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[YearOfMigrationService].toInstance(mockYearOfMigrationService),
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[IncomeTaxCalculationConnector].toInstance(mockIncomeTaxCalculationConnector),
      api.inject.bind[CustomerFactsUpdateService].toInstance(mockCustomerFactsUpdateService),
    ).build()

  lazy val testController: CompleteStepsController = app.injector.instanceOf[CompleteStepsController]

  val singleBusinessIncomeWithYearOfMigration = IncomeSourceDetailsModel("AA123456A", testMtditid, Some("2018"), List(business1), Nil, channel = HmrcUnconfirmed.getValue)

  val singleBusinessIncomeUnconfirmed: IncomeSourceDetailsModel = singleBusinessIncomeNoYearOfMigration.copy(channel = HmrcUnconfirmed.getValue)

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      s"the user is authenticated as a $mtdRole" should {
        "render the Check HMRC Records page" when {
          "state is None" in {
            val taxYear = TaxYear(2023, 2024)
            val action = testController.show(isAgent)
            setupMockSuccess(mtdRole, false, List(TriggeredMigration))
            setupMockClearSession()
            mockUpdateCustomerFacts()
            mockItsaStatusRetrievalAction(taxYear = taxYear)
            mockTriggeredMigrationRetrievalAction()

            setupMockGetCurrentTaxYear(mockDateServiceInjected)(taxYear)

            when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
              .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some(taxYear.startYear.toString))))

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 200
          }

          "state is TriggeredMigrationCeased" in {
            val taxYear = TaxYear(2023, 2024)
            val action = testController.show(isAgent)
            setupMockSuccess(mtdRole, false, List(TriggeredMigration))
            mockItsaStatusRetrievalAction(taxYear = taxYear)
            mockTriggeredMigrationRetrievalAction()
            
            when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
              .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some(taxYear.startYear.toString))))

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 200
          }

          "state is TriggeredMigrationAdded" in {
            val taxYear = TaxYear(2023, 2024)
            val action = testController.show(isAgent)
            setupMockSuccess(mtdRole, false, List(TriggeredMigration))
            mockItsaStatusRetrievalAction(taxYear = taxYear)
            mockTriggeredMigrationRetrievalAction()
            
            when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
              .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some(taxYear.startYear.toString))))

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 200
          }
        }
        "redirect to the home page" when {
          val action = testController.show(isAgent)
          "the triggered migration feature switch is disabled" in {
            val taxYear = TaxYear(2023, 2024)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(taxYear = taxYear)
            
            when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
              .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some(taxYear.startYear.toString))))

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 303
            redirectLocation(result).get should include(appConfig.homePageUrl(isAgent, newHubContextRootEnabled))
          }
        }
      }
      testMTDAuthFailuresForRole(testController.show(isAgent), mtdRole)(fakeRequest)
    }
  }
}
