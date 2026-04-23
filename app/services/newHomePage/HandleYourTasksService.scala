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

package services.newHomePage

import auth.MtdItUser
import config.FrontendAppConfig
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import models.creditsandrefunds.CreditsModel
import models.financialDetails.*
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.newHomePage.*
import models.newHomePage.YourTaskCardType.{FINANCIALS, PENALTIES, SUBMISSIONS}
import models.newHomePage.YourTasksCard.*

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import obligations.controllers.{routes => obligationsRoutes}

@Singleton
class HandleYourTasksService @Inject(appConfig: FrontendAppConfig) {

  private val chargesSet: Set[TransactionType] = Set(PoaOneDebit, PoaTwoDebit, PoaOneReconciliationDebit, PoaTwoReconciliationDebit, BalancingCharge, MfaDebitCharge, ITSAReturnAmendment)
  private val lppSet: Set[TransactionType] = Set(FirstLatePaymentPenalty, SecondLatePaymentPenalty)
  private val lspSet: Set[TransactionType] = Set(LateSubmissionPenalty)

  def getYourTasksCards(submissionsViewModel: SubmissionDeadlinesViewModel,
                        isAgent: Boolean,
                        chargeItemList: List[ChargeItem],
                        credits: CreditsModel,
                        creditsRefundsRepayEnabled: Boolean,
                        currentItsaStatus: ITSAStatus,
                        penaltiesAndAppealsEnabled: Boolean)(implicit user: MtdItUser[_]) = {

    val isQuarterly = currentItsaStatus == ITSAStatus.Mandated || currentItsaStatus == ITSAStatus.Voluntary

    val submissionsTasks = getSubmissionTasks(submissionsViewModel, isAgent, isQuarterly)
    val paymentsTasks    = if(user.isSupportingAgent) None else getFinancialsAndPenalties(chargeItemList, credits, creditsRefundsRepayEnabled, isAgent, penaltiesAndAppealsEnabled)

    val allTasks = submissionsTasks ++ paymentsTasks

    if (allTasks.isEmpty) {
      val noTaskDescription = if(user.isSupportingAgent) "newHome.yourTasks.selfAssessment.supporting.no-tasks" else "newHome.yourTasks.selfAssessment.no-tasks"
      val noTaskCard: NoTaskCard = NoTaskCard("newHome.yourTasks.no-tasks.selfAssessment", noTaskDescription)

      HandleYourTasksViewModel(overdueTasks = Seq.empty, datelessTasks = Seq.empty, upcomingTasks = Seq.empty, noTaskCard = Some(noTaskCard))
    } else {
      given Ordering[YourTaskCardType] = Ordering.by {
        case FINANCIALS => 0
        case PENALTIES => 1
        case SUBMISSIONS => 2
      }

      val (overdue, dateless, upcoming) = (
        allTasks.collect { case c: OverdueTaskCard => c },
        allTasks.collect { case c: DatelessTaskCard => c },
        allTasks.collect { case c: UpcomingTaskCard => c }
      )

      val sortedOverdueTasks = overdue.sortBy { c => (c.dueDate.fold(Long.MaxValue)(_.toEpochDay), c.cardType) }
      val sortedUpcomingTasks = upcoming.sortBy { c => (c.dueDate.fold(Long.MaxValue)(_.toEpochDay), c.cardType) }

      HandleYourTasksViewModel(overdueTasks = sortedOverdueTasks, datelessTasks = dateless, upcomingTasks = sortedUpcomingTasks, noTaskCard = None)
    }
  }

  private def getSubmissionTasks(viewModel: SubmissionDeadlinesViewModel, isAgent: Boolean, isQuarterly: Boolean): Seq[YourTasksCard] = {
    val submissionsLink = if(isAgent) {
      obligationsRoutes.NextUpdatesController.showAgent().url
    } else {
      obligationsRoutes.NextUpdatesController.show().url
    }

    val submissionsLinkTextKey = "new.home.yourTasks.updates-and-deadlines"

    val overdueCards =  if(viewModel.hasOpenObligations) {
      getOverdueSubmissionsCards(viewModel, submissionsLink, submissionsLinkTextKey, isQuarterly)
    } else Seq.empty

    overdueCards ++ getUpcomingSubmissionsCards(viewModel, submissionsLink, submissionsLinkTextKey, isQuarterly)
  }


