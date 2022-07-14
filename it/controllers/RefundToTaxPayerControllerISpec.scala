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

package controllers

import auth.MtdItUser
import config.featureswitch.PaymentHistoryRefunds
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.Nino
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, RepaymentItem, RepaymentSupplementItem}
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.paymentHistoryBusinessAndPropertyResponse

import java.time.LocalDate

class RefundToTaxPayerControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val testRepaymentHistoryModel: RepaymentHistoryModel = RepaymentHistoryModel(
    List(RepaymentHistory(
      Some(705.2),
      705.2,
      "BACS",
      12345,
      Vector(
        RepaymentItem(
          Vector(
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.78),
              Some(LocalDate.of(2021, 7, 31)),
              Some(LocalDate.of(2021, 9, 15)),
              Some(2.01)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(2.63),
              Some(LocalDate.of(2021, 9, 15)),
              Some(LocalDate.of(2021, 10, 24)),
              Some(1.76)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.26),
              Some(LocalDate.of(2021, 10, 24)),
              Some(LocalDate.of(2021, 11, 30)),
              Some(2.01))
          )
        )
      ), LocalDate.of(2021, 7, 23), LocalDate.of(2021, 7, 21), "000000003135")
    )
  )

  val repaymentRequestNumber: String = "023942042349"
  val testNino: String = "AB123456C"

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  s"GET ${controllers.routes.RefundToTaxPayerController.show(repaymentRequestNumber).url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        isAuthorisedUser(authorised = false)
        stubUserDetails()
        enable(PaymentHistoryRefunds)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
        IncomeTaxViewChangeStub.stubGetRepaymentHistoryByRepaymentId(Nino(testNino), repaymentRequestNumber)(OK, testRepaymentHistoryModel)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentHistory

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }

  s"return $OK with the refund to tax payer page" when {
    "the payment history refunds feature switch is enabled" in {
      isAuthorisedUser(authorised = true)
      stubUserDetails()
      enable(PaymentHistoryRefunds)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetRepaymentHistoryByRepaymentId(Nino(testNino), repaymentRequestNumber)(OK, testRepaymentHistoryModel)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getRefundToTaxPayer(repaymentRequestNumber)

      Then("The Refund to tax payer page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleIndividual("refund-to-taxpayer.heading")
      )
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getRefundToTaxPayer(repaymentRequestNumber))
    }
  }
}
