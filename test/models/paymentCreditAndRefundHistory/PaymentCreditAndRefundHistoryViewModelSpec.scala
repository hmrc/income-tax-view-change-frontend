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

import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

object PaymentCreditAndRefundHistoryViewModelSpec extends Properties("String") {


  implicit val messages: Messages = stubMessages()



  val messageKeys: Gen[(Boolean, Boolean, String)] = Gen.oneOf(
    (false, true, "paymentHistory.paymentAndRefundHistory.heading"),
    (true, false, "paymentHistory.paymentAndCreditHistory"),
    (true, true, "paymentHistory.paymentCreditAndRefundHistory.heading"),
    (false, false, "paymentHistory.heading")
  )

  property("Appropriate message key is returned for all combinations") = forAll(messageKeys) {
    case (creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled, expectedKey) =>
      val model = PaymentCreditAndRefundHistoryViewModel(creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled)
      model.title() == messages(expectedKey)
  }

}