  protected def getOverdueSubmissionsCards(viewModel: SubmissionDeadlinesViewModel, submissionsLink: String, submissionsLinkText: String, isQuarterly: Boolean): Seq[YourTasksCard] = {
    val annualCount    = viewModel.getNumberOfOverdueAnnualObligations
    val quarterlyCount = viewModel.getNumberOfOverdueQuarterlyObligations
    val hasAnnual      = viewModel.isAnnualObligations
    val hasQuarterly   = viewModel.isQuarterlyObligations

    val overdueAnnualSubmission = OverdueTaskCard("new.home.yourTasks.overdue-annual-submission-single-body", submissionsLinkText, submissionsLink, "new.home.yourTasks.overdue-annual-submission-single-label", viewModel.getOldestAnnualOverdueDate, cardType = SUBMISSIONS)
    val multipleOverdueAnnualSubmission = OverdueTaskCard("new.home.yourTasks.overdue-annual-submission-multiple-body", submissionsLinkText, submissionsLink, "new.home.yourTasks.overdue-annual-submission-multiple-label", viewModel.getOldestAnnualOverdueDate, Some(annualCount.toString),  cardType = SUBMISSIONS)

    val overdueQuarterlySubmission = OverdueTaskCard("new.home.yourTasks.overdue-quarterly-submission-single-body", submissionsLinkText, submissionsLink, "new.home.yourTasks.overdue-quarterly-submission-single-label", viewModel.getOldestQuarterlyOverdueDate, cardType = SUBMISSIONS)
    val multipleOverdueQuarterlySubmission = OverdueTaskCard("new.home.yourTasks.overdue-quarterly-submission-multiple-body", submissionsLinkText, submissionsLink, "new.home.yourTasks.overdue-quarterly-submission-multiple-label", viewModel.getOldestQuarterlyOverdueDate, Some(quarterlyCount.toString), cardType = SUBMISSIONS)

    val annualCardOpt = if(hasAnnual && annualCount > 0) {
      Some(if(annualCount == 1) overdueAnnualSubmission else multipleOverdueAnnualSubmission)
    } else None

    val quarterlyCardOpt = {
      if (!isQuarterly) {
        None
      } else if(hasQuarterly && quarterlyCount > 0) {
        Some(if(quarterlyCount == 1) overdueQuarterlySubmission else multipleOverdueQuarterlySubmission)
      } else None
    }

    annualCardOpt.toList ++ quarterlyCardOpt.toList
  }

  private def getUpcomingSubmissionsCards(
                                           viewModel: SubmissionDeadlinesViewModel,
                                           submissionsLink: String,
                                           submissionsLinkText: String,
                                           isQuarterly: Boolean
                                         ): Seq[UpcomingTaskCard] = {

    val annualCardOpt: Option[UpcomingTaskCard] =
      if (viewModel.isAnnualObligations && viewModel.nextTaxReturnDueDate.nonEmpty) {
        for {
          date     <- viewModel.nextTaxReturnDueDate
          maturity <- MaturityLevel.get(viewModel.nextTaxReturnDueDate)
        } yield UpcomingTaskCard("new.home.yourTasks.upcoming-annual-updates-body", submissionsLinkText, submissionsLink, "new.home.yourTasks.upcoming-annual-updates-label", Some(date), maturityLevel = maturity, cardType = SUBMISSIONS)
      } else None

    val quarterlyCardOpt: Option[UpcomingTaskCard] =
      if (viewModel.isQuarterlyObligations && viewModel.nextQuarterlyUpdateDueDate.nonEmpty && isQuarterly) {
        for {
          date     <- viewModel.nextQuarterlyUpdateDueDate
          maturity <- MaturityLevel.get(viewModel.nextQuarterlyUpdateDueDate)
        } yield UpcomingTaskCard("new.home.yourTasks.upcoming-quarterly-updates-body", submissionsLinkText, submissionsLink, "new.home.yourTasks.upcoming-quarterly-updates-label", Some(date), maturityLevel = maturity, cardType = SUBMISSIONS)
      } else None

    annualCardOpt.toSeq ++ quarterlyCardOpt.toSeq
  }

  private def getFinancialsAndPenalties(chargeItemList: List[ChargeItem],
                                credits: CreditsModel,
                                creditsRefundsRepayEnabled: Boolean,
                                isAgent: Boolean,
                                        penaltiesAndAppealsEnabled: Boolean): Seq[YourTasksCard] = {

    val redirectLinkMoneyInYourAccount = {
      if (isAgent) {
        controllers.routes.MoneyInYourAccountController.showAgent().url
      } else {
        controllers.routes.MoneyInYourAccountController.show().url
      }
    }

    val datelessTasks = if (creditsRefundsRepayEnabled) getMoneyInAccount(credits, redirectLinkMoneyInYourAccount) else Seq.empty

    val paymentTask = if(!chargeItemList.isEmpty) {
      getPaymentsCard(chargeItemList, isAgent, penaltiesAndAppealsEnabled)
    } else Seq.empty

    datelessTasks ++ paymentTask
  }

