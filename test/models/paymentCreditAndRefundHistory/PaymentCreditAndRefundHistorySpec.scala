/*
 * Copyright 2024 HM Revenue & Customs
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

package models.paymentCreditAndRefundHistory

import controllers.PaymentHistoryController
import org.scalacheck.Gen
import org.scalatestplus.play._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._

class PaymentCreditAndRefundHistorySpec extends PlaySpec with ScalaCheckPropertyChecks {

  private val application = new GuiceApplicationBuilder().build()
  private val controller = application.injector.instanceOf[PaymentHistoryController]

  private val viewModelGen: Gen[PaymentCreditAndRefundHistoryViewModel] = for {
    creditsRefundsRepayEnabled <- Gen.oneOf(true, false)
    paymentHistoryAndRefundsEnabled <- Gen.oneOf(true, false)
  } yield PaymentCreditAndRefundHistoryViewModel(creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled)

  "PaymentHistoryController" should {
    "return 200 OK when rendering PaymentHistory view" in forAll(viewModelGen) { viewModel =>
      val request = FakeRequest(GET, "/payment-refund-history")

      val result = controller.show().apply(request)

      status(result) mustBe OK
    }
  }
}
