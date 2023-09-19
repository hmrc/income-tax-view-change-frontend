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
import enums.IncomeSourceJourney.SelfEmployment
import forms.utils.SessionKeys
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import models.createIncomeSource.CreateIncomeSourceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{CreateBusinessDetailsService, SessionService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.CheckBusinessDetails

import java.time.LocalDate
import scala.concurrent.Future

class CheckBusinessDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching {

  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: String = LocalDate.of(2023, 1, 2).toString
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "123 Main Street"
  val testBusinessPostCode: String = "AB123CD"
  val testBusinessAccountingMethod = "Quarterly"
  val testAccountingPeriodEndDate: String = LocalDate.of(2023, 11, 11).toString

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCheckBusinessDetails: CheckBusinessDetails = app.injector.instanceOf[CheckBusinessDetails]
  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  object TestCheckBusinessDetailsController extends CheckBusinessDetailsController(
    checkBusinessDetails = app.injector.instanceOf[CheckBusinessDetails],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate,
    businessDetailsService = mockBusinessDetailsService
  )(ec, mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    sessionService = app.injector.instanceOf[SessionService],
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  ) {
    val heading: String = messages("check-business-details.heading")
    val title: String = s"${messages("htmlTitle", heading)}"
    val link: String = s"${messages("check-business-details.change-details-link")}"
  }


  "CheckBusinessDetailsController- Individual" should {
    ".show" should {
      "return 200 OK" when {
        "the session contains full business details and FS enabled" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestCheckBusinessDetailsController.show()(
            fakeRequestWithActiveSession
              .withSession(
                SessionKeys.businessName -> testBusinessStartDate,
                SessionKeys.businessStartDate -> testBusinessStartDate,
                SessionKeys.businessTrade -> testBusinessTrade,
                SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
                SessionKeys.addIncomeSourcesAccountingMethod -> testBusinessAccountingMethod,
                SessionKeys.addBusinessAccountingPeriodEndDate -> testAccountingPeriodEndDate,
                SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
              ))

          val document: Document = Jsoup.parse(contentAsString(result))
          val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")

          status(result) shouldBe OK
          document.title shouldBe TestCheckBusinessDetailsController.title
          document.select("h1:nth-child(1)").text shouldBe TestCheckBusinessDetailsController.heading
          changeDetailsLinks.first().text shouldBe TestCheckBusinessDetailsController.link
        }
      }

      "return 303 and redirect an individual back to the home page" when {
        "the IncomeSources FS is disabled" in {
          disable(IncomeSources)
          mockSingleBusinessIncomeSource()

          val result: Future[Result] = TestCheckBusinessDetailsController.show()(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }

        "called with an unauthenticated user" in {
          setupMockAuthorisationException()
          val result: Future[Result] = TestCheckBusinessDetailsController.show()(fakeRequestWithActiveSession)
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

          val result = TestCheckBusinessDetailsController.show()(
            fakeRequestWithActiveSession
              .withSession(
                SessionKeys.businessName -> testBusinessStartDate,
                SessionKeys.businessStartDate -> testBusinessStartDate,
                SessionKeys.businessTrade -> testBusinessTrade,
                SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
              ))

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".submit" should {

      "return 303" when {
        "data is correct and redirect next page" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
            .thenReturn(Future {
              Right(CreateIncomeSourceResponse("incomeSourceId"))
            })

          val result = TestCheckBusinessDetailsController.submit()(
            fakeRequestWithActiveSession
              .withSession(
                SessionKeys.businessName -> testBusinessStartDate,
                SessionKeys.businessStartDate -> testBusinessStartDate,
                SessionKeys.businessTrade -> testBusinessTrade,
                SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
                SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
                SessionKeys.addIncomeSourcesAccountingMethod -> testBusinessAccountingMethod,
                SessionKeys.addBusinessAccountingPeriodEndDate -> testAccountingPeriodEndDate,
                SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
              ))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.BusinessReportingMethodController.show("incomeSourceId").url)
        }
      }

      "redirect to custom error page when unable to create business" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse("incomeSourceId"))
          })

        val result = TestCheckBusinessDetailsController.submit()(
          fakeRequestWithActiveSession
            .withSession(
              SessionKeys.businessName -> testBusinessStartDate,
              SessionKeys.businessStartDate -> testBusinessStartDate,
              SessionKeys.businessTrade -> testBusinessTrade,
              SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
              SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
              SessionKeys.addIncomeSourcesAccountingMethod -> testBusinessAccountingMethod
            ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = SelfEmployment.key).url)
      }
    }
  }

  "CheckBusinessDetailsController - Agent" should {
    ".show" should {
      "return 200 OK" when {
        "the session contains full business details and FS enabled" in {
          disableAllSwitches()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          enable(IncomeSources)

          mockSingleBusinessIncomeSource()
          when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
            .thenReturn(Future {
              Right(CreateIncomeSourceResponse("incomeSourceId"))
            })

          val result = TestCheckBusinessDetailsController.showAgent()(
            fakeRequestConfirmedClient().withSession(
              SessionKeys.businessName -> testBusinessStartDate,
              SessionKeys.businessStartDate -> testBusinessStartDate,
              SessionKeys.businessTrade -> testBusinessTrade,
              SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
              SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
              SessionKeys.addIncomeSourcesAccountingMethod -> testBusinessAccountingMethod,
              SessionKeys.addBusinessAccountingPeriodEndDate -> testAccountingPeriodEndDate,
              SessionKeys.addBusinessPostalCode -> testBusinessPostCode
            ))

          status(result) shouldBe Status.OK

          val document: Document = Jsoup.parse(contentAsString(result))

          val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")
          changeDetailsLinks.first().text shouldBe TestCheckBusinessDetailsController.link


        }
      }
      "return 303 SEE_OTHER and redirect to home page" when {
        "navigating to the page with FS Disabled" in {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          disable(IncomeSources)
          mockSingleBusinessIncomeSource()

          val result: Future[Result] = TestCheckBusinessDetailsController.showAgent()(fakeRequestConfirmedClientwithFullBusinessDetails())

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
        }
        "called with an unauthenticated user" in {
          setupMockAgentAuthorisationException()
          val result: Future[Result] = TestCheckBusinessDetailsController.showAgent()(fakeRequestConfirmedClient())

          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }

    ".submit" should {
      "return 303 and create business " when {
        "data is correct" in {
          disableAllSwitches()
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          enable(IncomeSources)

          mockSingleBusinessIncomeSource()
          when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
            .thenReturn(Future {
              Right(CreateIncomeSourceResponse("incomeSourceId"))
            })

          val result = TestCheckBusinessDetailsController.submitAgent()(
            fakeRequestConfirmedClient().withSession(
              SessionKeys.businessName -> testBusinessStartDate,
              SessionKeys.businessStartDate -> testBusinessStartDate,
              SessionKeys.businessTrade -> testBusinessTrade,
              SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
              SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
              SessionKeys.addIncomeSourcesAccountingMethod -> testBusinessAccountingMethod,
              SessionKeys.addBusinessAccountingPeriodEndDate -> testAccountingPeriodEndDate,
              SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
            ))

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent("incomeSourceId").url)
        }
      }
      "redirect to custom error page when unable to create business" in {
        disableAllSwitches()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)

        mockSingleBusinessIncomeSource()
        when(mockBusinessDetailsService.createBusinessDetails(any())(any(), any(), any()))
          .thenReturn(Future {
            Right(CreateIncomeSourceResponse("incomeSourceId"))
          })

        val result = TestCheckBusinessDetailsController.submitAgent()(
          fakeRequestConfirmedClient().withSession(
            SessionKeys.businessName -> testBusinessStartDate,
            SessionKeys.businessStartDate -> testBusinessStartDate,
            SessionKeys.businessTrade -> testBusinessTrade,
            SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
            SessionKeys.addBusinessPostalCode -> testBusinessPostCode,
            SessionKeys.addIncomeSourcesAccountingMethod -> testBusinessAccountingMethod,
          ))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = SelfEmployment.key).url)

      }
    }
  }
}