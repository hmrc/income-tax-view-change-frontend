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

  def getPaymentAllocationBackUrl(isAgent: Boolean,
                                  gatewayPageOpt: Option[GatewayPage],
                                  taxYearOpt: Option[Int],
                                  origin: Option[String],
                                  isReturnsFrontendEnabled: Boolean = false): String =
    (gatewayPageOpt, taxYearOpt) match
      case (Some(TaxYearSummaryPage), Some(taxYear)) =>
        appConfig.taxYearSummaryUrl(isAgent, taxYear, origin, Some("payments"), isReturnsFrontendEnabled)
      case (Some(TaxYearSummaryPage), None) => appConfig.homePageUrl(isAgent, origin)
      case (Some(WhatYouOwePage), _) => whatYouOweUrl(isAgent, origin)
      case (Some(PaymentHistoryPage), _) => paymentHistoryUrl(isAgent, origin)
      case _ => appConfig.homePageUrl(isAgent, origin)

  def getChargeSummaryBackUrl(isAgent: Boolean, gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isReturnsFrontendEnabled: Boolean = false): String =
    gatewayPageOpt match
      case Some(TaxYearSummaryPage) =>
        appConfig.taxYearSummaryUrl(isAgent, taxYear, origin, Some("payments"), isReturnsFrontendEnabled)
      case Some(WhatYouOwePage) => whatYouOweUrl(isAgent, origin)
      case Some(PaymentHistoryPage) => paymentHistoryUrl(isAgent, origin)
      case _ => appConfig.homePageUrl(isAgent, origin)

  private def whatYouOweUrl(isAgent: Boolean, origin: Option[String]): String =
    if isAgent then financialsRoutes.WhatYouOweController.showAgent().path
    else financialsRoutes.WhatYouOweController.show(origin).path

  private def paymentHistoryUrl(isAgent: Boolean, origin: Option[String]): String =
    if isAgent then financialsRoutes.PaymentHistoryController.showAgent().path
    else financialsRoutes.PaymentHistoryController.show(origin).path

}