  private def getMoneyInAccount(credits: CreditsModel, moneyInAccountLink: String): Seq[DatelessTaskCard] = {
    if(credits.totalCredit > 0) {
      Seq(DatelessTaskCard("newHome.yourTasks.selfAssessment.money-in-account", "newHome.yourTasks.selfAssessment.money-in-account.h1", moneyInAccountLink, Some(credits.totalCredit.toCurrencyString), cardType = FINANCIALS))
    } else {
      Seq.empty
    }
  }

  private def getPaymentsCard(chargeItemList: List[ChargeItem],
                              isAgent: Boolean,
                              penaltiesAndAppealsEnabled: Boolean): Seq[YourTasksCard] = {

    val dueChargesList: List[ChargeItem] = dueChargesByTypes(chargeItemList, chargesSet)
    val dueLppsList: List[ChargeItem]    = dueChargesByTypes(chargeItemList, lppSet)
    val dueLspsList: List[ChargeItem]    = dueChargesByTypes(chargeItemList, lspSet)

    val chargeTask = if(dueChargesList.isEmpty) Seq.empty else getChargesCard(dueChargesList, isAgent)
    val lppTask = if(dueLppsList.isEmpty || !penaltiesAndAppealsEnabled) Seq.empty else getLppCard(dueLppsList, isAgent)
    val lspTask = if(dueLspsList.isEmpty || !penaltiesAndAppealsEnabled) Seq.empty else getLspCard(dueLspsList, isAgent)

    chargeTask ++ lppTask ++ lspTask
  }

  private def getChargesCard(chargeList: List[ChargeItem], isAgent: Boolean) = {
    val linkText = "newHome.yourTasks.selfAssessment"
    val redirectLink = if(isAgent) controllers.routes.WhatYouOweController.showAgent().url else controllers.routes.WhatYouOweController.show().url

    val oldestCharge = oldestTransaction(chargeList)
    val overdueCharges = chargeList.filter(charge => chargesSet.contains(charge.transactionType) && charge.dueDate.exists(_.isBefore(LocalDate.now())))

    val contentDescription = oldestCharge.flatMap { charge => {
        MaturityLevel.get(charge.dueDate) match {
          case Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueEarly) | Some(MaturityLevel.DueToday) => Some(("newHome.yourTasks.selfAssessment.upcomingCharge", None))
          case Some(MaturityLevel.Overdue) => if(overdueCharges.size > 1) {
            Some(("newHome.yourTasks.selfAssessment.overdueCharge.multiple", Some(chargeList.map(_.outstandingAmount).sum.toCurrencyString)))
          } else {
            Some(("newHome.yourTasks.selfAssessment.overdueCharge.single", Some(chargeList.map(_.outstandingAmount).sum.toCurrencyString)))
          }
          case _ => None
        }
      }
    }.getOrElse(("", None))

    val maturity = MaturityLevel.get(oldestCharge.flatMap(_.dueDate))
    val count = chargeList.size
    val dueDate = oldestCharge.flatMap(_.dueDate)

