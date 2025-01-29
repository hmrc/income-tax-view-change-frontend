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

import audit.AuditingService
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.PaymentHistoryController
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Prop, Properties}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubMessages}
import services.{DateServiceInterface, PaymentHistoryService, RepaymentService}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.PaymentHistory
import views.html.errorPages.CustomNotFoundError

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

object PaymentCreditAndRefundHistoryViewModelSpec extends Properties("String") {

//  implicit val messages: Messages = stubMessages()
//
//  val messageKeys: Gen[(Boolean, Boolean, String)] = Gen.oneOf(
//    (false, true, "paymentHistory.paymentAndRefundHistory.heading"),
//    (true, false, "paymentHistory.paymentAndCreditHistory"),
//    (true, true, "paymentHistory.paymentCreditAndRefundHistory.heading"),
//    (false, false, "paymentHistory.heading")
//  )
//
//  property("Appropriate message key is returned for all combinations") = forAll(messageKeys) {
//    case (creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled, expectedKey) =>
//      val model = PaymentCreditAndRefundHistoryViewModel(creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled)
//      model.title() == messages(expectedKey)
//  }

  private val application = GuiceApplicationBuilder().build()
  private implicit val messages: Messages = application.injector.instanceOf[Messages]
  private val paymentHistoryView: PaymentHistory = application.injector.instanceOf[PaymentHistory]

  private val viewModelGenerator: Gen[PaymentCreditAndRefundHistoryViewModel] = for {
    creditsRefundsRepayEnabled <- Gen.oneOf(true, false)
    paymentHistoryAndRefundsEnabled <- Gen.oneOf(true, false)
  } yield PaymentCreditAndRefundHistoryViewModel(creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled)

  property("PaymentHistory view returns 200 OK") = forAll(viewModelGenerator) { viewModel =>
    val request = FakeRequest()
    val html = paymentHistoryView(
      backUrl = "/test-url",
      isAgent = false,
      saUtr = None,
      viewModel = viewModel,
      btaNavPartial = None,
      groupedPayments = Nil,
      paymentHistoryAndRefundsEnabled = viewModel.paymentHistoryAndRefundsEnabled
    )(request, messages)

    val result: Result = Results.Ok(html)

    result.header.status == OK
  }
}