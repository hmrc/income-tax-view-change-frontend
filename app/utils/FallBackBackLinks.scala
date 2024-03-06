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
    ((gatewayPageOpt, taxYearOpt, isAgent) match {
      case (Some(TaxYearSummaryPage), Some(taxYear), true) => routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).withFragment("payments")
      case (Some(TaxYearSummaryPage), Some(taxYear),    _) => routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).withFragment("payments")
      case (Some(PaymentHistoryPage), _,             true) => routes.PaymentHistoryController.showAgent
      case (Some(PaymentHistoryPage), _,                _) => routes.PaymentHistoryController.show(origin)
      case (Some(WhatYouOwePage),     _,             true) => routes.WhatYouOweController.showAgent
      case (Some(WhatYouOwePage),     _,                _) => routes.WhatYouOweController.show(origin)
      case (_,                        _,             true) => routes.HomeController.showAgent
      case (_,                        _,                _) => routes.HomeController.show(origin)
    }).path

  def getChargeSummaryBackUrl(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isAgent: Boolean): String =
    ((gatewayPageOpt, isAgent) match {
      case (Some(TaxYearSummaryPage), true) => routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).withFragment("payments")
      case (Some(TaxYearSummaryPage),    _) => routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).withFragment("payments")
      case (Some(PaymentHistoryPage), true) => routes.PaymentHistoryController.showAgent
      case (Some(PaymentHistoryPage),    _) => routes.PaymentHistoryController.show(origin)
      case (Some(WhatYouOwePage),     true) => routes.WhatYouOweController.showAgent
      case (Some(WhatYouOwePage),        _) => routes.WhatYouOweController.show(origin)
      case (_,                        true) => routes.HomeController.showAgent
      case (_,                           _) => routes.HomeController.show(origin)
    }).path
}
