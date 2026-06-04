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

import businessDetails.mocks.services.MockTriggeredMigrationService
import businessDetails.models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}
import businessDetails.services.triggeredMigration.TriggeredMigrationService
import common.connectors.{ITSAStatusConnector, IncomeTaxCalculationConnector}
import common.enums.IncomeSourceJourney.SelfEmployment
import common.enums.MTDIndividual
import common.enums.TriggeredMigration.{TriggeredMigrationAdded, TriggeredMigrationCeased}
import common.mocks.auth.MockAuthActions
import common.models.admin.TriggeredMigration
import common.services.DateServiceInterface
import models.core.IncomeSourceId
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncomeUnconfirmed

import scala.concurrent.Future

class CheckHmrcRecordsControllerSpec extends MockAuthActions with MockTriggeredMigrationService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[TriggeredMigrationService].toInstance(mockTriggeredMigrationService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
      api.inject.bind[IncomeTaxCalculationConnector].toInstance(mockIncomeTaxCalculationConnector)
    ).build()

  lazy val testController = app.injector.instanceOf[CheckHmrcRecordsController]

  val checkHmrcRecordsSoleTraderDetails = CheckHmrcRecordsSoleTraderDetails(
    incomeSourceId = IncomeSourceId("XA00001234"),
    incomeSource = Some("Testing"),
    businessName = Some("Test Inc")
  )
  val testCheckHmrcRecordsViewModel = CheckHmrcRecordsViewModel(
    soleTraderBusinesses = List(checkHmrcRecordsSoleTraderDetails),
    hasActiveUkProperty = true,
    hasActiveForeignProperty = true,
    triggeredMigrationState = None
  )

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      s"the user is authenticated as a $mtdRole" should {
        "render the Check HMRC Records page" when {
          "state is None" in {
            val action = testController.show(isAgent)
            setupMockSuccess(mtdRole, false, List(TriggeredMigration))
            mockItsaStatusRetrievalAction()
            mockTriggeredMigrationRetrievalAction()
            mockGetCheckHmrcRecordsViewModel(testCheckHmrcRecordsViewModel)

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 200
          }

          "state is TriggeredMigrationCeased" in {
            val action = testController.show(isAgent, Some(TriggeredMigrationCeased.toString))
            setupMockSuccess(mtdRole, false, List(TriggeredMigration))
            mockItsaStatusRetrievalAction()
            mockTriggeredMigrationRetrievalAction()
            mockGetCheckHmrcRecordsViewModel(testCheckHmrcRecordsViewModel.copy(triggeredMigrationState = Some(TriggeredMigrationCeased)))

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 200
          }

          "state is TriggeredMigrationAdded" in {
            val action = testController.show(isAgent, Some(TriggeredMigrationAdded(SelfEmployment).toString))
            setupMockSuccess(mtdRole, false, List(TriggeredMigration))
            mockItsaStatusRetrievalAction()
            mockTriggeredMigrationRetrievalAction()
            mockGetCheckHmrcRecordsViewModel(testCheckHmrcRecordsViewModel.copy(triggeredMigrationState = Some(TriggeredMigrationAdded(SelfEmployment))))

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
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction()

            when(
              mockIncomeSourceConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncomeUnconfirmed))

            val result = action(fakeRequest)

            status(result) shouldBe 303
            redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
          }
        }
      }
      testMTDAuthFailuresForRole(testController.show(isAgent), mtdRole)(fakeRequest)
    }
  }
}
