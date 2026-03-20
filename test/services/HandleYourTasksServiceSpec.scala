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

package services

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import models.creditsandrefunds.CreditsModel
import models.financialDetails.*
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.newHomePage.MaturityLevel.Upcoming
import models.newHomePage.YourTaskCardType.{FINANCIALS, PENALTIES, SUBMISSIONS}
import models.newHomePage.YourTasksCard.{DatelessTaskCard, OverdueTaskCard, UpcomingTaskCard}
import models.newHomePage.{HandleYourTasksViewModel, NoTaskCard, SubmissionDeadlinesViewModel}
import models.obligations.{SingleObligationModel, StatusOpen}
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

  val overdueCharge = ChargeItem("100", fixedTaxYear, BalancingCharge, None, LocalDate.of(2018, 3, 29), Some(fixedDate.minusYears(3)),10000,1000,Some(10),None,None,Some(LocalDate.of(2020, 8, 1)),Some(LocalDate.of(2021, 7, 21)),Some(3),None,None,false,None,Some(LocalDate.of(2020, 8, 13)),None,None,None,Some("AA000000000001"))
  val overdueLatePaymentPenalty = ChargeItem("200", fixedTaxYear, FirstLatePaymentPenalty, None, LocalDate.of(2018, 3, 29), Some(fixedDate.minusYears(2)),10000,1000,Some(10),None,None,Some(LocalDate.of(2020, 8, 1)),Some(LocalDate.of(2021, 7, 21)),Some(3),None,None,false,None,Some(LocalDate.of(2020, 8, 13)),None,None,None,Some("AA000000000002"))
  val overdueLateSubmissionPenalty = ChargeItem("300", fixedTaxYear, LateSubmissionPenalty, None, LocalDate.of(2018, 3, 29), Some(fixedDate.minusYears(1)), 10000, 1000, Some(10), None, None, Some(LocalDate.of(2020, 8, 1)), Some(LocalDate.of(2021, 7, 21)), Some(3), None, None, false, None, Some(LocalDate.of(2020, 8, 13)), None, None, None, Some("AA000000000003"))
  val upcomingCharge = ChargeItem("400", fixedTaxYear.nextYear, MfaDebitCharge, None, LocalDate.of(2018, 3, 29), Some(fixedDate.plusYears(100)),10000,1000,Some(10),None,None,Some(LocalDate.of(2020, 8, 1)),Some(LocalDate.of(2021, 7, 21)),Some(3),None,None,false,None,Some(LocalDate.of(2020, 8, 13)),None,None,None,Some("AA000000000001"))

  val overdueAnnualSubmissionCard: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-annual-submission-single-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-annual-submission-single-label", Some(LocalDate.of(2021, 12, 15)), None, SUBMISSIONS)
  val overdueQuarterlySubmissionCard: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-quarterly-submission-single-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-quarterly-submission-single-label", Some(LocalDate.of(2021, 12, 15)), None, SUBMISSIONS)

  val overdueMultipleAnnualSubmissionsCard: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-annual-submission-multiple-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-annual-submission-multiple-label", Some(LocalDate.of(2020, 12, 15)), Some("2"), SUBMISSIONS)
  val overdueMultipleQuarterlySubmissionsCard: OverdueTaskCard = OverdueTaskCard("new.home.yourTasks.overdue-quarterly-submission-multiple-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.overdue-quarterly-submission-multiple-label", Some(LocalDate.of(2020, 12, 15)), Some("2"), SUBMISSIONS)

  val overdueChargeCard: OverdueTaskCard = OverdueTaskCard("newHome.yourTasks.selfAssessment.overdueCharge.single", "newHome.yourTasks.selfAssessment", "/report-quarterly/income-and-expenses/view/what-you-owe", "newHome.yourTasks.selfAssessment.overdueCharge.single.label", Some(fixedDate.minusYears(3)), Some("1000"), PENALTIES)
  val overdueLppCard: OverdueTaskCard = OverdueTaskCard("newHome.yourTasks.selfAssessment.lpp.single", "newHome.yourTasks.selfAssessment.lpp.single.link", "/report-quarterly/income-and-expenses/view/tax-years/2024/charge?id=200", "newHome.yourTasks.selfAssessment.lpp.single.label", Some(fixedDate.minusYears(2)), None, PENALTIES)
  val overdueLspCard: OverdueTaskCard = OverdueTaskCard("newHome.yourTasks.selfAssessment.lsp.single", "newHome.yourTasks.selfAssessment.lsp.single.link", "/report-quarterly/income-and-expenses/view/tax-years/2024/charge?id=300", "newHome.yourTasks.selfAssessment.lsp.single.label", Some(fixedDate.minusYears(1)), None, PENALTIES)

  val overdueMultipleLppCard: OverdueTaskCard = OverdueTaskCard("newHome.yourTasks.selfAssessment.lpp.multiple", "newHome.yourTasks.selfAssessment.lpp.multiple.link", "http://localhost:9185/view-penalty/self-assessment#lppTab", "newHome.yourTasks.selfAssessment.lsp.multiple.label", Some(fixedDate.minusYears(2)), Some("2"), PENALTIES)
  val overdueMultipleLspCard: OverdueTaskCard = OverdueTaskCard("newHome.yourTasks.selfAssessment.lsp.multiple", "newHome.yourTasks.selfAssessment.lsp.multiple.link", "http://localhost:9185/view-penalty/self-assessment#lspTab", "newHome.yourTasks.selfAssessment.lsp.multiple.label", Some(fixedDate.minusYears(1)), Some("2"), PENALTIES)
  val overdueMultipleChargeCard: OverdueTaskCard = OverdueTaskCard("newHome.yourTasks.selfAssessment.overdueCharge.multiple", "newHome.yourTasks.selfAssessment", "/report-quarterly/income-and-expenses/view/what-you-owe", "newHome.yourTasks.selfAssessment.overdueCharge.multiple.label", Some(fixedDate.minusYears(3)), Some("2000"), PENALTIES)

  val moneyInYourAccountTask: DatelessTaskCard = DatelessTaskCard("newHome.yourTasks.selfAssessment.money-in-account", "newHome.yourTasks.selfAssessment.money-in-account.h1", "/report-quarterly/income-and-expenses/view/money-in-your-account", Some("1000"), FINANCIALS)

  val upcomingAnnualSubmissionsTaskCard: UpcomingTaskCard = UpcomingTaskCard("new.home.yourTasks.upcoming-annual-updates-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.upcoming-annual-updates-label", Some(LocalDate.of(2026, 12, 15)), None, Upcoming, SUBMISSIONS)
  val upcomingQuarterlySubmissionsTaskCard: UpcomingTaskCard = UpcomingTaskCard("new.home.yourTasks.upcoming-quarterly-updates-body", "new.home.yourTasks.updates-and-deadlines", "/report-quarterly/income-and-expenses/view/submission-deadlines", "new.home.yourTasks.upcoming-quarterly-updates-label", Some(LocalDate.of(2026, 12, 15)), None, Upcoming, SUBMISSIONS)

  val upcomingChargeCard: UpcomingTaskCard = UpcomingTaskCard("newHome.yourTasks.selfAssessment.upcomingCharge", "newHome.yourTasks.selfAssessment", "/report-quarterly/income-and-expenses/view/what-you-owe", "newHome.yourTasks.selfAssessment.upcomingCharge.label", Some(fixedDate.plusYears(100)), None, Upcoming, PENALTIES)

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
      "there's an overdue charge" in new TestSetup(chargeItemList = List(overdueCharge)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueChargeCard), Seq.empty, Seq.empty, None)
      }
      "there's a late payment penalty" in new TestSetup(chargeItemList = List(overdueLatePaymentPenalty)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueLppCard), Seq.empty, Seq.empty, None)
      }
      "there's a late submission penalty" in new TestSetup(chargeItemList = List(overdueLateSubmissionPenalty)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueLspCard), Seq.empty, Seq.empty, None)
      }
      "there's multiple late payment penalties" in new TestSetup(chargeItemList = List(overdueLatePaymentPenalty, overdueLatePaymentPenalty)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleLppCard), Seq.empty, Seq.empty, None)
      }
      "there's multiple late submission penalties" in new TestSetup(chargeItemList = List(overdueLateSubmissionPenalty, overdueLateSubmissionPenalty)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleLspCard), Seq.empty, Seq.empty, None)
      }
      "there's multiple charges" in new TestSetup(chargeItemList = List(overdueCharge, overdueCharge)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleChargeCard), Seq.empty, Seq.empty, None)
      }

      "there's an overdue annual submission" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = false))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueAnnualSubmissionCard), Seq.empty, Seq.empty, None)
      }

      "there's an overdue quarterly submission" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = true)), currentItsaStatus = ITSAStatus.Mandated) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueQuarterlySubmissionCard), Seq.empty, Seq.empty, None)
      }

      "there's multiple overdue annual submissions" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = false), singleObligation(fixedDate.minusYears(3), isQuarterly = false))) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleAnnualSubmissionsCard), Seq.empty, Seq.empty, None)
      }

      "there's multiple overdue quarterly submissions" in new TestSetup(obligations = Seq(singleObligation(fixedDate.minusYears(2), isQuarterly = true), singleObligation(fixedDate.minusYears(3), isQuarterly = true)), currentItsaStatus = ITSAStatus.Mandated) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueMultipleQuarterlySubmissionsCard), Seq.empty, Seq.empty, None)
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

      "there is upcoming payments" in new TestSetup(chargeItemList = List(upcomingCharge)) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq(upcomingChargeCard), None)
      }
    }
    "return the no task card" when {
      "there are no overdue, dateless or upcoming tasks" in new TestSetup() {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
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
      "the user is a supporting agent there is money in a user's account" in new TestSetup(supportingAgent = true, credits = creditsModel(1000), isAgent = true) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskSupportingAgentsTask))
      }
      "the user is a supporting agent there's an overdue charge"  in new TestSetup(chargeItemList = List(overdueCharge), supportingAgent = true, isAgent = true)  {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskSupportingAgentsTask))
      }
      "the user is a supporting agent there's a late payment penalty"  in new TestSetup(chargeItemList = List(overdueLatePaymentPenalty), supportingAgent = true, isAgent = true)  {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskSupportingAgentsTask))
      }
      "the user is a supporting agent there's a late submission penalty"  in new TestSetup(chargeItemList = List(overdueLateSubmissionPenalty), supportingAgent = true, isAgent = true)  {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskSupportingAgentsTask))
      }
      "the user has money in their account but creditsRefundsRepayEnabled is false" in new TestSetup(credits = creditsModel(1000), creditsRefundsRepayEnabled = false) {
        resultViewModel shouldBe HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, Some(noTaskCard))
      }
    }

    "put the cards in the correct order" when {
      "an overdue submissions card and an overdue penalty card have the same due date (Penalties first then Submissions)" in new TestSetup(chargeItemList = List(overdueCharge), obligations = Seq(singleObligation(fixedDate.minusYears(3), isQuarterly = false))){
        resultViewModel shouldBe HandleYourTasksViewModel(Seq(overdueChargeCard, overdueAnnualSubmissionCard.copy(dueDate = Some(fixedDate.minusYears(3)))), Seq.empty, Seq.empty, None)
      }
    }
  }
}
