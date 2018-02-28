/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers


import play.api.http.Status
import config.FrontendAppConfig
import controllers.predicates.SessionTimeoutPredicate
import mocks.controllers.predicates.MockAuthenticationPredicate
import play.api.i18n.MessagesApi
import utils.TestSupport
import assets.TestConstants.PaymentData._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class PaymentControllerSpec extends TestSupport with MockAuthenticationPredicate {



  object TestPaymentController extends PaymentController()(
    frontendAppConfig,
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate
  )

  "The PaymentController.paymentHandoff is action" when {

    s"Called with $testAmountInPence" should {

      lazy val result: Future[Result] = TestPaymentController.paymentHandoff(testAmountInPence)(fakeRequestWithActiveSession)

      "return status SEE_OTHER (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      s"check redirect location is'${frontendAppConfig.paymentsUrl}'" in {
        redirectLocation(result) shouldBe Some(frontendAppConfig.paymentsUrl)
      }

      s"check payment data is '$testPaymentDataJson'" in {
        await(result).session.get("payment-data") shouldBe Some(testPaymentDataJson.toString())
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestPaymentController.paymentHandoff(testAmountInPence)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }

    }
  }
}
