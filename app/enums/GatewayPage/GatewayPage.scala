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

package enums.GatewayPage

sealed trait GatewayPage {
  val name: String
}

object GatewayPage {
  def apply(page: String): GatewayPage = {
    page match {
      case WhatYouOwePage.name => WhatYouOwePage
      case PaymentHistoryPage.name => PaymentHistoryPage
      case TaxYearSummaryPage.name => TaxYearSummaryPage
      case _ => NoMatch
    }
  }
}

case object WhatYouOwePage extends GatewayPage {
  val name = "whatYouOwe"
}

case object PaymentHistoryPage extends GatewayPage {
  val name = "paymentHistory"
}

case object TaxYearSummaryPage extends GatewayPage {
  val name = "taxYearSummary"
}

case object NoMatch extends GatewayPage {
  val name = "nomatch"
}
