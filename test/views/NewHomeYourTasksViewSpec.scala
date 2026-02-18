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
import models.newHomePage.{HandleYourTasksViewModel, SubmissionDeadlinesViewModel}
import models.obligations.{SingleObligationModel, StatusOpen}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
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

  private val annualObligationType: String = "Crystallisation"
  private val quarterlyObligationType: String = "Quarterly"

  val multipleAnnualOverdueObligations: Seq[SingleObligationModel] = getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(90)) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(120)) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(20))

  val multipleQuarterlyOverdueObligations: Seq[SingleObligationModel] = getSingleObligationModels(dateServiceCurrentDate.minusDays(90), quarterlyObligationType) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(120), quarterlyObligationType) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(20), quarterlyObligationType)

  val multipleQuarterlyAndAnnualOverdueObligations: Seq[SingleObligationModel] = getSingleObligationModels(dateServiceCurrentDate.minusDays(85), annualObligationType) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(120), quarterlyObligationType) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(20), quarterlyObligationType) ++
    getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(100), annualObligationType)
  
  class TestSetup(
                   origin: Option[String] = None,
                   isAgent: Boolean = false,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   welshLang: Boolean = false,
                   obligations: Seq[SingleObligationModel] = getSingleObligationModels(),
                   nextTaxReturnDueDate: Option[LocalDate] = None,
                   nextQuarterlyUpdatesDueDate: Option[LocalDate] = None) {
    
  }
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
    val submissionDeadlinesURLAgent = "/report-quarterly/income-and-expenses/view/agents/submission-deadlines"

    lazy val page: HtmlFormat.Appendable =
      newHomeYourTasksView(
        origin = origin,
        viewModel = getNextUpdatesTileViewModel(dateServiceCurrentDate, obligations, nextTaxReturnDueDate, nextQuarterlyUpdatesDueDate),
        isAgent = isAgent,
        yourTasksUrl = yourTasksUrl,
        recentActivityUrl = recentActivityUrl,
        overViewUrl = overViewUrl,
        helpUrl = helpUrl,
        isGovUkRebrandEnabled = true)(testMessages, FakeRequest(), testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }
    
  "New Home Your Tasks page for individuals" when {
    "upcoming annual submission due more than 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(31))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--green") shouldBe true
        document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.plusDays(31).toLongDateShort}"
      }
    }

    "upcoming annual submission due exactly in 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.plusDays(30).toLongDateShort}"
      }
    }

    "upcoming annual submission due in 1 day" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(1))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.plusDays(1).toLongDateShort}"
      }
    }

    "upcoming annual submission due less then 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(29))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.plusDays(29).toLongDateShort}"
      }
    }

    "upcoming annual submission due today" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate)) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--pink") shouldBe true
        document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.toLongDateShort}"
      }
    }

    "upcoming annual submission coming due less or equal that 30 days" should {
      "display the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(30))) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
        document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--yellow") shouldBe true
        document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.plusDays(30).toLongDateShort}"
      }
    }
  }

  "New Home Overview page for agents" should {
    "display the the correct content" in new TestSetup(nextTaxReturnDueDate = Some(dateServiceCurrentDate.plusDays(31)), isAgent = true) {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
      document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
      document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURLAgent)
      document.select(".govuk-body").get(2).text() shouldBe upcomingAnnualSubmissionDeadlineBody
      document.select("#upcomingAnnualDate").hasClass("govuk-tag govuk-tag--green") shouldBe true
      document.select("#upcomingAnnualDate").text() shouldBe s"Due ${dateServiceCurrentDate.plusDays(31).toLongDateShort}"
    }
  }

  "New Home Your Tasks page for individuals" when {
    "single overdue annual submission" should {
      "display the correct content" in new TestSetup(obligations = getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(30)), nextTaxReturnDueDate = None) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select("#finalAnnualOverdueSubmissionTile div h3").text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select("#finalAnnualOverdueSubmissionTile div h3").get(0).hasCorrectHref(submissionDeadlinesURL)
        document.select("#finalAnnualOverdueSubmissionTile div p").get(0).text() shouldBe "You have an overdue annual submission."
        document.select("#finalAnnualOverdueSubmissionTile div p").get(1).select("span").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.select("#finalAnnualOverdueSubmissionTile div p").get(1).select("span").text() shouldBe s"Was due ${dateServiceCurrentDate.minusDays(30).toLongDateShort}"
      }
    }

    "multiple overdue annual submissions" should {
      "display the correct content" in new TestSetup(obligations = multipleAnnualOverdueObligations, nextTaxReturnDueDate = None) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select("#finalAnnualOverdueSubmissionTile div h3").text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select("#finalAnnualOverdueSubmissionTile div h3").get(0).hasCorrectHref(submissionDeadlinesURL)
        document.select("#finalAnnualOverdueSubmissionTile div p").get(0).text() shouldBe "You have 3 overdue annual submissions."
        document.select("#finalAnnualOverdueSubmissionTile div p").get(1).select("span").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.select("#finalAnnualOverdueSubmissionTile div p").get(1).select("span").text() shouldBe s"Oldest submission due ${dateServiceCurrentDate.minusDays(120).toLongDateShort}"
      }
    }

    "single overdue quarterly submission" should {
      "display the correct content" in new TestSetup(obligations = getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(30), quarterlyObligationType), nextTaxReturnDueDate = None) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select("#finalQuarterlyOverdueSubmissionTile div h3").text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select("#finalQuarterlyOverdueSubmissionTile div h3").get(0).hasCorrectHref(submissionDeadlinesURL)
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(0).text() shouldBe "You have an overdue quarterly submission."
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(1).select("span").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(1).select("span").text() shouldBe s"Was due ${dateServiceCurrentDate.minusDays(30).toLongDateShort}"
      }
    }

    "multiple overdue quarterly submissions" should {
      "display the correct content" in new TestSetup(obligations = multipleQuarterlyOverdueObligations, nextTaxReturnDueDate = None) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select("#finalQuarterlyOverdueSubmissionTile div h3").text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select("#finalQuarterlyOverdueSubmissionTile div h3").get(0).hasCorrectHref(submissionDeadlinesURL)
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(0).text() shouldBe "You have 3 overdue quarterly submissions."
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(1).select("span").hasClass("govuk-tag govuk-tag--red") shouldBe true
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(1).select("span").text() shouldBe s"Oldest submission due ${dateServiceCurrentDate.minusDays(120).toLongDateShort}"
      }
    }

    "multiple overdue quarterly and annual submissions" should {
      "display the correct content" in new TestSetup(obligations = multipleQuarterlyAndAnnualOverdueObligations, nextTaxReturnDueDate = None) {
        document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
        document.select(".hmrc-card__heading").get(1).text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".hmrc-card__heading").get(1).hasCorrectHref(submissionDeadlinesURL)
        document.select("#finalAnnualOverdueSubmissionTile div p").get(0).text() shouldBe "You have 2 overdue annual submissions."
        document.select(".govuk-body").get(3).select("span").hasClass("govuk-tag govuk-tag--red")
        document.select("#finalAnnualOverdueSubmissionTile div p").get(1).text() shouldBe s"Oldest submission due ${dateServiceCurrentDate.minusDays(100).toLongDateShort}"

        document.select("#finalQuarterlyOverdueSubmissionTile div h3").text() shouldBe viewUpdatesAndDeadlinesTitle
        document.select(".govuk-body").get(4).text() shouldBe "You have 2 overdue quarterly submissions."
        document.select(".govuk-body").get(6).select("span").hasClass("govuk-tag govuk-tag--red")
        document.select("#finalQuarterlyOverdueSubmissionTile div p").get(1).text() shouldBe s"Oldest submission due ${dateServiceCurrentDate.minusDays(120).toLongDateShort}"
      }
    }

    "New Home Your Tasks page for agents" when {
      "single overdue annual submission" should {
        "display the correct content" in new TestSetup(obligations = getSingleObligationModels(dueDate = dateServiceCurrentDate.minusDays(30)), nextTaxReturnDueDate = None, isAgent = true) {
          document.select("h2.govuk-heading-m").get(0).text() shouldBe yourTasksHeading
          document.select("#finalAnnualOverdueSubmissionTile div h3").text() shouldBe viewUpdatesAndDeadlinesTitle
          document.select("#finalAnnualOverdueSubmissionTile div h3").get(0).hasCorrectHref(submissionDeadlinesURLAgent)
          document.select("#finalAnnualOverdueSubmissionTile div p").get(0).text() shouldBe "You have an overdue annual submission."
          document.select("#finalAnnualOverdueSubmissionTile div p").get(1).select("span").hasClass("govuk-tag govuk-tag--red") shouldBe true
          document.select("#finalAnnualOverdueSubmissionTile div p").get(1).select("span").text() shouldBe s"Was due ${dateServiceCurrentDate.minusDays(30).toLongDateShort}"
        }
      }
    }

  }



  private def getSingleObligationModels(dueDate: LocalDate = dateServiceCurrentDate.plusDays(31), obligationType: String = annualObligationType): Seq[SingleObligationModel] =
    Seq(SingleObligationModel(dateServiceCurrentDate.minusMonths(6), dateServiceCurrentDate.minusMonths(3), dueDate, obligationType, None, "#002", StatusOpen))

  private def getNextUpdatesTileViewModel(currentDate: LocalDate, obligations: Seq[SingleObligationModel], nextTaxReturnDueDate: Option[LocalDate], nextQuarterlyUpdatesDueDate: Option[LocalDate]): HandleYourTasksViewModel = {
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
      obligations,
      currentDate,
      nextQuarterlyUpdatesDueDate,
      nextTaxReturnDueDate,
    )

    HandleYourTasksViewModel(
      outstandingChargesModel = List.empty,
      credits = zeroCredits,
      creditsRefundsRepayEnabled = false,
      obligations = updatesAndDeadlinesViewModel,
      userMandatedOrVoluntary = false
    )
  }
}
