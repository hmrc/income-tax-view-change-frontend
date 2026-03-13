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

package models.newHomePage

import config.FrontendAppConfig
import models.creditsandrefunds.CreditsModel
import models.financialDetails.*

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HandleYourTasksViewModel(outstandingChargesModel: List[ChargeItem],
                               unpaidCharges: List[FinancialDetailsResponseModel],
                               credits: CreditsModel,
                               creditsRefundsRepayEnabled: Boolean)
                              (implicit val appConfig: FrontendAppConfig) {

  private val today: LocalDate = LocalDate.now()

   val chargesSet: Set[TransactionType] = Set(PoaOneDebit, PoaTwoDebit, PoaOneReconciliationDebit, PoaTwoReconciliationDebit,
    BalancingCharge, MfaDebitCharge, ITSAReturnAmendment)
   val lppSet: Set[TransactionType] = Set(FirstLatePaymentPenalty, SecondLatePaymentPenalty)
   val lspSet: Set[TransactionType] = Set(LateSubmissionPenalty)

  val areChargesPresent: Boolean = chargeItemByType(chargesSet).nonEmpty
  val areLPPPresent: Boolean = chargeItemByType(lppSet).nonEmpty
  val isLSPPresent: Boolean = chargeItemByType(lspSet).nonEmpty
  val isMoneyInYourAccountPresent = creditInAccount.isDefined && creditInAccount.get > BigDecimal(0)

  val areAnyTasksPresent: Boolean = areChargesPresent || areLPPPresent || isLSPPresent || isMoneyInYourAccountPresent

  enum MaturityLevel:
    case Overdue extends MaturityLevel()
    case DueToday extends MaturityLevel()
    case DueEarly extends MaturityLevel() // due <= 30 days
    case Upcoming extends MaturityLevel() // due > 30 days

  object MaturityLevel {
    def get(dueDate: Option[LocalDate]): Option[MaturityLevel] =
      dueDate.map { date =>
        val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), date)
        if (daysUntil < 0) Overdue
        else if (daysUntil == 0) DueToday
        else if (daysUntil <= 30) DueEarly
        else Upcoming
      }
  }

  def creditInAccount: Option[BigDecimal] = if (creditsRefundsRepayEnabled) {
    Some(credits.totalCredit)
  } else {
    None
  }

  private def chargeItemByType(targetTypes: Set[TransactionType]): List[ChargeItem] = {
    outstandingChargesModel.filter { charge =>
      targetTypes.contains(charge.transactionType)
    }
  }
  private def overDueCharges(): List[ChargeItem] = {
    outstandingChargesModel.filter { charge =>
      chargesSet.contains(charge.transactionType) &&
        charge.dueDate.exists(_.isBefore(today))
    }
  }

  private def oldestTransactionByType(targetTypes: Set[TransactionType]): Option[ChargeItem] = outstandingChargesModel
    .filter(item => targetTypes.contains(item.transactionType))
    .collect { case item if item.dueDate.isDefined => item }
    .minByOption(_.dueDate)


  def getLPPTaskDescription: (String, Option[Int]) ={
    if(chargeItemByType(lppSet).size > 1){
      ("newHome.yourTasks.selfAssessment.lpp.multiple", Some(chargeItemByType(lppSet).size))
    }else {
      ("newHome.yourTasks.selfAssessment.lpp.single", None)
    }
  }

  def getLSPTaskDescription: (String, Option[Int]) = {
    if (chargeItemByType(lspSet).size > 1) {
      ("newHome.yourTasks.selfAssessment.lsp.multiple", Some(chargeItemByType(lspSet).size))
    } else {
      ("newHome.yourTasks.selfAssessment.lsp.single", None)
    }
  }

  def getChargesTaskDescription: Option[(String, Option[BigDecimal])] = {
    val oldestCharges =  oldestTransactionByType(chargesSet)
    val charges = chargeItemByType(chargesSet)
    oldestCharges.flatMap( charge => {
        MaturityLevel.get(charge.dueDate) match {
          case Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueEarly) | Some(MaturityLevel.DueToday) => Some(("newHome.yourTasks.selfAssessment.upcomingCharge", None))
          case Some(MaturityLevel.Overdue) => if(overDueCharges().size > 1){
            Some(("newHome.yourTasks.selfAssessment.overdueCharge.multiple", Some(charges.map(_.outstandingAmount).sum)))
          }else{
            Some(("newHome.yourTasks.selfAssessment.overdueCharge.single", Some(charges.map(_.outstandingAmount).sum)))
          }
          case _ => None
          }
        }
      )
  }

  def getLSPDateLabel: (String, Option[LocalDate]) = {
    if (chargeItemByType(lspSet).size > 1) {
      ("newHome.yourTasks.selfAssessment.lsp.multiple.label", oldestTransactionByType(lspSet).get.dueDate)
    } else {
      ("newHome.yourTasks.selfAssessment.lsp.single.label", oldestTransactionByType(lspSet).get.dueDate)
    }
  }

  def getLPPDateLabel: (String, Option[LocalDate]) = {
    if (chargeItemByType(lppSet).size > 1) {
      ("newHome.yourTasks.selfAssessment.lsp.multiple.label", oldestTransactionByType(lppSet).get.dueDate)
    } else {
      ("newHome.yourTasks.selfAssessment.lpp.single.label", oldestTransactionByType(lppSet).get.dueDate)
    }
  }

  def getChargeDateLabel: (String, Option[LocalDate]) = {
    val oldestCharge = oldestTransactionByType(chargesSet)
    val maturity = MaturityLevel.get(oldestCharge.flatMap(_.dueDate))
    val count = chargeItemByType(chargesSet).size

    val dueDate = oldestCharge.flatMap(_.dueDate)

    val label = (maturity, count) match {
      case (Some(MaturityLevel.Upcoming) | Some(MaturityLevel.DueToday) | Some(MaturityLevel.DueEarly), _) => "newHome.yourTasks.selfAssessment.upcomingCharge.label"
      case (_, 1)                            => "newHome.yourTasks.selfAssessment.overdueCharge.single.label"
      case _                                 => "newHome.yourTasks.selfAssessment.overdueCharge.multiple.label"
    }

    (label, dueDate)
  }

  val getMoneyInYourAccountTaskDescription: (String, Option[BigDecimal]) = ("newHome.yourTasks.selfAssessment.money-in-account", creditInAccount)


  def oldestChargeStatusColor(transactionType: Set[TransactionType]): String = {
    val maturityLevel = MaturityLevel.get(oldestTransactionByType(transactionType).flatMap(_.dueDate))
    maturityLevel match {
      case Some(MaturityLevel.Upcoming) => "green"

      case Some(MaturityLevel.Overdue) => "red"

      case Some(MaturityLevel.DueEarly) => "yellow"

      case Some(MaturityLevel.DueToday) => "pink"

      case None => "white"
    }
  }


   def getLpp1orLpp2Link(isAgent: Boolean): String = {
    val lppCharges = chargeItemByType(lppSet)
    if(lppCharges.size > 1){
      defineLppLink(lppCharges.head, lppCharges.head.transactionType, isAgent)
    }else {
      if(isAgent){
        controllers.routes.ChargeSummaryController.showAgent(lppCharges.head.taxYear.endYear, lppCharges.head.transactionId).url
      }else{
        controllers.routes.ChargeSummaryController.show(lppCharges.head.taxYear.endYear, lppCharges.head.transactionId).url
      }
    }
  }
  private def defineLppLink(chargeItem: ChargeItem, transactionType: TransactionType, isAgent: Boolean): String = {
      (transactionType, isAgent) match {
        case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, false) => s"${appConfig.incomeTaxPenaltiesFrontend}#lppTab"
        case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, true) => s"${appConfig.incomeTaxPenaltiesFrontend}/agent#lppTab"
        case _ => ""
      }
    }

  private def defineLspLink(chargeItem: ChargeItem, transactionType: TransactionType, isAgent: Boolean): String = {
    (transactionType, isAgent) match {
      case (LateSubmissionPenalty, false) => s"${appConfig.incomeTaxPenaltiesFrontend}#lspTab"
      case (LateSubmissionPenalty, true) => s"${appConfig.incomeTaxPenaltiesFrontend}/agent#lspTab"
      case _ => ""
    }
  }

   def getLspLink(isAgent: Boolean): String = {
    val lspCharges = chargeItemByType(lspSet)
    if (lspCharges.size > 1) {
      defineLspLink(lspCharges.head, lspCharges.head.transactionType, isAgent)
    } else {
      if (isAgent) {
        controllers.routes.ChargeSummaryController.showAgent(lspCharges.head.taxYear.endYear, lspCharges.head.transactionId).url
      } else {
        controllers.routes.ChargeSummaryController.show(lspCharges.head.taxYear.endYear, lspCharges.head.transactionId).url
      }
    }
  }

  def getChargesPaymentsLink(isAgent: Boolean): String = {
    if(isAgent){
      controllers.routes.WhatYouOweController.showAgent().url
    }else{
      controllers.routes.WhatYouOweController.show().url
    }
  }

  def getMoneyInYourAccountLink(isAgent: Boolean): String = {
      if (isAgent) {
        controllers.routes.MoneyInYourAccountController.showAgent().url
      } else {
        controllers.routes.MoneyInYourAccountController.show().url
      }
    }

  def getNoTasksLabel(isSupportingAgent: Boolean): String = {
    if(isSupportingAgent){
      "newHome.yourTasks.selfAssessment.supporting.no-tasks"
    }else {
      "newHome.yourTasks.selfAssessment.no-tasks"
    }
  }

 def getLspLinkLabel = {
   val lspCharges = chargeItemByType(lspSet)

   if(lspCharges.size > 1){
     "newHome.yourTasks.selfAssessment.lsp.multiple.link"
   }else {
     "newHome.yourTasks.selfAssessment.lsp.single.link"
   }
 }

  def getLppLinkLabel = {
    val lppCharges = chargeItemByType(lppSet)

    if (lppCharges.size > 1) {
      "newHome.yourTasks.selfAssessment.lpp.multiple.link"
    } else {
      "newHome.yourTasks.selfAssessment.lpp.single.link"
    }
  }
}
