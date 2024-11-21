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

package controllers.manageBusinesses.cease

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, IncomeSources, JourneyType}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.admin.IncomeSourcesFs
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
import views.html.manageBusinesses.cease.DeclareIncomeSourceCeased

import scala.concurrent.Future

class DeclareIncomeSourceCeasedControllerSpec extends TestSupport with MockAuthenticationPredicate with
  MockIncomeSourceDetailsPredicate with FeatureSwitching with MockSessionService {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockDeclarePropertyCeased: DeclareIncomeSourceCeased = app.injector.instanceOf[DeclareIncomeSourceCeased]

  object TestDeclareIncomeSourceCeasedController extends DeclareIncomeSourceCeasedController(
    mockAuthService,
    app.injector.instanceOf[DeclareIncomeSourceCeased],
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

    val titleSelfEmployment: String = s"${messages("htmlTitle", messages("incomeSources.cease.SE.heading"))}"
    val titleAgentSelfEmployment: String = s"${messages("htmlTitle.agent", messages("incomeSources.cease.SE.heading"))}"
    val headingSelfEmployment: String = messages("incomeSources.cease.SE.heading")
  }


  def showCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Future[Result] = {
    (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => TestDeclareIncomeSourceCeasedController.showAgent(Some("test-id"), SelfEmployment)(fakeRequestConfirmedClient())
      case (_, SelfEmployment)    => TestDeclareIncomeSourceCeasedController.show(Some("test-id"), SelfEmployment)(fakeRequestWithNinoAndOrigin("pta"))
      case (true, UkProperty)     => TestDeclareIncomeSourceCeasedController.showAgent(None, UkProperty)(fakeRequestConfirmedClient())
      case (_, UkProperty)        => TestDeclareIncomeSourceCeasedController.show(None, UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
      case (true, _)              => TestDeclareIncomeSourceCeasedController.showAgent(None, ForeignProperty)(fakeRequestConfirmedClient())
      case (_, _)                 => TestDeclareIncomeSourceCeasedController.show(None, ForeignProperty)(fakeRequestWithNinoAndOrigin("pta"))
    }
  }

  def submitCall(isAgent: Boolean, incomeSourceType: IncomeSourceType, formBody: Option[Map[String, String]] = None): Future[Result] = {
    val formData = formBody.getOrElse(Map(
      DeclareIncomeSourceCeasedForm.declaration -> "true",
      DeclareIncomeSourceCeasedForm.ceaseCsrfToken -> "12345"
    ))

    (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => TestDeclareIncomeSourceCeasedController.submitAgent(None, SelfEmployment)(fakePostRequestConfirmedClient()
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (_, SelfEmployment) => TestDeclareIncomeSourceCeasedController.submit(None, SelfEmployment)(fakePostRequestWithNinoAndOrigin("pta")
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (true, UkProperty) => TestDeclareIncomeSourceCeasedController.submitAgent(None, UkProperty)(fakePostRequestConfirmedClient()
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (_, UkProperty) => TestDeclareIncomeSourceCeasedController.submit(None, UkProperty)(fakePostRequestWithNinoAndOrigin("pta")
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (true, _) => TestDeclareIncomeSourceCeasedController.submitAgent(None, ForeignProperty)(fakePostRequestConfirmedClient()
        .withFormUrlEncodedBody(formData.toSeq: _*))
      case (_, _) => TestDeclareIncomeSourceCeasedController.submit(None, ForeignProperty)(fakePostRequestWithNinoAndOrigin("pta")
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

  "DeclareIncomeSourceCeasedController.show / DeclareIncomeSourceCeasedController.showAgent" should {
    "return 200 OK" when {
      def testViewReturnsOKWithCorrectContent(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        enable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()

        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSources(Cease, incomeSourceType)))))

        val result = showCall(isAgent, incomeSourceType)
        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) shouldBe Status.OK

        (isAgent, incomeSourceType) match {
          case (true, SelfEmployment) =>
            document.title shouldBe TestDeclareIncomeSourceCeasedController.titleAgentSelfEmployment
            document.select("legend:nth-child(1)").text shouldBe TestDeclareIncomeSourceCeasedController.headingSelfEmployment
          case (_, SelfEmployment) =>
            document.title shouldBe TestDeclareIncomeSourceCeasedController.titleSelfEmployment
            document.select("legend:nth-child(1)").text shouldBe TestDeclareIncomeSourceCeasedController.headingSelfEmployment
          case (true, UkProperty) =>
            document.title shouldBe TestDeclareIncomeSourceCeasedController.titleAgentUkProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclareIncomeSourceCeasedController.headingUkProperty
          case (_, UkProperty) =>
            document.title shouldBe TestDeclareIncomeSourceCeasedController.titleUkProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclareIncomeSourceCeasedController.headingUkProperty
          case (true, _) =>
            document.title shouldBe TestDeclareIncomeSourceCeasedController.titleAgentForeignProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclareIncomeSourceCeasedController.headingForeignProperty
          case (_, _) =>
            document.title shouldBe TestDeclareIncomeSourceCeasedController.titleForeignProperty
            document.select("legend:nth-child(1)").text shouldBe TestDeclareIncomeSourceCeasedController.headingForeignProperty
        }
      }

      "income source is Sole Trader Business and FS Enabled - Individual" in {
        testViewReturnsOKWithCorrectContent(isAgent = false, SelfEmployment)
      }
      "income source is Sole Trader Business and FS Enabled - Agent" in {
        testViewReturnsOKWithCorrectContent(isAgent = true, UkProperty)
      }
      "income source is UK Property and FS Enabled - Individual" in {
        testViewReturnsOKWithCorrectContent(isAgent = false, UkProperty)
      }
      "income source is UK Property and FS Enabled - Agent" in {
        testViewReturnsOKWithCorrectContent(isAgent = true, UkProperty)
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

        disable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()

        val result: Future[Result] = showCall(isAgent, incomeSourceType)
        status(result) shouldBe Status.SEE_OTHER

        val expectedRedirectUrl: String = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "navigating to the Sole Trader Business declaration page with FS Disabled - Individual" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = false, SelfEmployment)
      }
      "navigating to the Sole Trader Business declaration page with FS Disabled - Agent" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = true, SelfEmployment)
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

      "unauthorised user navigates to Sole Trader Business declaration page - Individual" in {
        testUnauthorisedUserRedirectsToSignIn(isAgent = false, SelfEmployment)
      }
      "unauthorised user navigates to Sole Trader Business declaration page - Agent" in {
        testUnauthorisedUserRedirectsToSignIn(isAgent = true, SelfEmployment)
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
        enable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSources(Cease, incomeSourceType)))))

        val result = if (isAgent) {
          TestDeclareIncomeSourceCeasedController.showAgent(None, incomeSourceType)(fakeRequestConfirmedClient())
        } else {
          TestDeclareIncomeSourceCeasedController.show(None, incomeSourceType)(fakeRequestWithActiveSession)
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

  "DeclareIncomeSourceCeasedController.submit / DeclareIncomeSourceCeasedController.submitAgent" should {
    "return 200 OK" when {
      def testSubmitReturnsOKAndSetsMongoData(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Unit = {
        setupMockAuthorisationSuccess(isAgent)
        enable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()
        setupMockSetSessionKeyMongo(Right(true))

        val journeyType = IncomeSources(Cease, incomeSourceType)
        val redirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
          (isAgent, incomeSourceType) match {
            case (true, _) => controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, incomeSourceType, isAgent, isChange).url
            case (false, _) => controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, incomeSourceType, isAgent, isChange).url
          }
        val result = submitCall(isAgent, incomeSourceType)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(redirectUrl(isAgent, incomeSourceType))
        verifySetMongoKey(CeaseIncomeSourceData.ceaseIncomeSourceDeclare, "true", journeyType)
      }

      "Sole Trader Business cease declaration is completed - Individual" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = false, isChange = false, SelfEmployment)
      }
      "Sole Trader Business cease declaration is completed - Agent" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = true, isChange = false, SelfEmployment)
      }
      "UK Property cease declaration is completed - Individual" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = false, isChange = false, UkProperty)
      }
      "UK Property cease declaration is completed - Agent" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = true, isChange = false, UkProperty)
      }
      "Foreign Property cease declaration is completed - Individual" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = false, isChange = false, ForeignProperty)
      }
      "Foreign Property cease declaration is completed - Agent" in {
        testSubmitReturnsOKAndSetsMongoData(isAgent = true, isChange = false, ForeignProperty)
      }
    }


    "return 303 SEE_OTHER and redirect to home page" when {
      def testFeatureSwitchRedirectsToHomePage(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)

        disable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()

        lazy val result: Future[Result] = submitCall(isAgent, incomeSourceType)
        val expectedRedirectUrl: String = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "POST call to the Sole Trader Business declaration page with FS Disabled - Individual" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = false, SelfEmployment)
      }
      "POST call to the Sole Trader Business declaration page with FS Disabled - Agent" in {
        testFeatureSwitchRedirectsToHomePage(isAgent = true, SelfEmployment)
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
        enable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()
        setupMockSetSessionKeyMongo(Right(true))
        val invalidForm = Map(DeclareIncomeSourceCeasedForm.declaration -> "invalid")
        lazy val result = submitCall(isAgent, incomeSourceType, Some(invalidForm))

        status(result) shouldBe Status.BAD_REQUEST
      }

      "Sole Trader Business cease declaration is not completed - Individual" in {
        testInvalidForm(isAgent = false, SelfEmployment)
      }
      "Sole Trader Business cease declaration is not completed - Agent" in {
        testInvalidForm(isAgent = true, SelfEmployment)
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
        enable(IncomeSourcesFs)
        mockBothPropertyBothBusiness()
        setupMockSetSessionKeyMongo(Left(new Exception))

        lazy val result = submitCall(isAgent, incomeSourceType)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "Exception received from Mongo while setting Sole Trader Business Declaration - Individual" in {
        testMongoException(isAgent = false, SelfEmployment)
      }
      "Exception received from Mongo while setting Sole Trader Business Declaration - Agent" in {
        testMongoException(isAgent = true, SelfEmployment)
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
