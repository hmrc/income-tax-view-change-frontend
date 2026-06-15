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

package common.utils

import common.enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import financials.controllers.routes as financialsRoutes
import hub.controllers.routes.*
import returns.controllers.routes as returnsRoutes

trait FallBackBackLinks {

  def getPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String], isAgent: Boolean): String =
    if (isAgent) getAgentPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt)
    else getIndividualPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt, origin)

  private def getIndividualPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String]): String =
    ((gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) => returnsRoutes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).withFragment("payments")
      case (Some(TaxYearSummaryPage), None) => HomeController.show(origin)
      case (Some(WhatYouOwePage), _) => financialsRoutes.WhatYouOweController.show(origin)
      case (Some(PaymentHistoryPage), _) => financialsRoutes.PaymentHistoryController.show(origin)
      case _ => HomeController.show(origin)
    }).path

  private def getAgentPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int]): String =
    ((gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) => returnsRoutes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).withFragment("payments")
      case (Some(TaxYearSummaryPage), None) => HomeController.showAgent()
      case (Some(WhatYouOwePage), _) => financialsRoutes.WhatYouOweController.showAgent()
      case (Some(PaymentHistoryPage), _) => financialsRoutes.PaymentHistoryController.showAgent()
      case _ => HomeController.showAgent()
    }).path

  def getChargeSummaryBackUrl(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isAgent: Boolean): String =
    if (isAgent) getChargeSummaryBackUrlAgent(gatewayPageOpt, taxYear)
    else getChargeSummaryBackUrlIndividual(gatewayPageOpt, taxYear, origin)

  private def getChargeSummaryBackUrlAgent(gatewayPageOpt: Option[GatewayPage], taxYear: Int): String =
    (gatewayPageOpt match {
      case Some(TaxYearSummaryPage) => returnsRoutes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).withFragment("payments")
      case Some(WhatYouOwePage) => financialsRoutes.WhatYouOweController.showAgent()
      case Some(PaymentHistoryPage) => financialsRoutes.PaymentHistoryController.showAgent()
      case _ => HomeController.showAgent()
    }).path

  private def getChargeSummaryBackUrlIndividual(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String]): String =
    (gatewayPageOpt match {
      case Some(TaxYearSummaryPage) => returnsRoutes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).withFragment("payments")
      case Some(WhatYouOwePage) => financialsRoutes.WhatYouOweController.show(origin)
      case Some(PaymentHistoryPage) => financialsRoutes.PaymentHistoryController.show(origin)
      case _ => HomeController.show(origin)
    }).path
}
