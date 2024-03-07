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

import controllers.routes
import enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}

trait FallBackBackLinks {

  def getPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String], isAgent: Boolean): String =
    if (isAgent) getAgentPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt)
    else         getIndividualPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt, origin)

  def getIndividualPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String]): String =
    (gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear))  => routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url + "#payments"
      case (Some(TaxYearSummaryPage), None)           => routes.HomeController.show(origin).url
      case (Some(WhatYouOwePage),     _)              => routes.WhatYouOweController.show(origin).url
      case (Some(PaymentHistoryPage), _)              => routes.PaymentHistoryController.show(origin).url
      case _                                          => routes.HomeController.show(origin).url
    }

  def getAgentPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int]): String =
    (gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) => routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url + "#payments"
      case (Some(TaxYearSummaryPage), None)          => routes.HomeController.showAgent.url
      case (Some(WhatYouOwePage),     _)             => routes.WhatYouOweController.showAgent.url
      case (Some(PaymentHistoryPage), _)             => routes.PaymentHistoryController.showAgent.url
      case _                                         => routes.HomeController.showAgent.url
    }

  def getChargeSummaryBackUrl(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isAgent: Boolean): String = {
    if (isAgent)
      getChargeSummaryBackUrlAgent(gatewayPageOpt, taxYear)
    else
      getChargeSummaryBackUrlIndividual(gatewayPageOpt, taxYear, origin)
  }

  private def getChargeSummaryBackUrlAgent(gatewayPageOpt: Option[GatewayPage], taxYear: Int) = {
    gatewayPageOpt match {
      case Some(TaxYearSummaryPage)   => routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url + "#payments"
      case Some(WhatYouOwePage)       => routes.WhatYouOweController.showAgent.url
      case Some(PaymentHistoryPage)   => routes.PaymentHistoryController.showAgent.url
      case _                          => routes.HomeController.showAgent.url
    }
  }

  private def getChargeSummaryBackUrlIndividual(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String]) = {
    gatewayPageOpt match {
      case Some(TaxYearSummaryPage)   => routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url + "#payments"
      case Some(WhatYouOwePage)       => routes.WhatYouOweController.show(origin).url
      case Some(PaymentHistoryPage)   => routes.PaymentHistoryController.show(origin).url
      case _                          => routes.HomeController.show(origin).url
    }
  }
}
