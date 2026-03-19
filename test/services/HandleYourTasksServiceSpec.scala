package services

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import models.creditsandrefunds.CreditsModel
import models.financialDetails.ChargeItem
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.newHomePage.MaturityLevel.Upcoming
import models.newHomePage.YourTaskCardType.{FINANCIALS, SUBMISSIONS}
import models.newHomePage.YourTasksCard.{DatelessTaskCard, OverdueTaskCard, UpcomingTaskCard}
import models.newHomePage.{HandleYourTasksViewModel, NoTaskCard, SubmissionDeadlinesViewModel}
import models.obligations.{SingleObligationModel, StatusOpen}
import testConstants.BaseTestConstants.testNino
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class HandleYourTasksServiceSpec extends TestSupport {

  val service = new HandleYourTasksService(appConfig)

  def singleObligation(currentDate: LocalDate, isQuarterly: Boolean) = {
    val obligationType = if(isQuarterly) "Quarterly" else "Crystallisation"
    SingleObligationModel(
      currentDate,
      currentDate,
      currentDate,
      obligationType,
      None,
      "periodKey",
      StatusOpen
    )
  }

  def creditsModel(moneyInAccount: BigDecimal): CreditsModel = CreditsModel(0, 0, 0, moneyInAccount, None, None, List.empty)

  val overdueAnnualSubmission: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-annual-submission-single-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-annual-submission-single-label", Some(LocalDate.of(2021, 12, 15)), None, SUBMISSIONS)
  val overdueQuarterlySubmission: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-quarterly-submission-single-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-quarterly-submission-single-label", Some(LocalDate.of(2021, 12, 15)), None, SUBMISSIONS)

  val overdueMultipleAnnualSubmissions: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-annual-submission-multiple-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-annual-submission-multiple-label", Some(LocalDate.of(2020, 12, 15)), Some("2"), SUBMISSIONS)
  val overdueMultipleQuarterlySubmissions: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-quarterly-submission-multiple-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-quarterly-submission-multiple-label", Some(LocalDate.of(2020, 12, 15)), Some("2"), SUBMISSIONS)

  val moneyInYourAccountTask: DatelessTaskCard = DatelessTaskCard("newHome.yourTasks.selfAssessment.money-in-account", "newHome.yourTasks.selfAssessment.money-in-account.h1", "/report-quarterly/income-and-expenses/view/money-in-your-account", Some("1000"), FINANCIALS)

  val upcomingAnnualSubmissionsTaskCard: UpcomingTaskCard = UpcomingTaskCard("new.home.yourTasks.upcoming-annual-updates-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.upcoming-annual-updates-label", Some(LocalDate.of(2026, 12, 15)), None, Upcoming, SUBMISSIONS)
  val upcomingQuarterlySubmissionsTaskCard: UpcomingTaskCard = UpcomingTaskCard("new.home.yourTasks.upcoming-quarterly-updates-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.upcoming-quarterly-updates-label", Some(LocalDate.of(2026, 12, 15)), None, Upcoming, SUBMISSIONS)

  val noTaskCard = NoTaskCard("newHome.yourTasks.no-tasks.selfAssessment", "newHome.yourTasks.selfAssessment.no-tasks")
  val noTaskSupportingAgentsTask = NoTaskCard("newHome.yourTasks.no-tasks.selfAssessment", "newHome.yourTasks.selfAssessment.supporting.no-tasks")

  class TestSetup(isAgent: Boolean = false,
                  supportingAgent: Boolean = false,
                  hasUpcomingQuarterlyDate: Boolean = false,
                  hasUpcomingAnnualDate: Boolean = false,
                  creditsRefundsRepayEnabled: Boolean = false,
                  credits: CreditsModel = creditsModel(0),
                  currentItsaStatus: ITSAStatus = ITSAStatus.Annual,
                  obligations: Seq[SingleObligationModel] = Seq.empty,
                  chargeItemList: List[ChargeItem] = List.empty) {

    val nextQuarterlyDueDate = if(hasUpcomingQuarterlyDate) Some(fixedDate.plusYears(3)) else None
    val nextTaxReturnDueDate = if (hasUpcomingAnnualDate) Some(fixedDate.plusYears(3)) else None

    val submissionsViewModel = SubmissionDeadlinesViewModel(obligations, fixedDate, nextQuarterlyDueDate, nextTaxReturnDueDate)

    val affinityGroup = if(isAgent) Agent else Individual
    val testUser: MtdItUser[_] = defaultMTDITUser(Some(affinityGroup), IncomeSourceDetailsModel("AB123456C", "123", Some("2023"), List.empty, List.empty), isSupportingAgent = supportingAgent)

    val resultViewModel = service.getYourTasksCards(submissionsViewModel, isAgent, chargeItemList, credits, creditsRefundsRepayEnabled, currentItsaStatus)(testUser)
  }


  "getYourTasksCards" should {
    "return overdue task cards" when {
      "there's an overdue charge" in {

      }
      "there's a late payment penalty" in {

      }
      "there's a late submission penalty" in {

      }
      "there's multiple late payment penalties" in {

      }
      "there's multiple late submission penalties" in {

      }
      "there's multiple charges" in {

      }

      "there's an overdue annual submission" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = false))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueAnnualSubmission), Seq.empty, Seq.empty, None)
      }

      "there's an overdue quarterly submission" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = true)), currentItsaStatus = ITSAStatus.Mandated) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueQuarterlySubmission), Seq.empty, Seq.empty, None)
      }

      "there's multiple overdue annual submissions" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = false), singleObligation(fixedDate.minusYears(3), isQuarterly = false))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleAnnualSubmissions), Seq.empty, Seq.empty, None)
      }

      "there's multiple overdue quarterly submissions" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = true), singleObligation(fixedDate.minusYears(3), isQuarterly = true)), currentItsaStatus = ITSAStatus.Mandated) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleQuarterlySubmissions), Seq.empty, Seq.empty, None)
      }
    }

    "return a dateless task card" when {
      "there is money in a user's account with creditsRefundsRepay enabled" in new TestSetup(creditsRefundsRepayEnabled = true, credits = creditsModel(1000)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq(moneyInYourAccountTask), Seq.empty, None)
      }
    }

    "return the correct upcoming task cards" when {
      "there is a future annual submission" in new TestSetup(hasUpcomingAnnualDate = true, obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = false))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq(upcomingAnnualSubmissionsTaskCard), None)
      }

      "there is a future quarterly submission and the user's ITSA status is mandated" in new TestSetup(hasUpcomingQuarterlyDate = true, currentItsaStatus = ITSAStatus.Mandated, obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq(upcomingQuarterlySubmissionsTaskCard), None)
      }

      "there is a future quarterly submission and the user's ITSA status is voluntary" in new TestSetup(hasUpcomingQuarterlyDate = true, currentItsaStatus = ITSAStatus.Voluntary, obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq(upcomingQuarterlySubmissionsTaskCard), None)
      }

      "there is upcoming payments" in {

      }
    }
    "return the no task card" when {
      "there are no overdue, dateless or upcoming tasks" in new TestSetup() {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }

      "there are penalties and financial related tasks however the user is a supporting agent" in new TestSetup() {

      }
    }

    "not show specific task cards" when {
      "the user's ITSA status is annual and they have overdue quarterly obligations marked" in new TestSetup(obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
      "the user's ITSA status is exempt and they have overdue quarterly obligations marked" in new TestSetup(obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true)), currentItsaStatus = ITSAStatus.Exempt) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
      "the user's ITSA status is digitally exempt and they have overdue quarterly obligations marked" in new TestSetup(obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true)), currentItsaStatus = ITSAStatus.DigitallyExempt) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
      "the user's ITSA status is annual and they have upcoming quarterly obligations marked" in new TestSetup(obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true)), hasUpcomingQuarterlyDate = true, currentItsaStatus = ITSAStatus.DigitallyExempt) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
      "the user's ITSA status is exempt and they have upcoming quarterly obligations marked" in new TestSetup(obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true)),hasUpcomingQuarterlyDate = true, currentItsaStatus = ITSAStatus.Exempt) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
      "the user's ITSA status is digitally exempt and they have upcoming quarterly obligations marked" in new TestSetup(obligations = Seq(singleObligation(fixedDate.plusYears(2), isQuarterly = true)), hasUpcomingQuarterlyDate = true, currentItsaStatus = ITSAStatus.DigitallyExempt) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
      "the user is a supporting agent there is money in a user's account" in new TestSetup(supportingAgent = true, credits = creditsModel(1000)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskSupportingAgentsTask))
      }
      "the user is a supporting agent there's an overdue charge" in {

      }
      "the user is a supporting agent there's a late payment penalty" in {

      }
      "the user is a supporting agent there's a late submission penalty" in {

      }
      "the user has money in their account but creditsRefundsRepayEnabled is false" in {
        
      }
    }
  }
}
