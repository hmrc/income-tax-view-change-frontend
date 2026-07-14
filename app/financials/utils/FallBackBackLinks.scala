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

package financials.utils

import common.config.FrontendAppConfig
import common.enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import financials.controllers.routes as financialsRoutes

trait FallBackBackLinks {

  val appConfig: FrontendAppConfig
  val returnsFrontendEnabled: Boolean

  def getPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage],
                                  taxYearOpt: Option[Int],
                                  origin: Option[String],
                                  isAgent: Boolean): String =
    if (isAgent) getAgentPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt)
    else getIndividualPaymentAllocationBackUrl(gatewayPageOpt, taxYearOpt, origin)

  private def getIndividualPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage],
                                                    taxYearOpt: Option[Int],
                                                    origin: Option[String]): String =
    (gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) =>
        appConfig.returnsTaxYearSummaryIndividualUrl(taxYear, origin, Some("payments"), returnsFrontendEnabled)
      case (Some(TaxYearSummaryPage), None) => appConfig.individualHomeUrlWithOrigin(origin)
      case (Some(WhatYouOwePage), _) => financialsRoutes.WhatYouOweController.show(origin).path
      case (Some(PaymentHistoryPage), _) => financialsRoutes.PaymentHistoryController.show(origin).path
      case _ => appConfig.individualHomeUrlWithOrigin(origin)
    }

  private def getAgentPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage],
                                               taxYearOpt: Option[Int]): String =
    (gatewayPageOpt, taxYearOpt) match {
      case (Some(TaxYearSummaryPage), Some(taxYear)) =>
        appConfig.returnsTaxYearSummaryAgentUrl(taxYear, Some("payments"), returnsFrontendEnabled)
      case (Some(TaxYearSummaryPage), None) => appConfig.homePageUrl(true)
      case (Some(WhatYouOwePage), _) => financialsRoutes.WhatYouOweController.showAgent().path
      case (Some(PaymentHistoryPage), _) => financialsRoutes.PaymentHistoryController.showAgent().path
      case _ => appConfig.homePageUrl(true)
    }

  def getChargeSummaryBackUrl(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isAgent: Boolean): String =
    if (isAgent) getChargeSummaryBackUrlAgent(gatewayPageOpt, taxYear)
    else getChargeSummaryBackUrlIndividual(gatewayPageOpt, taxYear, origin)

  private def getChargeSummaryBackUrlAgent(gatewayPageOpt: Option[GatewayPage], taxYear: Int): String =
    gatewayPageOpt match {
      case Some(TaxYearSummaryPage) =>
        appConfig.returnsTaxYearSummaryAgentUrl(taxYear, Some("payments"), returnsFrontendEnabled)
      case Some(WhatYouOwePage) => financialsRoutes.WhatYouOweController.showAgent().path
      case Some(PaymentHistoryPage) => financialsRoutes.PaymentHistoryController.showAgent().path
      case _ => appConfig.homePageUrl(true)
    }

  private def getChargeSummaryBackUrlIndividual(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String]): String =
    gatewayPageOpt match {
      case Some(TaxYearSummaryPage) =>
        appConfig.returnsTaxYearSummaryIndividualUrl(taxYear, origin, Some("payments"), returnsFrontendEnabled)
      case Some(WhatYouOwePage) => financialsRoutes.WhatYouOweController.show(origin).path
      case Some(PaymentHistoryPage) => financialsRoutes.PaymentHistoryController.show(origin).path
      case _ => appConfig.individualHomeUrlWithOrigin(origin)
    }
}
