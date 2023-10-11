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

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.cease.IncomeSourceEndDateForm
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSelfEmploymentId}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.cease.IncomeSourceEndDate

import scala.concurrent.Future

class IncomeSourceEndDateControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching with MockSessionService {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val validCeaseDate: String = "2022-08-27"

  object TestIncomeSourceEndDateController extends IncomeSourceEndDateController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[IncomeSourceEndDateForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[IncomeSourceEndDate],
    sessionService = mockSessionService)(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    def heading(incomeSourceType: IncomeSourceType): String = {
      incomeSourceType match {
        case SelfEmployment => messages("incomeSources.cease.endDate.selfEmployment.heading")
        case UkProperty => messages("incomeSources.cease.endDate.ukProperty.heading")
        case ForeignProperty => messages("incomeSources.cease.endDate.foreignProperty.heading")
      }
    }

    def title(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
      if (isAgent)
        s"${messages("htmlTitle.agent", heading(incomeSourceType))}"
      else
        s"${messages("htmlTitle", heading(incomeSourceType))}"
    }

    def getActions(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: Option[String], isChange: Boolean): (Call, Call, Call) = {
          (incomeSourceType, isAgent, isChange) match {
            case (UkProperty, true, false) =>
              (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
                routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType))
            case (UkProperty, false, false) =>
              (routes.DeclarePropertyCeasedController.show(incomeSourceType),
                routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType))
            case (UkProperty, true, true) =>
              (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
                routes.IncomeSourceEndDateController.submitChangeAgent(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType))
            case (UkProperty, false, true) =>
              (routes.DeclarePropertyCeasedController.show(incomeSourceType),
                routes.IncomeSourceEndDateController.submitChange(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType))
            case (ForeignProperty, true, false) =>
              (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
                routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType))
            case (ForeignProperty, false, false) =>
              (routes.DeclarePropertyCeasedController.show(incomeSourceType),
                routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType))
            case (ForeignProperty, true, true) =>
              (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
                routes.IncomeSourceEndDateController.submitChangeAgent(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType))
            case (ForeignProperty, false, true) =>
              (routes.DeclarePropertyCeasedController.show(incomeSourceType),
                routes.IncomeSourceEndDateController.submitChange(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType))
            case (SelfEmployment, true, false) =>
              (routes.CeaseIncomeSourceController.showAgent(),
                routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType))
            case (SelfEmployment, false, false) =>
              (routes.CeaseIncomeSourceController.show(),
                routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType))
            case (SelfEmployment, true, true) =>
              (routes.CeaseIncomeSourceController.showAgent(),
                routes.IncomeSourceEndDateController.submitChangeAgent(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType))
            case (SelfEmployment, false, true) =>
              (routes.CeaseIncomeSourceController.show(),
                routes.IncomeSourceEndDateController.submitChange(id = id, incomeSourceType = incomeSourceType),
                routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType))
          }
    }

    def testShowResponse(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Unit = {
      val result: Future[Result] = (isAgent, isChange) match {
        case (true, true) =>
          TestIncomeSourceEndDateController.showChangeAgent(id, incomeSourceType)(fakeRequestConfirmedClient())
        case (true, false) =>
          TestIncomeSourceEndDateController.showAgent(id, incomeSourceType)(fakeRequestConfirmedClient())
        case (false, true) =>
          TestIncomeSourceEndDateController.showChange(id, incomeSourceType)(fakeRequestWithActiveSession)
        case (false, false) =>
          TestIncomeSourceEndDateController.show(id, incomeSourceType)(fakeRequestWithActiveSession)
      }

      val document: Document = Jsoup.parse(contentAsString(result))
      val (backAction, postAction, _) = TestIncomeSourceEndDateController.getActions(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        id = id,
        isChange = isChange)

      status(result) shouldBe OK
      document.title shouldBe TestIncomeSourceEndDateController.title(incomeSourceType, isAgent = isAgent)
      document.select("h1").text shouldBe TestIncomeSourceEndDateController.heading(incomeSourceType)
      document.getElementById("back").attr("href") shouldBe backAction.url
      document.getElementById("income-source-end-date-form").attr("action") shouldBe postAction.url

      if (isChange) {
        document.getElementById("income-source-end-date.day").`val`() shouldBe "27"
        document.getElementById("income-source-end-date.month").`val`() shouldBe "8"
        document.getElementById("income-source-end-date.year").`val`() shouldBe "2022"

      }
    }

  }

  "Individual - IncomeSourceEndDateController.show" should {
    def stage(): Unit = {
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
    }

    "return 200 OK" when {
      val isAgent = false

      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()
        val incomeSourceType = SelfEmployment
        TestIncomeSourceEndDateController.testShowResponse(id = Some(testSelfEmploymentId), incomeSourceType, isAgent = isAgent, isChange = false)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()
        val incomeSourceType = ForeignProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = false)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()
        val incomeSourceType = UkProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = false)
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.show(Some(testSelfEmploymentId), incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.show(Some(testSelfEmploymentId), incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        stage()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.show(None, incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        stage()
        val result: Future[Result] = TestIncomeSourceEndDateController.show(None, incomeSourceType = SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
  "Individual - IncomeSourceEndDateController.submit" should {
    s"return 303 SEE_OTHER" when {
      "Self Employment - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment).url)
        verifyMockSetMongoKeyResponse(2)
      }
      "UK Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(None, UkProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty).url)
        verifyMockSetMongoKeyResponse(1)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(None, ForeignProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty).url)
        verifyMockSetMongoKeyResponse(1)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        val incomeSourceType = SelfEmployment
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(None, incomeSourceType)(fakeRequestNoSession
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(None, incomeSourceType = SelfEmployment)(fakeRequestNoSession
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Agent - IncomeSourceEndDateController.showAgent" should {
    def stage(): Unit = {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
    }

    "return 200 OK" when {
      val isAgent = true

      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()
        val incomeSourceType = SelfEmployment
        TestIncomeSourceEndDateController.testShowResponse(id = Some(testSelfEmploymentId), incomeSourceType, isAgent = isAgent, isChange = false)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()
        val incomeSourceType = ForeignProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = false)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()
        val incomeSourceType = UkProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = false)
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.show(Some(testSelfEmploymentId), incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.show(Some(testSelfEmploymentId), incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        stage()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.showAgent(None, incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        stage()
        val result: Future[Result] = TestIncomeSourceEndDateController.showAgent(None, incomeSourceType = SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
  "Agent - IncomeSourceEndDateController.submitAgent" should {
    s"return 303 SEE_OTHER" when {
      "Self Employment - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment).url)
        verifyMockSetMongoKeyResponse(2)
      }
      "UK Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(None, UkProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty).url)
        verifyMockSetMongoKeyResponse(1)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(None, ForeignProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url)
        verifyMockSetMongoKeyResponse(1)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        val incomeSourceType = SelfEmployment
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(None, incomeSourceType)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(None, incomeSourceType = SelfEmployment)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "Individual - IncomeSourceEndDateController.showChange" should {
    def stage(): Unit = {
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))
    }

    "return 200 OK" when {
      val isAgent = false

      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()
        val incomeSourceType = SelfEmployment
        TestIncomeSourceEndDateController.testShowResponse(id = Some(testSelfEmploymentId), incomeSourceType, isAgent = false, isChange = true)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()
        val incomeSourceType = ForeignProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()
        val incomeSourceType = UkProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.showChange(Some(testSelfEmploymentId), incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.showChange(Some(testSelfEmploymentId), incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        stage()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.showChange(None, incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        stage()
        val result: Future[Result] = TestIncomeSourceEndDateController.showChange(None, incomeSourceType = SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "Individual - IncomeSourceEndDateController.submitChange" should {
    s"return 303 SEE_OTHER" when {
      "Self Employment - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment).url)
        verifyMockSetMongoKeyResponse(2)
      }
      "UK Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(None, UkProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty).url)
        verifyMockSetMongoKeyResponse(1)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(None, ForeignProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty).url)
        verifyMockSetMongoKeyResponse(1)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()
        val incomeSourceType = SelfEmployment
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(None, incomeSourceType)(fakeRequestNoSession
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(None, incomeSourceType = SelfEmployment)(fakeRequestNoSession
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "Agent - IncomeSourceEndDateController.showChangeAgent" should {
    def stage(): Unit = {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))
    }

    "return 200 OK" when {
      val isAgent = true

      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()
        val incomeSourceType = SelfEmployment
        TestIncomeSourceEndDateController.testShowResponse(id = Some(testSelfEmploymentId), incomeSourceType, isAgent = isAgent, isChange = true)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()
        val incomeSourceType = ForeignProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()
        val incomeSourceType = UkProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        stage()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.showChangeAgent(None, incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](0)
      }

      "income source ID is missing" in {
        stage()
        val result: Future[Result] = TestIncomeSourceEndDateController.showChangeAgent(None, incomeSourceType = SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](0)
      }
    }
  }
  "Agent - IncomeSourceEndDateController.submitChangeAgent" should {
    s"return 303 SEE_OTHER" when {
      "Self Employment - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment).url)
        verifyMockSetMongoKeyResponse(2)
      }
      "UK Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(None, UkProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty).url)
        verifyMockSetMongoKeyResponse(2)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockCreateSession(true)
        setupMockSetSessionKeyMongo(Right(true))

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(None, ForeignProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url)
        verifyMockSetMongoKeyResponse(2)
      }
    }
    "return 400 BAD_REQUEST" when {
      "the form is not completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.BAD_REQUEST
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        val incomeSourceType = SelfEmployment
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(None, incomeSourceType)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(None, incomeSourceType = SelfEmployment)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
