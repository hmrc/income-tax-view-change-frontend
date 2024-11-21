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

package controllers.incomeSources.cease

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import enums.MTDIndividual
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockIncomeSourceDetailsService, MockSessionService}
import models.admin.IncomeSources
import models.incomeSourceDetails.viewmodels.CeaseIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{IncomeSourceDetailsService, SessionService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.BusinessDetailsTestConstants.{ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2}
import testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData
import testUtils.TestSupport

import scala.concurrent.Future

class CeaseIncomeSourceControllerSpec extends MockAuthActions
  with MockSessionService {

  override def fakeApplication() = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
    ).build()

  val testCeaseIncomeSourceController = fakeApplication().injector.instanceOf[CeaseIncomeSourceController]

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = getFakeRequestBasedOnMTDUserType(mtdRole)
    s"show${if (mtdRole != MTDIndividual) "Agent"}" when {
      val action = if (mtdRole == MTDIndividual) testCeaseIncomeSourceController.show() else testCeaseIncomeSourceController.showAgent()
      s"the user is authenticated as a $mtdRole" should {
        "be redirected back to the home page" when {
          "income source is disabled" in {
            setupMockSuccess(mtdRole)
            disable(IncomeSources)
            mockSingleBISWithCurrentYearAsMigrationYear()

            val result = action(fakeRequest)

            val expectedRedirectUrl = if (mtdRole == MTDIndividual) {
              controllers.routes.HomeController.show().url
            } else {
              controllers.routes.HomeController.showAgent.url
            }

            redirectLocation(result) shouldBe Some(expectedRedirectUrl)
          }
        }
        "redirect user to the cease an income source page" when {
          "income source is enabled" in {
            setupMockSuccess(mtdRole)
            enable(IncomeSources)
            mockBothIncomeSources()
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, SelfEmployment)))))
            setupMockDeleteSession(true)

            when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
              .thenReturn(Right(CeaseIncomeSourcesViewModel(
                soleTraderBusinesses = List(ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2),
                ukProperty = Some(ceaseUkPropertyDetailsViewModel),
                foreignProperty = Some(ceaseForeignPropertyDetailsViewModel),
                ceasedBusinesses = Nil)))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
            //No redirect, cause it says redirect?
            //Agent ->  val result = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))
            //Individual -> val result = controller.show()(fakeRequestWithActiveSession)
          }
        }
        "show error page" when {
          "income source is enabled" in {
            setupMockSuccess(mtdRole)
            enable(IncomeSources)
            mockBothIncomeSources()

            when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any()))
              .thenReturn(Left(MissingFieldException("Trading Name")))

            val result: Future[Result] = action(fakeRequest)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            //No redirect, cause it says redirect?
            //Agent -> val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient("AB123456C"))
            //Individual -> val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
          }
        }
      }
    }
  }
}
