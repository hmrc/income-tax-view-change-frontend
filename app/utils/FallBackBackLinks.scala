/*
 * Copyright 2022 HM Revenue & Customs
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

import enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}

trait FallBackBackLinks {

  def getPaymentAllocationBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String], isAgent: Boolean): String = (gatewayPageOpt, taxYearOpt) match {
    case (Some(TaxYearSummaryPage), Some(taxYear)) =>
      if (isAgent) controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url + "#payments"
      else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url + "#payments"
    case (Some(TaxYearSummaryPage), None) =>
      if (isAgent) controllers.routes.HomeController.showAgent().url
      else controllers.routes.HomeController.show(origin).url
    case (Some(WhatYouOwePage), _) =>
      if (isAgent) controllers.routes.WhatYouOweController.showAgent().url
      else controllers.routes.WhatYouOweController.show(origin).url
    case (Some(PaymentHistoryPage), _) =>
      if (isAgent) controllers.routes.PaymentHistoryController.showAgent().url
      else controllers.routes.PaymentHistoryController.show(origin).url
    case _ =>
      if (isAgent) controllers.routes.HomeController.showAgent().url
      else controllers.routes.HomeController.show(origin).url
  }

  def getChargeSummaryBackUrl(gatewayPageOpt: Option[GatewayPage], taxYear: Int, origin: Option[String], isAgent: Boolean): String = gatewayPageOpt match {
    case Some(TaxYearSummaryPage) => if (isAgent) controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url + "#payments"
    else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url + "#payments"
    case Some(WhatYouOwePage) => if (isAgent) controllers.routes.WhatYouOweController.showAgent().url
    else controllers.routes.WhatYouOweController.show(origin).url
    case Some(PaymentHistoryPage) => if (isAgent) controllers.routes.PaymentHistoryController.showAgent().url
    else controllers.routes.PaymentHistoryController.show(origin).url
    case _ => if (isAgent) controllers.routes.HomeController.showAgent().url
    else controllers.routes.HomeController.show(origin).url
  }

}
