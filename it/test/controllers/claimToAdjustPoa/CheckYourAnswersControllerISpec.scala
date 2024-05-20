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

package controllers.claimToAdjustPoa

import config.featureswitch.AdjustPaymentsOnAccount
import forms.adjustPoa.SelectYourReasonFormProvider
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.claimToAdjustPoa.{PoAAmendmentData, SelectYourReason}
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelJson}

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  val isAgent = false
  def homeUrl: String =
    if (isAgent) controllers.routes.HomeController.showAgent.url
    else         controllers.routes.HomeController.show().url
  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    sessionService.setMongoData(None)
    if(isAgent) {
      stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
  }
  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(s"""${if (isAgent) {"/agents" } else ""}$url""", additionalCookies = clientDetailsWithConfirmation)
  }

  def postSelectYourReason(isAgent: Boolean, answer: Option[SelectYourReason])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    val formProvider = app.injector.instanceOf[SelectYourReasonFormProvider]
    IncomeTaxViewChangeFrontend.post(
      uri = s"""${if(isAgent) {"/agents"} else {""}}/adjust-poa/check-your-answers""",
      additionalCookies = additionalCookies
    )(
      answer.fold(Map.empty[String, Seq[String]])(
        selection =>
          formProvider()
            .fill(selection)
            .data
            .map { case (k, v) => (k, Seq(v)) }
      )
    )
  }

  s"calling GET" should {

    s"return status $OK" when {

      "user has successfully entered a new POA amount" in {

        enable(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )

        And("A session has been created")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call GET")
        val res = get("/adjust-poa/select-your-reason")

        res should have(
          httpStatus(OK)
        )
      }
    }
  }
}