    val dateLabel = (maturity, count) match {
      case (Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueToday) | Some(MaturityLevel.DueEarly), _) => "newHome.yourTasks.selfAssessment.upcomingCharge.label"
      case (_, 1) => "newHome.yourTasks.selfAssessment.overdueCharge.single.label"
      case _ => "newHome.yourTasks.selfAssessment.overdueCharge.multiple.label"
    }

    maturity match {
      case (Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueToday) | Some(MaturityLevel.DueEarly)) => Seq(UpcomingTaskCard(contentDescription._1, linkText, redirectLink, dateLabel, dueDate, contentDescription._2, maturity.get, FINANCIALS))
      case (Some(MaturityLevel.Overdue)) => Seq(OverdueTaskCard(contentDescription._1, linkText, redirectLink, dateLabel, dueDate, contentDescription._2, FINANCIALS))
      case None => Seq.empty
    }
  }

  private def getLppCard(latePaymentPenaltyList: List[ChargeItem], isAgent: Boolean) = {
    val oldestLpp = oldestTransaction(latePaymentPenaltyList)

    val linkText = if(latePaymentPenaltyList.size > 1) "newHome.yourTasks.selfAssessment.lpp.multiple.link" else "newHome.yourTasks.selfAssessment.lpp.single.link"
    val redirectLink = if(latePaymentPenaltyList.size > 1) {
      (latePaymentPenaltyList.head.transactionType, isAgent) match {
        case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, false) => s"${appConfig.incomeTaxPenaltiesFrontend}#lppTab"
        case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, true) => s"${appConfig.incomeTaxPenaltiesFrontend}/agent#lppTab"
        case _ => ""
      }
    } else if (isAgent) {
      controllers.routes.ChargeSummaryController.showAgent(latePaymentPenaltyList.head.taxYear.endYear, latePaymentPenaltyList.head.transactionId).url
    } else {
      controllers.routes.ChargeSummaryController.show(latePaymentPenaltyList.head.taxYear.endYear, latePaymentPenaltyList.head.transactionId).url
    }

    val contentDescription = if(latePaymentPenaltyList.size > 1) {
      ("newHome.yourTasks.selfAssessment.lpp.multiple", Some(latePaymentPenaltyList.size.toString))
    } else {
      ("newHome.yourTasks.selfAssessment.lpp.single", None)
    }

    val dueDateLabel = if (latePaymentPenaltyList.size > 1) {
      ("newHome.yourTasks.selfAssessment.lsp.multiple.label", oldestLpp.get.dueDate)
    } else {
      ("newHome.yourTasks.selfAssessment.lpp.single.label", oldestLpp.get.dueDate)
    }

    val maturity = MaturityLevel.get(oldestLpp.flatMap(_.dueDate))

    maturity match {
      case (Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueToday) | Some(MaturityLevel.DueEarly)) => Seq(UpcomingTaskCard(contentDescription._1, linkText, redirectLink, dueDateLabel._1, dueDateLabel._2, contentDescription._2, maturity.get, PENALTIES))
      case (Some(MaturityLevel.Overdue)) => Seq(OverdueTaskCard(contentDescription._1, linkText, redirectLink, dueDateLabel._1, dueDateLabel._2, contentDescription._2, PENALTIES))
      case None => Seq.empty
    }
  }

  private def getLspCard(lateSubmissionPenaltyList: List[ChargeItem], isAgent: Boolean) = {
    val oldestLsp = oldestTransaction(lateSubmissionPenaltyList)

    val linkText = if(lateSubmissionPenaltyList.size > 1) "newHome.yourTasks.selfAssessment.lsp.multiple.link" else "newHome.yourTasks.selfAssessment.lsp.single.link"
    val redirectLink = if (lateSubmissionPenaltyList.size > 1) {
      (lateSubmissionPenaltyList.head.transactionType, isAgent) match {
        case (LateSubmissionPenalty, false) => s"${appConfig.incomeTaxPenaltiesFrontend}#lspTab"
        case (LateSubmissionPenalty, true)  => s"${appConfig.incomeTaxPenaltiesFrontend}/agent#lspTab"
        case _ => ""
      }
    } else if (isAgent) {
      controllers.routes.ChargeSummaryController.showAgent(lateSubmissionPenaltyList.head.taxYear.endYear, lateSubmissionPenaltyList.head.transactionId).url
    } else {
      controllers.routes.ChargeSummaryController.show(lateSubmissionPenaltyList.head.taxYear.endYear, lateSubmissionPenaltyList.head.transactionId).url
    }

    val contentDescription = if (lateSubmissionPenaltyList.size > 1) {
      ("newHome.yourTasks.selfAssessment.lsp.multiple", Some(lateSubmissionPenaltyList.size.toString))
    } else {
      ("newHome.yourTasks.selfAssessment.lsp.single", None)
    }

    val dueDateLabel = if (lateSubmissionPenaltyList.size > 1) {
      ("newHome.yourTasks.selfAssessment.lsp.multiple.label", oldestLsp.get.dueDate)
    } else {
      ("newHome.yourTasks.selfAssessment.lsp.single.label", oldestLsp.get.dueDate)
    }

    val maturity = MaturityLevel.get(oldestLsp.flatMap(_.dueDate))

    maturity match {
      case (Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueToday) | Some(MaturityLevel.DueEarly)) => Seq(UpcomingTaskCard(contentDescription._1, linkText, redirectLink, dueDateLabel._1, dueDateLabel._2, contentDescription._2, maturity.get, PENALTIES))
      case (Some(MaturityLevel.Overdue)) => Seq(OverdueTaskCard(contentDescription._1, linkText, redirectLink, dueDateLabel._1, dueDateLabel._2, contentDescription._2, PENALTIES))
      case None => Seq.empty
    }
  }

  private def dueChargesByTypes(list: List[ChargeItem], targetTypes: Set[TransactionType]): List[ChargeItem] = list.filter { charge => targetTypes.contains(charge.transactionType) }

  private def oldestTransaction(chargeList: List[ChargeItem]): Option[ChargeItem] = chargeList.collect { case item if item.dueDate.isDefined => item }.minByOption(_.dueDate)
}
