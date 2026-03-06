/*
 * Copyright 2026 HM Revenue & Customs
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

package views

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.creditsandrefunds.CreditsModel
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.newHomePage.{HandleYourTasksViewModel, SubmissionDeadlinesViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.HandleYourTasksView

import java.time.LocalDate

class NewHomeYourTasksViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val newHomeYourTasksView: HandleYourTasksView = app.injector.instanceOf[HandleYourTasksView]
  val testMtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), businessAndPropertyAligned)

  val dateServiceCurrentDate: LocalDate = dateService.getCurrentDate

  class TestSetup(
                   origin: Option[String] = None,
                   isAgent: Boolean = false,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   welshLang: Boolean = false,
                   dueDates: Seq[LocalDate] = Seq(dateServiceCurrentDate.plusDays(31)),
                   nextTaxReturnDueDate: Option[LocalDate] = Some(dateServiceCurrentDate.plusDays(31)),
                   nextQuarterlyUpdatesDueDate: Option[LocalDate] = None,
                   currentYearITSAStatus: ITSAStatus = ITSAStatus.Annual) {

    val testMessages: Messages = if (welshLang) {
      app.injector.instanceOf[MessagesApi].preferred(FakeRequest().withHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy"))
    } else {
      messages
    }

    val testUrl = "testUrl"

    val yourTasksHeading = "Your tasks"
    val viewUpdatesAndDeadlinesTitle = "View submission deadlines"
    val upcomingAnnualSubmissionDeadlineBody = "You have an upcoming annual submission deadline."
    val submissionDeadlinesURL = "/report-quarterly/income-and-expenses/view/submission-deadlines"

    lazy val page: HtmlFormat.Appendable =
      newHomeYourTasksView(
        origin = origin,
        viewModel = getNextUpdatesTileViewModel(dateServiceCurrentDate, dueDates, nextTaxReturnDueDate, currentYearITSAStatus, nextQuarterlyUpdatesDueDate),
        isAgent = isAgent,
        yourTasksUrl = yourTasksUrl,
        recentActivityUrl = recentActivityUrl,
        overViewUrl = overViewUrl,
        helpUrl = helpUrl)(testMessages, FakeRequest(), testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  //TODO proceed with this test until done
  "New Home Your Tasks page for individuals" when {
    "upcoming annual submission due more than 30 days" should {
      "display the correct content" in new TestSetup() {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--green") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 15 Jan 2024"
      }
    }

    "upcoming annual submission due exactly in 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 14 Jan 2024"
      }
    }

    "upcoming annual submission due in 1 day" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(1))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 16 Dec 2023"
      }
    }

    "upcoming annual submission due less then 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(29))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 13 Jan 2024"
      }
    }

    "upcoming annual submission due today" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate)) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--pink") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 15 Dec 2023"
      }
    }

    "upcoming annual submission overdue" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.minusDays(1))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 14 Dec 2023"
      }
    }

    "upcoming annual submission coming due less or equal that 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.getElementById("date-tag").text() shouldBe "Due 14 Jan 2024"
      }
    }
  }

  //TODO proceed with this test until done
  "New Home Overview page for agents" should {
    "display the the correct content" in new TestSetup(isAgent = true) {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
      document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
      document.select(".hmrc-card__heading").get(1).hasCorrectHref("/report-quarterly/income-and-expenses/view/agents/submission-deadlines")
      //TODO check if wording for Agents is matching
      document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
      document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--green") shouldBe true
      document.getElementById("date-tag").text() shouldBe "Due 15 Jan 2024"
    }
  }

  "New Home Your Tasks page for individuals" when {
    "single overdue annual submission" should {
      "display the correct content" in new TestSetup(dueDates = Seq(dateServiceCurrentDate.minusDays(30)), nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe "You have an overdue annual submission."
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.getElementById("date-tag").text() shouldBe s"Was due ${dateServiceCurrentDate.minusDays(30).toLongDateShort}"
      }
    }

    "multiple overdue annual submissions" should {
      "display the correct content" in new TestSetup(dueDates = Seq(dateServiceCurrentDate.minusDays(90), dateServiceCurrentDate.minusDays(120), dateServiceCurrentDate.minusDays(20)), nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe "You have 3 overdue annual submissions."
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.getElementById("date-tag").text() shouldBe s"Oldest submission due ${dateServiceCurrentDate.minusDays(120).toLongDateShort}"
      }
    }

    "single overdue quarterly submission" should {
      "display the correct content" in new TestSetup(dueDates = Seq(dateServiceCurrentDate.minusDays(30)), nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30)), currentYearITSAStatus = ITSAStatus.Voluntary) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe "You have an overdue quarterly submission."
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.getElementById("date-tag").text() shouldBe s"Was due ${dateServiceCurrentDate.minusDays(30).toLongDateShort}"
      }
    }

    "multiple overdue quarterly submissions" should {
      "display the correct content" in new TestSetup(dueDates = Seq(dateServiceCurrentDate.minusDays(90), dateServiceCurrentDate.minusDays(120), dateServiceCurrentDate.minusDays(20)), nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30)), currentYearITSAStatus = ITSAStatus.Voluntary) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe "You have 3 overdue quarterly submissions."
        document.getElementById("date-tag").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.getElementById("date-tag").text() shouldBe s"Oldest submission due ${dateServiceCurrentDate.minusDays(120).toLongDateShort}"
      }
    }
  }

  private def getNextUpdatesTileViewModel(currentDate: LocalDate, dueDates: Seq[LocalDate], nextTaxReturnDueDate: Option[LocalDate], currentYearITSAStatus: ITSAStatus, nextQuarterlyUpdatesDueDate: Option[LocalDate]): HandleYourTasksViewModel = {
    val zeroCredits = CreditsModel(
      availableCreditForRepayment = BigDecimal(0.00),
      allocatedCreditForFutureCharges = BigDecimal(0.00),
      unallocatedCredit = BigDecimal(0.00),
      totalCredit = BigDecimal(0.00),
      firstPendingAmountRequested = None,
      secondPendingAmountRequested = None,
      transactions = List.empty
    )

    val updatesAndDeadlinesViewModel = SubmissionDeadlinesViewModel(
      dueDates,
      currentDate,
      currentYearITSAStatus,
      nextQuarterlyUpdatesDueDate,
      nextTaxReturnDueDate,
    )

    HandleYourTasksViewModel(
      outstandingChargesModel = List.empty,
      unpaidCharges = List.empty,
      credits = zeroCredits,
      creditsRefundsRepayEnabled = false,
      obligations = updatesAndDeadlinesViewModel
    )
  }
}
