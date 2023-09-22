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

package controllers.incomeSources.add

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.UkProperty
import forms.utils.SessionKeys
import forms.utils.SessionKeys.{addIncomeSourcesAccountingMethod, addUkPropertyStartDate}
import implicits.ImplicitDateFormatter
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.createIncomeSource.CreateIncomeSourceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{CreateBusinessDetailsService, SessionService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.{CheckBusinessDetails, CheckUKPropertyDetails}

import java.time.LocalDate
import scala.concurrent.Future

class CheckUKPropertyDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching with ImplicitDateFormatter with IncomeSourcesUtils {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCheckBusinessDetails: CheckBusinessDetails = app.injector.instanceOf[CheckBusinessDetails]
  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  val date = "2023-05-01"
  val propertyStartDate: LocalDate = LocalDate.parse(date)
  val testUKPropertyStartDate: String = LocalDate.of(2023, 1, 2).toString
  val cash: String = messages("incomeSources.add.accountingMethod.cash")


  lazy val errorUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = UkProperty.key).url
  lazy val agentErrorUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = UkProperty.key).url

  object TestCheckUKPropertyDetailsController extends CheckUKPropertyDetailsController(
    checkUKPropertyDetails = app.injector.instanceOf[CheckUKPropertyDetails],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    businessDetailsService = mockBusinessDetailsService,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    createBusinessDetailsService = mockBusinessDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    sessionService = app.injector.instanceOf[SessionService],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    languageUtils) {
    val heading: String = messages("incomeSources.add.checkUKPropertyDetails.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val agentsTitle: String = s"${messages("htmlTitle.agent", heading)}"
    val link: String = s"${messages("check-business-details.change-details-link")}"
  }


  "CheckUKPropertyDetailsController - Individual" should {

    "return 200 OK" when {
      "the session contains full details and FS enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestCheckUKPropertyDetailsController.show()(
          fakeRequestWithActiveSession
            .withSession(
              addUkPropertyStartDate -> date,
              addIncomeSourcesAccountingMethod -> "CASH"
            ))

        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestCheckUKPropertyDetailsController.title
        document.select("h1:nth-child(1)").text shouldBe TestCheckUKPropertyDetailsController.heading
        document.select("dl:nth-of-type(1) > div > dd.govuk-summary-list__value").text() shouldBe propertyStartDate.toLongDate
        document.select("dl:nth-of-type(2) > div > dd.govuk-summary-list__value").text() shouldBe cash

      }
    }

    "return 303" when {
      "data is submitted and redirect next page" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val incomeSourceId = "incomeSourceId"
        when(mockBusinessDetailsService.createUKProperty(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse(incomeSourceId))
          })

        val result = TestCheckUKPropertyDetailsController.submit()(
          fakeRequestWithActiveSession
            .withSession(
              addUkPropertyStartDate -> date,
              addIncomeSourcesAccountingMethod -> "CASH"
            ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result).get shouldBe routes.UKPropertyReportingMethodController.show().url

      }
    }

    "redirect to custom error page on submit" when {
      "UK property model can't be created on submit" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(noIncomeDetails)

        when(mockBusinessDetailsService.createUKProperty(any())(any(), any(), any())).
          thenReturn(Future(Left(new Error(s"Failed to created incomeSources"))))
        val result = TestCheckUKPropertyDetailsController.submit()(
          fakeRequestWithActiveSession
            .withSession(
              SessionKeys.addUkPropertyStartDate -> testUKPropertyStartDate,
              SessionKeys.addIncomeSourcesAccountingMethod -> cash
            )
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(errorUrl)
      }
    }

    "return 303 and redirect an individual back to the home page" when {
      "the IncomeSources FS is disabled" in {
        disable(IncomeSources)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestCheckUKPropertyDetailsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }

      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCheckUKPropertyDetailsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }


    "return 500 INTERNAL_SERVER_ERROR" when {
      "there is session data missing" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse("incomeSourceId"))
          })

        val result = TestCheckUKPropertyDetailsController.show()(
          fakeRequestWithActiveSession
            .withSession())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "CheckUKPropertyDetailsController - Agents" should {

    "return 200 OK" when {
      "the session contains full details and FS enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestCheckUKPropertyDetailsController.showAgent()(
          fakeRequestConfirmedClient()
            .withSession(
              addUkPropertyStartDate -> date,
              addIncomeSourcesAccountingMethod -> "CASH"
            ))

        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.title shouldBe TestCheckUKPropertyDetailsController.agentsTitle
        document.select("h1:nth-child(1)").text shouldBe TestCheckUKPropertyDetailsController.heading
        document.select("dl:nth-of-type(1) > div > dd.govuk-summary-list__value").text() shouldBe propertyStartDate.toLongDate
        document.select("dl:nth-of-type(2) > div > dd.govuk-summary-list__value").text() shouldBe cash
      }
    }

    "return 303" when {
      "data is submitted and redirect next page" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        val incomeSourceId = "incomeSourceId"
        when(mockBusinessDetailsService.createUKProperty(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse(incomeSourceId))
          })

        val result = TestCheckUKPropertyDetailsController.submitAgent()(
          fakeRequestConfirmedClient()
            .withSession(
              addUkPropertyStartDate -> date,
              addIncomeSourcesAccountingMethod -> "CASH"
            ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result).get shouldBe routes.UKPropertyReportingMethodController.showAgent().url

      }
    }

    "redirect to custom error page on submit" when {
      "foreign property model successfully created and user clicks continue button (agent)" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        val incomeSourceId = "incomeSourceId"
        when(mockBusinessDetailsService.createUKProperty(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse(incomeSourceId))
          })

        val result = TestCheckUKPropertyDetailsController.submitAgent()(
          fakeRequestConfirmedClient()
            .withSession(
              addUkPropertyStartDate -> testUKPropertyStartDate
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(agentErrorUrl)
      }
    }

    "return 303 and redirect an agent back to the home page" when {
      "the IncomeSources FS is disabled" in {
        disable(IncomeSources)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = TestCheckUKPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }

      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestCheckUKPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }


    "return 500 INTERNAL_SERVER_ERROR" when {
      "there is session data missing" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse("incomeSourceId"))
          })

        val result = TestCheckUKPropertyDetailsController.showAgent()(
          fakeRequestConfirmedClient()
            .withSession())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
