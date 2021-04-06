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

package controllers.agent

import java.time.LocalDate

import assets.BaseIntegrationTestConstants._
import assets.PaymentHistoryTestConstraints.getCurrentTaxYearEnd
import config.featureswitch.{API5, AgentViewer, FeatureSwitching}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}


class PaymentHistoryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(API5)
    enable(AgentViewer)
  }

  val clientDetails: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val paymentFullJson: JsValue = Json.arr(Json.obj(
    "reference" -> "reference",
    "amount" -> 100.00,
    "method" -> "method",
    "lot" -> "lot",
    "lotItem" -> "lotItem",
    "date" -> "2018-04-25"
    )
  )

  val currentTaxYearEnd = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd = currentTaxYearEnd-1
  val twoPreviousTaxYearEnd = currentTaxYearEnd-2


  s"GET ${controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url}" should {
    s"SEE_OTHER to " when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = IncomeSourceDetailsModel(
            mtdbsa = testMtditid,
            yearOfMigration = None,
            businesses = List(BusinessDetailsModel(
              "testId",
              AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
              None, None, None, None, None, None, None, None,
              Some(getCurrentTaxYearEnd)
            )),
            property = None
          )
        )


        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentFullJson)
        IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentFullJson)


        val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetails)


        Then("The user is redirected to")
        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  s"return $NOT_FOUND" when {
    "the payment history feature switch is disabled" in {
      disable(AgentViewer)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = IncomeSourceDetailsModel(
          mtdbsa = testMtditid,
          yearOfMigration = None,
          businesses = List(BusinessDetailsModel(
            "testId",
            AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
            None, None, None, None, None, None, None, None,
            Some(getCurrentTaxYearEnd)
          )),
          property = None
        )
      )

      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentFullJson)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentFullJson)

      val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetails)

      Then(s"A not found page is returned to the user")
      result should have(
        httpStatus(NOT_FOUND)
      )
    }
  }

  s"return $OK with the enter client utr page" when {
    "the payment history feature switch is enabled" in {
      enable(AgentViewer)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = IncomeSourceDetailsModel(
          mtdbsa = testMtditid,
          yearOfMigration = None,
          businesses = List(BusinessDetailsModel(
            "testId",
            AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
            None, None, None, None, None, None, None, None,
            Some(getCurrentTaxYearEnd)
          )),
          property = None
        )
      )

      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, paymentFullJson)
      IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, paymentFullJson)

      val result = IncomeTaxViewChangeFrontend.getPaymentHistory(clientDetails)

      Then("The Payment History page is returned to the user")
      result should have(
        httpStatus(OK)
      )
    }
  }
}
