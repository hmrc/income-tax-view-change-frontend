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
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.admin.IncomeSources
import models.incomeSourceDetails.CeaseIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify}
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.cease.DeclarePropertyCeased

import scala.concurrent.Future

class DeclarePropertyCeasedControllerSpec extends TestSupport with MockAuthenticationPredicate with
  MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockDeclarePropertyCeased: DeclarePropertyCeased = app.injector.instanceOf[DeclarePropertyCeased]

  object TestDeclarePropertyCeasedController extends DeclarePropertyCeasedController(
    mockAuthService,
    app.injector.instanceOf[DeclarePropertyCeased],
    sessionService = mockSessionService,
    testAuthenticator)(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val titleUkProperty: String = s"${messages("htmlTitle", messages("incomeSources.cease.UK.heading"))}"
    val titleAgentUkProperty: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.UK.heading"))}"
    val headingUkProperty: String = messages("incomeSources.cease.UK.heading")

    val titleForeignProperty: String = s"${messages("htmlTitle", messages("incomeSources.cease.FP.heading"))}"
    val titleAgentForeignProperty: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.FP.heading"))}"
    val headingForeignProperty: String = messages("incomeSources.cease.FP.heading")
  }


  def showCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Future[Result] = {
    (isAgent, incomeSourceType) match {
      case (true, UkProperty) => TestDeclarePropertyCeasedController.showAgent(UkProperty)(fakeRequestConfirmedClient())
      case (_, UkProperty) => TestDeclarePropertyCeasedController.show(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
      case (true, _) => TestDeclarePropertyCeasedController.showAgent(ForeignProperty)(fakeRequestConfirmedClient())
      case (_, _) => TestDeclarePropertyCeasedController.show(ForeignProperty)(fakeRequestWithNinoAndOrigin("pta"))
    }
  }

  def submitCall(isAgent: Boolean, incomeSourceType: IncomeSourceType, formBody: Option[Map[String, String]] = None): Future[Result] = {
    val formData = formBody.getOrElse(Map(
      DeclareIncomeSourceCeasedForm.declaration -> "true",
      DeclareIncomeSourceCeasedForm.ceaseCsrfToken -> "12345"
    ))

    (isAgent, incomeSourceType) match {
      case (true, UkProperty) => TestDeclarePropertyCeasedController.submitAgent(UkProperty)(fakePostRequestConfirmedClient()
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (_, UkProperty) => TestDeclarePropertyCeasedController.submit(UkProperty)(fakePostRequestWithNinoAndOrigin("pta")
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (true, _) => TestDeclarePropertyCeasedController.submitAgent(ForeignProperty)(fakePostRequestConfirmedClient()
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (_, _) => TestDeclarePropertyCeasedController.submit(ForeignProperty)(fakePostRequestWithNinoAndOrigin("pta")
        .withFormUrlEncodedBody(formData.toSeq: _*))
    }
  }

  def verifySetMongoKey(key: String, value: String, journeyType: JourneyType): Unit = {
    val argumentKey: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val argumentValue: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    val argumentJourneyType: ArgumentCaptor[JourneyType] = ArgumentCaptor.forClass(classOf[JourneyType])

    verify(mockSessionService).setMongoKey(argumentKey.capture(), argumentValue.capture(), argumentJourneyType.capture())(any(), any())
    argumentKey.getValue shouldBe key
    argumentValue.getValue shouldBe value
    argumentJourneyType.getValue.toString shouldBe journeyType.toString
  }

  "DeclarePropertyCeasedController.show / DeclarePropertyCeasedController.showAgent" should {
    "return 200 OK" when {
      def testViewReturnsOKWithCorrectContent(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        enable(IncomeSources)
        mockPropertyIncomeSource()

        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

        val result = showCall(isAgent, incomeSourceType)
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) shouldBe Status.OK

        (isAgent, incomeSourceType) match {
          case (true, UkProperty) =>
            document.title shouldBe TestDeclarePropertyCeasedController.titleAgentUkProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingUkProperty
          case (_, UkProperty) =>
            document.title shouldBe TestDeclarePropertyCeasedController.titleUkProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingUkProperty
          case (true, _) =>
            document.title shouldBe TestDeclarePropertyCeasedController.titleAgentForeignProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingForeignProperty
          case (_, _) =>
            document.title shouldBe TestDeclarePropertyCeasedController.titleForeignProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclarePropertyCeasedController.headingForeignProperty
        }
      }

      "income source is UK Property and FS Enabled - Individual" in {
        testViewReturnsOKWithCorrectContent(isAgent = false, UkProperty)
      }
      "income source is UK Property and FS Enabled - Agent" in {
        testViewReturnsOKWithCorrectContent(isAgent = false, UkProperty)
      }
      "income source is Foreign Property and FS Enabled - Individual" in {
        testViewReturnsOKWithCorrectContent(isAgent = false, ForeignProperty)
      }
      "income source is Foreign Property and FS Enabled - Agent" in {
        testViewReturnsOKWithCorrectContent(isAgent = true, ForeignProperty)
      }
    }


    "return 303 SEE_OTHER and redirect to home page" when {
      def testFeatureSwitchRedirectsToHomePage(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)

        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = showCall(isAgent, incomeSourceType)
        status(result) shouldBe Status.SEE_OTHER

        val expectedRedirectUrl: String = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "navigating to the UK Property declaration page with FS Disabled - Individual" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = false, UkProperty)
      }
      "navigating to the UK Property declaration page with FS Disabled - Agent" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = true, UkProperty)
      }
      "navigating to the Foreign Property declaration page with FS Disabled - Individual" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = false, ForeignProperty)
      }
      "navigating to the Foreign Property declaration page with FS Disabled - Agent" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = true, ForeignProperty)
      }
    }


    "return 303 SEE_OTHER and redirect to error page" when {
      def testUnauthorisedUserRedirectsToSignIn(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationException(isAgent)

        val result: Future[Result] = showCall(isAgent, incomeSourceType)
        status(result) shouldBe Status.SEE_OTHER

        val expectedRedirectUrl: String = if (isAgent) controllers.agent.routes.ClientRelationshipFailureController.show.url else
          controllers.errors.routes.NotEnrolledController.show.url
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "unauthorised user navigates to UK Property declaration page - Individual" in {
        testUnauthorisedUserRedirectsToSignIn(isAgent = false, UkProperty)
      }
      "unauthorised user navigates to UK Property declaration page - Agent" in {
        testUnauthorisedUserRedirectsToSignIn(isAgent = true, UkProperty)
      }
      "unauthorised user navigates to Foreign Property declaration page - Individual" in {
        testUnauthorisedUserRedirectsToSignIn(isAgent = false, ForeignProperty)
      }
      "unauthorised user navigates to Foreign Property declaration page - Agent" in {
        testUnauthorisedUserRedirectsToSignIn(isAgent = true, ForeignProperty)
      }
    }

    "redirect to the Cannot Go Back page" when {
      def setupCompletedCeaseJourney(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

        val result = if (isAgent) {
          TestDeclarePropertyCeasedController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
        } else {
          TestDeclarePropertyCeasedController.show(incomeSourceType)(fakeRequestWithActiveSession)
        }

        val expectedRedirectUrl = if (isAgent) {
          routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url
        } else {
          routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "UK Property journey is complete - Individual" in {
        setupCompletedCeaseJourney(isAgent = false, UkProperty)
      }
      "UK Property journey is complete - Agent" in {
        setupCompletedCeaseJourney(isAgent = true, UkProperty)
      }
      "Foreign Property journey is complete - Individual" in {
        setupCompletedCeaseJourney(isAgent = false, ForeignProperty)
      }
      "Foreign Property journey is complete - Agent" in {
        setupCompletedCeaseJourney(isAgent = true, ForeignProperty)
      }
      "Self Employment journey is complete - Individual" in {
        setupCompletedCeaseJourney(isAgent = false, SelfEmployment)
      }
      "Self Employment journey is complete - Agent" in {
        setupCompletedCeaseJourney(isAgent = true, SelfEmployment)
      }
    }

  }

  "DeclarePropertyCeasedController.submit / DeclarePropertyCeasedController.submitAgent" should {
    "return 200 OK" when {
      def testSubmitReturnsOKAndSetsMongoData(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
        setupMockAuthorisationSuccess(isAgent)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        setupMockSetSessionKeyMongo(Right(true))

        val journeyType = JourneyType(Cease, incomeSourceType)
        val redirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
          (isAgent, incomeSourceType) match {
            case (true, _) => controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, incomeSourceType).url
            case (false, _) => controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, incomeSourceType).url
          }
        val result = submitCall(isAgent, incomeSourceType)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(redirectUrl(isAgent, incomeSourceType))
        verifySetMongoKey(CeaseIncomeSourceData.ceaseIncomeSourceDeclare, "true", journeyType)
      }

      "UK Property cease declaration is completed - Individual" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = false, UkProperty)
      }
      "UK Property cease declaration is completed - Agent" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = true, UkProperty)
      }
      "Foreign Property cease declaration is completed - Individual" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = false, ForeignProperty)
      }
      "Foreign Property cease declaration is completed - Agent" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = true, ForeignProperty)
      }
    }


    "return 303 SEE_OTHER and redirect to home page" when {
      def testFeatureSwitchRedirectsToHomePage(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)

        disable(IncomeSources)
        mockPropertyIncomeSource()

        lazy val result: Future[Result] = submitCall(isAgent, incomeSourceType)
        val expectedRedirectUrl: String = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "POST call to the UK Property declaration page with FS Disabled - Individual" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = false, UkProperty)
      }
      "POST call to the UK Property declaration page with FS Disabled - Agent" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = true, UkProperty)
      }
      "POST call to the Foreign Property declaration page with FS Disabled - Individual" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = false, ForeignProperty)
      }
      "POST call to the Foreign Property declaration page with FS Disabled - Agent" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = true, ForeignProperty)
      }
    }


    "return 400 BAD_REQUEST" when {
      def testInvalidForm(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        setupMockSetSessionKeyMongo(Right(true))
        val invalidForm = Map(DeclareIncomeSourceCeasedForm.declaration -> "invalid")
        lazy val result = submitCall(isAgent, incomeSourceType, Some(invalidForm))

        status(result) shouldBe Status.BAD_REQUEST
      }

      "UK Property cease declaration is not completed - Individual" in {
        testInvalidForm(isAgent = false, UkProperty)
      }
      "UK Property cease declaration is not completed - Agent" in {
        testInvalidForm(isAgent = true, UkProperty)
      }
      "Foreign Property cease declaration is not completed - Individual" in {
        testInvalidForm(isAgent = false, ForeignProperty)
      }
      "Foreign Property cease declaration is not completed - Agent" in {
        testInvalidForm(isAgent = true, ForeignProperty)
      }
    }


    "return 500 INTERNAL_SERVER_ERROR" when {
      def testMongoException(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        enable(IncomeSources)
        mockPropertyIncomeSource()
        setupMockSetSessionKeyMongo(Left(new Exception))

        lazy val result = submitCall(isAgent, incomeSourceType)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "Exception received from Mongo while setting UK Property Declaration - Individual" in {
        testMongoException(isAgent = false, UkProperty)
      }
      "Exception received from Mongo while setting UK Property Declaration - Agent" in {
        testMongoException(isAgent = true, UkProperty)
      }
      "Exception received from Mongo while setting Foreign Property Declaration - Individual" in {
        testMongoException(isAgent = false, ForeignProperty)
      }
      "Exception received from Mongo while setting Foreign Property Declaration - Agent" in {
        testMongoException(isAgent = true, ForeignProperty)
      }
    }
  }
}
