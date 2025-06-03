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

package utils

import controllers.routes._
import enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage, YourSelfAssessmentChargeSummaryPage}

trait FallBackBackLinks {

  def getPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String], isAgent: Boolean): String =
    if (isAgent) getAgentPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt)
    else getIndividualPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt, origin)

  private def getIndividualPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String]): String =
    ((gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) => TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).withFragment("payments")
      case (Some(TaxYearSummaryPage), None) => HomeController.show(origin)
      case (Some(WhatYouOwePage), _) => WhatYouOweController.show(origin)
      case (Some(YourSelfAssessmentChargeSummaryPage), _) => YourSelfAssessmentChargesController.show(origin)
      case (Some(PaymentHistoryPage), _) => PaymentHistoryController.show(origin)
      case _ => HomeController.show(origin)
    }).path

  private def getAgentPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int]): String =
    ((gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) => TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).withFragment("payments")
      case (Some(TaxYearSummaryPage), None) => HomeController.showAgent()
      case (Some(WhatYouOwePage), _) => WhatYouOweController.showAgent()
      case (Some(YourSelfAssessmentChargeSummaryPage), _) => YourSelfAssessmentChargesController.showAgent()
      case (Some(PaymentHistoryPage), _) => PaymentHistoryController.showAgent()
      case _ => HomeController.showAgent()
    }).path

  def getChargeSummaryBackUrl(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isAgent: Boolean): String =
    if (isAgent) getChargeSummaryBackUrlAgent(gatewayPageOpt, taxYear)
    else getChargeSummaryBackUrlIndividual(gatewayPageOpt, taxYear, origin)

  private def getChargeSummaryBackUrlAgent(gatewayPageOpt: Option[GatewayPage], taxYear: Int): String =
    (gatewayPageOpt match {
      case Some(TaxYearSummaryPage) => TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).withFragment("payments")
      case Some(WhatYouOwePage) => WhatYouOweController.showAgent()
      case Some(YourSelfAssessmentChargeSummaryPage) => YourSelfAssessmentChargesController.showAgent()
      case Some(PaymentHistoryPage) => PaymentHistoryController.showAgent()
      case _ => HomeController.showAgent()
    }).path

  private def getChargeSummaryBackUrlIndividual(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String]): String =
    (gatewayPageOpt match {
      case Some(TaxYearSummaryPage) => TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).withFragment("payments")
      case Some(WhatYouOwePage) => WhatYouOweController.show(origin)
      case Some(YourSelfAssessmentChargeSummaryPage) => YourSelfAssessmentChargesController.show(origin)
      case Some(PaymentHistoryPage) => PaymentHistoryController.show(origin)
      case _ => HomeController.show(origin)
    }).path
}
