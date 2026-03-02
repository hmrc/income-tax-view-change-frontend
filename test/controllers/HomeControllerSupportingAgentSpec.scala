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

import audit.AuditingService
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.MTDSupportingAgent
import models.admin.{OptInOptOutContentUpdateR17, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.*
import play.api.test.Injecting
import play.twirl.api.Html
import services.{CreditService, NextUpdatesService}
import services.optIn.OptInService
import services.optout.OptOutService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import views.html.{HomeView, NewHomeHelpView, NewHomeOverviewView, NewHomeRecentActivityView}
import views.html.agent.{PrimaryAgentHomeView, SupportingAgentHomeView}
import views.html.helpers.injected.home.YourReportingObligationsTile

import java.time.LocalDate
import scala.concurrent.Future

class HomeControllerSupportingAgentSpec extends HomeControllerHelperSpec with Injecting {

  given mockedYourReportingObligationsTile: YourReportingObligationsTile = mock(classOf[YourReportingObligationsTile])

  val application: Application = applicationBuilderWithAuthBindings.overrides(
    api.inject.bind[YourReportingObligationsTile].toInstance(mockedYourReportingObligationsTile),
  ).build()

  val homeView: HomeView = application.injector.instanceOf(classOf[HomeView])
  val primaryAgentHomeView: PrimaryAgentHomeView = application.injector.instanceOf(classOf[PrimaryAgentHomeView])
  val supportingAgentHomeView: SupportingAgentHomeView = application.injector.instanceOf(classOf[SupportingAgentHomeView])
  val recentActivityView: NewHomeRecentActivityView = application.injector.instanceOf(classOf[NewHomeRecentActivityView])
  val overviewView: NewHomeOverviewView = application.injector.instanceOf(classOf[NewHomeOverviewView])
  val helpView: NewHomeHelpView = application.injector.instanceOf(classOf[NewHomeHelpView])
  val authActions: AuthActions = application.injector.instanceOf(classOf[AuthActions])
  val auditingService: AuditingService = application.injector.instanceOf(classOf[AuditingService])

  given mockedCreditService: CreditService = mock(classOf[CreditService])
  given mockedOptInService: OptInService = mock(classOf[OptInService])
  given mockedOptOutService: OptOutService = mock(classOf[OptOutService])
  given mockedNextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  given ItvcErrorHandler = mock(classOf[ItvcErrorHandler])
  given AgentItvcErrorHandler = mock(classOf[AgentItvcErrorHandler])
  given MessagesControllerComponents = app.injector.instanceOf(classOf[MessagesControllerComponents])

  trait Setup {
    val controller: HomeController = HomeController(
      homeView,
      recentActivityView,
      overviewView,
      helpView,
      primaryAgentHomeView,
      supportingAgentHomeView,
      authActions,
      mockedNextUpdatesService,
      mockIncomeSourceDetailsService,
      mockFinancialDetailsService,
      mockDateServiceInjected,
      mockWhatYouOweService,
      mockITSAStatusService,
      mockPenaltyDetailsService,
      mockedCreditService,
      mockedOptInService,
      mockedOptOutService,
      auditingService
    )

    when(mockDateServiceInjected.getCurrentDate) thenReturn fixedDate
    when(mockDateServiceInjected.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1

    lazy val homePageTitle = s"${messages("htmlTitle.agent", messages("home.agent.heading"))}"
    lazy val homePageCaption = "You are signed in as a supporting agent"
    lazy val homePageHeading = s"${messages("home.agent.headingWithClientName", "Test User")}"

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  "show()" when {
    val agentType = MTDSupportingAgent
    val isSupportingAgent = true
    val fakeRequest = fakeRequestConfirmedClient(isSupportingAgent = true)
    s"the user is authenticated $agentType" should {
      "render the home page controller with the next updates tile" when {
        "there is a future update date to display" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockItsaStatusRetrievalAction()
          mockGetDueDates(Right(futureDueDates))
          mockSingleBusinessIncomeSource()
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          when(mockedOptInService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
            .thenReturn(Future.successful(true))
          when(mockedOptOutService.updateJourneyStatusInSessionData(any())(any(), any()))
            .thenReturn(Future.successful(true))
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("h1").text() shouldBe homePageHeading
          document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
          document.select("#updates-tile p:nth-child(2)").text() shouldBe "1 January 2100"
        }

        "there is an overdue update date to display" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockItsaStatusRetrievalAction()
          mockGetDueDates(Right(overdueDueDates))
          mockSingleBusinessIncomeSource()
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile p:nth-child(2)").text() shouldBe "Overdue 1 January 2018"
        }

        "there are no updates to display" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockItsaStatusRetrievalAction()
          mockGetDueDates(Right(Seq()))
          mockSingleBusinessIncomeSource()
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile").text() shouldBe "Your submission deadlines View update deadlines"
        }
      }

      "render the home without the Next Updates tile" when {
        "the user has no updates due" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockItsaStatusRetrievalAction()
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Right(Seq()))
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

          val result: Future[Result] = controller.showAgent()(fakeRequest)
          status(result) shouldBe Status.OK

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile").text shouldBe "Your submission deadlines View update deadlines"
        }
      }

      "render the home page with the next updates tile and OptInOptOutContentUpdateR17 enabled for quarterly user (voluntary)" in new Setup {
        enable(OptInOptOutContentUpdateR17)
        setupMockAgentWithClientAuth(isSupportingAgent)
        val currentTaxYear: TaxYear = TaxYear(fixedDate.getYear, fixedDate.getYear + 1)
        val nextQuarterlyUpdateDate: LocalDate = LocalDate.of(2024, 2, 5)
        val nextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYear.endYear + 1, 1, 31)

        setupMockGetStatusTillAvailableFutureYears(currentTaxYear.previousYear)(
          Future.successful(Map(currentTaxYear -> baseStatusDetail.copy(status = ITSAStatus.Voluntary)))
        )

        setupNextUpdatesTests(allDueDates = Seq(nextQuarterlyUpdateDate),
          nextQuarterlyUpdateDueDate = Some(nextQuarterlyUpdateDate),
          nextTaxReturnDueDate = Some(nextTaxReturnDueDate),
          mtdUserRole = agentType)

        val result: Future[Result] = controller.showAgent()(fakeRequest)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))

        val tile: Elements = document.select("#updates-tile")
        tile.select("h2").text shouldBe "Your submission deadlines"
        tile.select("p").get(0).text shouldBe "Next update due: 5 February 2024"
        tile.select("p").get(1).text shouldBe "Next tax return due: 31 January 2025"

        val link: Elements = tile.select("a")
        link.text.trim shouldBe "View your deadlines"
        link.attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent().url
      }

      "render the homepage with the next updates tile and OptInOptOutContentUpdateR17 enabled for quarterly user (mandated) with overdue updates" in new Setup {
        enable(OptInOptOutContentUpdateR17)
        setupMockAgentWithClientAuth(isSupportingAgent)

        val currentTaxYear: TaxYear = TaxYear(fixedDate.getYear, fixedDate.getYear + 1)
        val overdue1: LocalDate = LocalDate.of(2000, 1, 1)
        val overdue2: LocalDate = LocalDate.of(2000, 2, 1)
        val nextQuarterlyUpdateDate: LocalDate = LocalDate.of(2024, 2, 5)
        val nextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYear.endYear + 1, 1, 31)

        setupNextUpdatesTests(allDueDates = Seq(overdue1, overdue2, nextQuarterlyUpdateDate),
          nextQuarterlyUpdateDueDate = Some(nextQuarterlyUpdateDate),
          nextTaxReturnDueDate = Some(nextTaxReturnDueDate),
          mtdUserRole = agentType)

        setupMockGetStatusTillAvailableFutureYears(currentTaxYear.previousYear)(
          Future.successful(Map(currentTaxYear -> baseStatusDetail.copy(status = ITSAStatus.Mandated)))
        )

        val result: Future[Result] = controller.showAgent()(fakeRequest)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))

        val tile: Elements = document.select("#updates-tile")
        tile.select("h2").text shouldBe "Your submission deadlines"
        tile.select("p").get(0).select("span.govuk-tag").text should include("2 Overdue updates")
        tile.select("p").get(1).text shouldBe "Next update due: 5 February 2024"
        tile.select("p").get(2).text shouldBe "Next tax return due: 31 January 2025"

        val link: Elements = tile.select("a")
        link.text.trim shouldBe "View your deadlines"
        link.attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent().url
      }

      "render the home page with the next updates tile and OptInOptOutContentUpdateR17 enabled for annual user" in new Setup {
        enable(OptInOptOutContentUpdateR17)
        setupMockAgentWithClientAuth(isSupportingAgent)

        val currentTaxYear = TaxYear(fixedDate.getYear, fixedDate.getYear + 1)
        val nextTaxReturnDueDate: LocalDate = LocalDate.of(currentTaxYear.endYear + 1, 1, 31)

        setupNextUpdatesTests(allDueDates = futureDueDates,
          nextQuarterlyUpdateDueDate = None,
          nextTaxReturnDueDate = Some(nextTaxReturnDueDate),
          mtdUserRole = agentType)

        setupMockGetStatusTillAvailableFutureYears(currentTaxYear.previousYear)(
          Future.successful(Map(currentTaxYear -> baseStatusDetail.copy(status = ITSAStatus.Annual)))
        )

        val result: Future[Result] = controller.showAgent()(fakeRequest)
        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))

        val tile: Elements = document.select("#updates-tile")
        tile.select("h2").text shouldBe "Your submission deadlines"
        tile.text should not include "Next update due"
        tile.select("p").get(0).text shouldBe "Next tax return due: 31 January 2025"

        val link: Elements = tile.select("a")
        link.text.trim shouldBe "View your deadlines"
        link.attr("href") shouldBe controllers.routes.NextUpdatesController.showAgent().url
      }

      "render the home page with the Your Businesses tile with link" when {
        "using the manage businesses journey" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockGetDueDates(Right(futureDueDates))
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

          val result: Future[Result] = controller.showAgent()(fakeRequest)
          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.newJourneyHeading")
          document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.newJourney.view")
          document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
        }
      }

      "render the home page with a Reporting Obligations tile" that {
        "states that the user is reporting annually" when {
          "Reporting Frequency FS is enabled and the current ITSA status is annually" in new Setup {
            enable(ReportingFrequencyPage)
            setupMockAgentWithClientAuth(isSupportingAgent)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            mockGetDueDates(Right(Seq.empty))
            mockSingleBusinessIncomeSource()
            when(mockedYourReportingObligationsTile.apply(any(), any())(any()))
              .thenReturn(Html(""))

            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#reporting-obligations-tile p:nth-child(2)").text() shouldBe ""
          }
        }
        "states that the user is reporting quarterly" when {
          "Reporting Frequency FS is enabled and the current ITSA status is voluntary" in new Setup {
            enable(ReportingFrequencyPage)
            setupMockAgentWithClientAuth(isSupportingAgent)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail.copy(status = ITSAStatus.Voluntary))))

            mockGetDueDates(Right(Seq.empty))
            mockSingleBusinessIncomeSource()

            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#reporting-obligations-tile p:nth-child(2)").text() shouldBe ""
          }

          "Reporting Frequency FS is enabled and the current ITSA status is mandated" in new Setup {
            enable(ReportingFrequencyPage)
            setupMockAgentWithClientAuth(isSupportingAgent)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail.copy(status = ITSAStatus.Mandated))))
            mockGetDueDates(Right(Seq.empty))
            mockSingleBusinessIncomeSource()

            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#reporting-obligations-tile p:nth-child(2)").text() shouldBe ""
          }
        }
      }

      "render the home page without a Next Payments due tile" in new Setup {
        setupMockAgentWithClientAuth(true)
        mockItsaStatusRetrievalAction()
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("payments-tile") shouldBe null
      }

      "render the home page without a Payment history tile" in new Setup {
        setupMockAgentWithClientAuth(true)
        mockItsaStatusRetrievalAction()
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("payment-history-tile") shouldBe null
      }

      "render the home page without a returns tile" in new Setup {
        setupMockAgentWithClientAuth(true)
        mockItsaStatusRetrievalAction()
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("returns-tile") shouldBe null
      }

      "render the home page without a reporting obligations tile" in new Setup {
        disable(ReportingFrequencyPage)
        setupMockAgentWithClientAuth(true)
        mockItsaStatusRetrievalAction()
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("reporting-obligations-tile") shouldBe null
      }
    }
  }
  new Setup {
    "test MTD Supporting Agent Auth Failures" should {
      testMTDAgentAuthFailures(controller.showAgent(), isSupportingAgent = true)
    }
  }
}
