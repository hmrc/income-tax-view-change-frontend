/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import assets.PaymentAllocationChargesTestConstants.{documentDetail, financialDetail}
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import testUtils.ViewSpec
import views.html.agent.PaymentAllocation

class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val paymentAllocation: PaymentAllocation = app.injector.instanceOf[PaymentAllocation]

  lazy val backUrl: String = controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url


  object paymentAllocationMessages {
    val title = "Payment made to HMRC - Your client’s Income Tax details - GOV.UK"
    val heading = "Payment made to HMRC"
    val backLink = "Back"
    val date = "31 January 2021"
    val amount = "£300.00"
    val info = "Any payments made will automatically be allocated towards penalties and earlier tax years before current and future tax years."
  }

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  class PaymentAllocationSetup extends Setup(
    paymentAllocation(singleTestPaymentAllocationCharge, mockImplicitDateFormatter, backUrl)
  )

  "Payment Allocation Page" should {
    s"have the title: ${paymentAllocationMessages.title}" in new PaymentAllocationSetup {
      document.title() shouldBe paymentAllocationMessages.title
    }

    s"have the heading: ${paymentAllocationMessages.heading}" in new PaymentAllocationSetup {
      document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
    }

    "have a back link" in new PaymentAllocationSetup {
      document.backLink.text shouldBe paymentAllocationMessages.backLink
      document.hasBackLinkTo(controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url)
    }
    s"have the correct date of ${paymentAllocationMessages.date}" in new PaymentAllocationSetup {
      document.getElementsByTag("td").eq(1).text shouldBe paymentAllocationMessages.date
    }

    s"have the correct Amount of ${paymentAllocationMessages.amount}" in new PaymentAllocationSetup {
      document.getElementsByTag("td").last.text shouldBe paymentAllocationMessages.amount
    }

    "have info text" in new PaymentAllocationSetup {
      document.getElementById("payments-allocation-info").text shouldBe paymentAllocationMessages.info
    }
  }

}
