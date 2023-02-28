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

package controllers

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.agent.utils
import controllers.agent.utils.SessionKeys.clientNino
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import mocks.views.agent.MockTaxYears
import models.liabilitycalculation.{Inputs, LiabilityCalculationError, LiabilityCalculationResponse, Metadata, PersonalInformation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.{CalculationService, IncomeSourceDetailsService}
import testConstants.BaseTestConstants.{testCredId, testMtditid, testNino, testRetrievedUserName, testUserTypeIndividual}
import testConstants.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testUtils.TestSupport
import views.html.FinalTaxCalculationView

import scala.concurrent.Future
class FinalTaxCalculationControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions with MockItvcErrorHandler with MockTaxYears with ImplicitDateFormatter with TestSupport with FeatureSwitching{

  val mockCalculationService: CalculationService = mock(classOf[CalculationService])
  val mockErrorHandler: ItvcErrorHandler = mock(classOf[ItvcErrorHandler])

  val testFinalTaxCalculationController = new FinalTaxCalculationController()(
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[FinalTaxCalculationView],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    mockErrorHandler,
    app.injector.instanceOf[FrontendAuthorisedFunctions],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[IncomeSourceDetailsService],
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[FrontendAppConfig]
  )

  val testCalcError: LiabilityCalculationError = LiabilityCalculationError(Status.OK, "Test message")
  val testCalcResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
    messages = None,
    calculation = None,
    metadata = Metadata(None))
  when(mockErrorHandler.showInternalServerError()(any()))
    .thenReturn(InternalServerError(HtmlFormat.empty))
  val taxYear = 2018
  val user: MtdItUser[AnyContent] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessAndPropertyAligned,
    btaNavPartial = None,
    saUtr = None,
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(FakeRequest())


  val noNameUser: MtdItUser[AnyContent] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = None,
    incomeSources = businessAndPropertyAligned,
    btaNavPartial = None,
    saUtr = None,
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(FakeRequest().withSession(utils.SessionKeys.clientUTR -> "1234567890",
    utils.SessionKeys.clientMTDID -> testMtditid,
    utils.SessionKeys.clientNino -> clientNino,
    utils.SessionKeys.confirmedClient -> "true",
    forms.utils.SessionKeys.calculationId -> "1234567890"))

  "handle show request" should(
    "return unknown error" when (
      "an unconventional error occurs" in {
        when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
          .thenReturn(Future.successful(testCalcError))
        val result: Future[Result] = testFinalTaxCalculationController.handleShowRequest(taxYear, mockErrorHandler, isAgent = false)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    )
  )

  "agent submit" should (
    "use blank first name" when {
      "client has no provided first name" in {
        val result: Future[Result] = testFinalTaxCalculationController.agentSubmit(taxYear)(noNameUser)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  )

  "agent final declaration submit" should{
    "return unknown error" when {
      "an unconventional error occurs" in {
        when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
          .thenReturn(Future.successful(testCalcError))
        val result: Future[Result] = testFinalTaxCalculationController.agentFinalDeclarationSubmit(taxYear, "Test Name")(user, headerCarrier)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return UTR missing error" when{
      "supplied a user with no UTR" in {
        when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
          .thenReturn(Future.successful(testCalcResponse))
        val result: Future[Result] = testFinalTaxCalculationController.agentFinalDeclarationSubmit(taxYear, "Test Name")(user, headerCarrier)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "final declaration submit" should (
    "return unknown error" when (
      "an unconventional error occurs" in {
        when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
          .thenReturn(Future.successful(testCalcError))
        val result: Future[Result] = testFinalTaxCalculationController.finalDeclarationSubmit(taxYear, Some("Test Name"))(user, headerCarrier)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      )
    )
}
