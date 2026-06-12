/*
 * Copyright 2024 HM Revenue & Customs
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

package financials.controllers

import common.connectors.ITSAStatusConnector
import common.enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import common.implicits.ImplicitDateFormatter
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockDateService
import common.services.{DateService, DateServiceInterface}
import financials.services.PaymentHistoryService
import org.mockito.Mockito.mock
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.*
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeNotMigrated

class NotMigratedUserControllerSpec extends MockAuthActions
  with ImplicitDateFormatter with MockDateService {

  lazy val mockPaymentHistoryService: PaymentHistoryService = mock(classOf[PaymentHistoryService])
  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)
  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[PaymentHistoryService].toInstance(mockPaymentHistoryService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected)
    ).build()

  lazy val testController = app.injector.instanceOf[NotMigratedUserController]

  def mtdRoles = List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  mtdRoles.foreach{ case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showAgent() else testController.show()
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent" else ""}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the not migrated page" when {
            "the user has not migrated to ETMP" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(singleBusinessIncomeNotMigrated)
              mockSingleBusinessIncomeSource(userMigrated = false)

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              JsoupParse(result).toHtmlDocument.title() shouldBe s"How to claim a refund - Manage your Self Assessment - GOV.UK"
            }
          }
          "render the error page" when {
            "the user has already migrated to ETMP" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBusinessIncomeSource()

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
       testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}
