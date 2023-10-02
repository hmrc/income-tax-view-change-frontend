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
import forms.utils.SessionKeys
import forms.utils.SessionKeys.ceaseBusinessIncomeSourceId
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.Results.Redirect
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

    def getActions(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: Option[String], isChange: Boolean): (Call, Call, Call, IncomeSourceType) = {
      (incomeSourceType, isAgent, isChange) match {
        case (UkProperty, true, false) =>
          (routes.CeaseUKPropertyController.showAgent(),
            routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = UkProperty),
            routes.CheckCeaseUKPropertyDetailsController.showAgent(),
            UkProperty)
        case (UkProperty, false, false) =>
          (routes.CeaseUKPropertyController.show(),
            routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = UkProperty),
            routes.CheckCeaseUKPropertyDetailsController.show(),
            UkProperty)
        case (UkProperty, true, true) =>
          (routes.CeaseUKPropertyController.showAgent(),
            routes.IncomeSourceEndDateController.submitChangeAgent(id = id, incomeSourceType = UkProperty),
            routes.CheckCeaseUKPropertyDetailsController.showAgent(),
            UkProperty)
        case (UkProperty, false, true) =>
          (routes.CeaseUKPropertyController.show(),
            routes.IncomeSourceEndDateController.submitChange(id = id, incomeSourceType = UkProperty),
            routes.CheckCeaseUKPropertyDetailsController.show(),
            UkProperty)
        case (ForeignProperty, true, false) =>
          (routes.CeaseForeignPropertyController.showAgent(),
            routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = ForeignProperty),
            routes.CheckCeaseForeignPropertyDetailsController.showAgent(),
            ForeignProperty)
        case (ForeignProperty, false, false) =>
          (routes.CeaseForeignPropertyController.show(),
            routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = ForeignProperty),
            routes.CheckCeaseForeignPropertyDetailsController.show(),
            ForeignProperty)
        case (ForeignProperty, true, true) =>
          (routes.CeaseForeignPropertyController.showAgent(),
            routes.IncomeSourceEndDateController.submitChangeAgent(id = id, incomeSourceType = ForeignProperty),
            routes.CheckCeaseForeignPropertyDetailsController.showAgent(),
            ForeignProperty)
        case (ForeignProperty, false, true) =>
          (routes.CeaseForeignPropertyController.show(),
            routes.IncomeSourceEndDateController.submitChange(id = id, incomeSourceType = ForeignProperty),
            routes.CheckCeaseForeignPropertyDetailsController.show(),
            ForeignProperty)
        case (SelfEmployment, true, false) =>
          (routes.CeaseIncomeSourceController.showAgent(),
            routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = SelfEmployment),
            routes.CheckCeaseBusinessDetailsController.showAgent(),
            SelfEmployment)
        case (SelfEmployment, false, false) =>
          (routes.CeaseIncomeSourceController.show(),
            routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = SelfEmployment),
            routes.CheckCeaseBusinessDetailsController.show(),
            SelfEmployment)
        case (SelfEmployment, true, true) =>
          (routes.CeaseIncomeSourceController.showAgent(),
            routes.IncomeSourceEndDateController.submitChangeAgent(id = id, incomeSourceType = SelfEmployment),
            routes.CheckCeaseBusinessDetailsController.showAgent(),
            SelfEmployment)
        case (SelfEmployment, false, true) =>
          (routes.CeaseIncomeSourceController.show(),
            routes.IncomeSourceEndDateController.submitChange(id = id, incomeSourceType = SelfEmployment),
            routes.CheckCeaseBusinessDetailsController.show(),
            SelfEmployment)
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
      val (backAction, postAction, _, _) = TestIncomeSourceEndDateController.getActions(
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
        setupMockGetSession(Some(testSelfEmploymentId))

        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url)
        setupMockSetSession(redirect, SessionKeys.ceaseBusinessEndDate -> validCeaseDate, SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe Some("2022-08-27")
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe Some(testSelfEmploymentId)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url)
      }
      "UK Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url)
        setupMockSetSession(redirect, SessionKeys.ceaseUKPropertyEndDate -> validCeaseDate)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(None, UkProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(UkProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url)
        setupMockSetSession(redirect, SessionKeys.ceaseForeignPropertyEndDate -> validCeaseDate)


        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submit(None, ForeignProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ForeignProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url)
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
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe None
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe None
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
        setupMockGetSession(Some(testSelfEmploymentId))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent().url)
        setupMockSetSession(redirect, SessionKeys.ceaseBusinessEndDate -> validCeaseDate, SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe Some("2022-08-27")
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe Some(testSelfEmploymentId)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent().url)
      }
      "UK Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)
        setupMockSetSession(redirect, SessionKeys.ceaseUKPropertyEndDate -> validCeaseDate)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(None, UkProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(UkProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url)
        setupMockSetSession(redirect, SessionKeys.ceaseForeignPropertyEndDate -> validCeaseDate)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitAgent(None, ForeignProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ForeignProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url)
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
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe None
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe None
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
      setupMockGetSession(Some("2022-08-27"))

    }

    "return 200 OK" when {
      val isAgent = false

      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()
        val incomeSourceType = SelfEmployment
        TestIncomeSourceEndDateController.testShowResponse(id = Some(testSelfEmploymentId), incomeSourceType, isAgent = false, isChange = true)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()
        val incomeSourceType = ForeignProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()
        val incomeSourceType = UkProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
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
        setupMockGetSession(Some(testSelfEmploymentId))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url)
        setupMockSetSession(redirect, SessionKeys.ceaseBusinessEndDate -> validCeaseDate, SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe Some("2022-08-27")
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe Some(testSelfEmploymentId)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url)
      }
      "UK Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url)
        setupMockSetSession(redirect, SessionKeys.ceaseUKPropertyEndDate -> validCeaseDate)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(None, UkProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(UkProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url)
        setupMockSetSession(redirect, SessionKeys.ceaseForeignPropertyEndDate -> validCeaseDate)


        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChange(None, ForeignProperty)(fakeRequestNoSession.withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ForeignProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url)
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
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe None
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe None
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
      setupMockGetSession(Some("2022-08-27"))

    }

    "return 200 OK" when {
      val isAgent = true

      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()
        val incomeSourceType = SelfEmployment
        TestIncomeSourceEndDateController.testShowResponse(id = Some(testSelfEmploymentId), incomeSourceType, isAgent = isAgent, isChange = true)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()
        val incomeSourceType = ForeignProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()
        val incomeSourceType = UkProperty
        TestIncomeSourceEndDateController.testShowResponse(id = None, incomeSourceType, isAgent = isAgent, isChange = true)
      }
    }
    "return 500 INTERNAL SERVER ERROR to internal server page" when {
      "income source ID is missing and income source type is Self Employment " in {
        stage()
        val incomeSourceType = SelfEmployment
        val result: Future[Result] = TestIncomeSourceEndDateController.showChangeAgent(None, incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "income source ID is missing" in {
        stage()
        val result: Future[Result] = TestIncomeSourceEndDateController.showChangeAgent(None, incomeSourceType = SelfEmployment)(fakeRequestConfirmedClient())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "Agent - IncomeSourceEndDateController.submitChangeAgent" should {
    s"return 303 SEE_OTHER" when {
      "Self Employment - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()
        setupMockGetSession(Some(testSelfEmploymentId))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent().url)
        setupMockSetSession(redirect, SessionKeys.ceaseBusinessEndDate -> validCeaseDate, SessionKeys.ceaseBusinessIncomeSourceId -> testSelfEmploymentId)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(Some(testSelfEmploymentId), SelfEmployment)(fakeRequestConfirmedClient().withMethod("POST")
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe Some("2022-08-27")
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe Some(testSelfEmploymentId)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent().url)
      }
      "UK Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)
        setupMockSetSession(redirect, SessionKeys.ceaseUKPropertyEndDate -> validCeaseDate)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(None, UkProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(UkProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)
      }
      "Foreign Property - form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockGetSession(Some("value"))
        val redirect = Redirect(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url)
        setupMockSetSession(redirect, SessionKeys.ceaseForeignPropertyEndDate -> validCeaseDate)

        lazy val result: Future[Result] = {
          TestIncomeSourceEndDateController.submitChangeAgent(None, ForeignProperty)(fakeRequestConfirmedClient()
            .withFormUrlEncodedBody("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8",
              "income-source-end-date.year" -> "2022"))
        }

        status(result) shouldBe Status.SEE_OTHER
        result.futureValue.session.get(ForeignProperty.endDateSessionKey) shouldBe Some("2022-08-27")
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url)
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
        result.futureValue.session.get(SelfEmployment.endDateSessionKey) shouldBe None
        result.futureValue.session.get(ceaseBusinessIncomeSourceId) shouldBe None
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
