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
import enums.MTDSupportingAgent
import models.financialDetails.*

import java.time.LocalDate
import java.time.temporal.ChronoUnit

case class HandleYourTasksViewModel(outstandingChargesModel: List[ChargeItem],
                                    unpaidCharges: List[FinancialDetailsResponseModel],
                                    creditsRefundsRepayEnabled: Boolean)
                                   (implicit val appConfig: FrontendAppConfig) {
  private val today: LocalDate = LocalDate.now()

  val creditInAccount: Option[BigDecimal] =
    if (creditsRefundsRepayEnabled) {
      Some(unpaidCharges.collectFirst {
        case fdm: FinancialDetailsModel => fdm.balanceDetails.getAbsoluteTotalCreditAmount.getOrElse(BigDecimal(0.00))
      }.getOrElse(BigDecimal(0.00)))
    } else None

  private val oldestCharge: Option[ChargeItem] = outstandingChargesModel
    .collect { case item if item.dueDate.isDefined => item }
    .minByOption(_.dueDate)

  private val chargesSet: Set[TransactionType] = Set(PoaOneDebit, PoaTwoDebit, PoaOneReconciliationDebit, PoaTwoReconciliationDebit,
    BalancingCharge, MfaDebitCharge, ITSAReturnAmendment)
  private val lppSet: Set[TransactionType] = Set(FirstLatePaymentPenalty, SecondLatePaymentPenalty)
  private val lspSet: Set[TransactionType] = Set(LateSubmissionPenalty)


  private val overdueCharges = overdueChargesByTypes(chargesSet)
  private val overdueLpps = overdueChargesByTypes(lppSet)
  private val overdueLsps = overdueChargesByTypes(lspSet)

  private val outstandingAmountOverdueCharges = overdueCharges.map(_.outstandingAmount).sum
  val outstandingAmountLpps: BigDecimal = overdueLpps.map(_.outstandingAmount).sum
  val outstandingAmountLsps: BigDecimal = overdueLsps.map(_.outstandingAmount).sum

   val oldestChargeDate: Option[LocalDate] = oldestCharge.flatMap(_.dueDate)

  private def overdueChargesByTypes(targetTypes: Set[TransactionType]): List[ChargeItem] = {
    outstandingChargesModel.filter { charge =>
      targetTypes.contains(charge.transactionType) &&
        charge.dueDate.exists(_.isBefore(today))
    }
  }


  def oldestChargeStatusColor(dueDate: Option[LocalDate]): String = {
    dueDate match {
      case Some(date) =>
        val daysUntil = ChronoUnit.DAYS.between(today, date)

        if (daysUntil < 0) "red"
        else if (daysUntil == 0) "pink"
        else if (daysUntil <= 30) "yellow"
        else "green"

      case None => "default"
    }
  }


  private def oldestChargeTransactionTypeTaskDescription(isSupportingAgent: Boolean): Option[ParametrisedTaskDescription] = {
    oldestCharge.flatMap { charge =>
      (charge.transactionType, MaturityLevel.get(oldestChargeDate)) match {
        case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge | ITSAReturnAmendment,
        Some(MaturityLevel.Upcoming)) => Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.upcomingCharge", None, None))

        case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge | ITSAReturnAmendment,
        Some(MaturityLevel.Overdue)) => if (overdueCharges.size > 1) Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.overdueCharge.multiple", Some(outstandingAmountOverdueCharges), None))
        else Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.overdueCharge.single", Some(outstandingAmountOverdueCharges), None))

        case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, _) => if(overdueLpps.size > 1) Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.lpp.multiple", None, Some(overdueLpps.size)))
        else Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.lpp.single", None, None))

        case (LateSubmissionPenalty, _) => if(overdueLsps.size > 1) Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.lsp.multiple", None, Some(overdueLsps.size)))
        else Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.lsp.single", None, None))

        case _ => if(isSupportingAgent) Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.supporting.no-tasks", None, None))
        else Some(ParametrisedTaskDescription("newHome.yourTasks.selfAssessment.no-tasks", None, None))
      }
    }
  }

  private def oldestChargeTransactionTypeTaskDateLabel(): Option[ParametrisedTaskDateLabel] = {
    oldestCharge.flatMap { charge =>
      (charge.transactionType, MaturityLevel.get(oldestChargeDate)) match {
        case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge | ITSAReturnAmendment,
        Some(MaturityLevel.Upcoming)) => Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.upcomingCharge.label", charge.dueDate))

        case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge | ITSAReturnAmendment,
        Some(MaturityLevel.Overdue)) => if (overdueCharges.size > 1) Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.overdueCharge.multiple.label", charge.dueDate))
        else Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.overdueCharge.single.label", charge.dueDate))

        case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, _) => if (overdueLpps.size > 1) Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.lpp.multiple.label", charge.dueDate))
        else Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.lpp.single.label", charge.dueDate))

        case (LateSubmissionPenalty, _) => if (overdueLsps.size > 1) Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.lsp.multiple.label", charge.dueDate))
        else Some(ParametrisedTaskDateLabel("newHome.yourTasks.selfAssessment.lsp.single.label", charge.dueDate))

        case _ => None
      }
    }
  }
  enum MaturityLevel:
    case Overdue  extends MaturityLevel()
    case DueToday extends MaturityLevel()
    case DueEarly extends MaturityLevel() // due <= 30 days
    case Upcoming  extends MaturityLevel() // due > 30 days

  object MaturityLevel{
      def get(dueDate: Option[LocalDate]): Option[MaturityLevel] =
        dueDate.map { date =>
          val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), date)
          if (daysUntil < 0) Overdue
          else if (daysUntil == 0) DueToday
          else if (daysUntil <= 30) DueEarly
          else Upcoming
        }
    }

  private case class ParametrisedTaskDescription(content: String, amount: Option[BigDecimal], count: Option[Int])

  private case class ParametrisedTaskDateLabel(content: String, date: Option[LocalDate])

  def getParametrisedContent(user: auth.MtdItUser[_]): (String, String) = {
    val isSupportingAgent = user.usersRole == MTDSupportingAgent
    oldestChargeTransactionTypeTaskDescription(isSupportingAgent).map(content => {
      (content.content,
        if(content.count.isDefined) content.count.get.toString
        else if(content.amount.isDefined) content.amount.get.toString() else "")
    }).getOrElse(if(isSupportingAgent) "newHome.yourTasks.selfAssessment.supporting.no-tasks" else "newHome.yourTasks.selfAssessment.no-tasks", "")
  }

  def getParametrisedTaskDateLabel: (String, LocalDate) = {
    val test = oldestChargeTransactionTypeTaskDateLabel().map( taskDateLabel => {
      (taskDateLabel.content,
        if(taskDateLabel.date.isDefined) taskDateLabel.date.get else LocalDate.now())
    }).getOrElse("", LocalDate.now())
    test
  }

   private def defineRedirectLinkPayments(isAgent: Boolean): Option[String] = {
      oldestCharge.flatMap { charge =>
        (charge.transactionType, isAgent) match {
          case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge | ITSAReturnAmendment, false) => Some("/report-quarterly/income-and-expenses/view/what-you-owe")
          case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge | ITSAReturnAmendment, true) => Some("/report-quarterly/income-and-expenses/view/agents/what-your-client-owes")

          case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, false) => if (overdueLpps.size > 1) Some(getLPP1orLPP2link(overdueLpps.head, isAgent))
          else Some(s"/report-quarterly/income-and-expenses/view/tax-years/${overdueLpps.head.taxYear.endYear}/charge?id=${overdueLpps.head.transactionId}")
          case (FirstLatePaymentPenalty | SecondLatePaymentPenalty, true) => if (overdueLpps.size > 1) Some(getLPP1orLPP2link(overdueLpps.head, isAgent))
          else Some(s"/report-quarterly/income-and-expenses/view/agents/tax-years/${overdueLpps.head.taxYear.endYear}/charge?id=${overdueLpps.head.transactionId}")


          case (LateSubmissionPenalty, false) => if (overdueLsps.size > 1) Some(getLPP1orLPP2link(overdueLsps.head, isAgent))
          else Some(s"/report-quarterly/income-and-expenses/view/tax-years/${overdueLsps.head.taxYear.endYear}/charge?id=${overdueLsps.head.transactionId}")
          case (LateSubmissionPenalty, true) => if (overdueLsps.size > 1) Some(getLPP1orLPP2link(overdueLsps.head, isAgent))
          else Some(s"/report-quarterly/income-and-expenses/view/agents/tax-years/${overdueLsps.head.taxYear.endYear}/charge?id=${overdueLsps.head.transactionId})")
        }
      }
    }

   private def defineRedirectLinkMoneyInYourAccount(isAgent: Boolean) = {
     if(isAgent){
       "/report-quarterly/income-and-expenses/view/agents/money-in-your-account"
     }else {
       "/report-quarterly/income-and-expenses/view/money-in-your-account"
     }
   }

  val getRedirectLinkPayments: Boolean => Option[String] = (isAgent: Boolean) => defineRedirectLinkPayments(isAgent)
  val getRedirectLinkMoneyInAccount: Boolean => String = (isAgent: Boolean) => defineRedirectLinkMoneyInYourAccount(isAgent)

  private def getLPP1orLPP2link(chargeItem: ChargeItem, isAgent: Boolean): String = {
    (chargeItem.transactionType, isAgent )match {
      case( FirstLatePaymentPenalty, false) => appConfig.incomeTaxPenaltiesFrontendLPP1Calculation(chargeItem.chargeReference.getOrElse(""))
      case( FirstLatePaymentPenalty, true) => appConfig.incomeTaxPenaltiesFrontendLPP1CalculationAgent(chargeItem.chargeReference.getOrElse(""))
      case (SecondLatePaymentPenalty, false) => appConfig.incomeTaxPenaltiesFrontendLPP2Calculation(chargeItem.chargeReference.getOrElse(""))
      case (SecondLatePaymentPenalty, true) => appConfig.incomeTaxPenaltiesFrontendLPP2CalculationAgent(chargeItem.chargeReference.getOrElse(""))
      case _ => ""
    }
  }
}

