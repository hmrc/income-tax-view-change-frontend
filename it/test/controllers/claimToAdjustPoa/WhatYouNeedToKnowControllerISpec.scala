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

import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class WhatYouNeedToKnowControllerISpec extends ComponentSpecBase {

  val isAgent = false

  val whatYouNeedToKnowUrl: String = controllers.claimToAdjustPoa.routes.WhatYouNeedToKnowController.show(isAgent).url
  val testTaxYear = 2024

  val enterPOAAmountUrl = controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent, NormalMode).url
  val selectReasonUrl = controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode).url


  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    sessionService.setMongoData(None)
    if(isAgent) {
      stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
  }

  s"calling GET $whatYouNeedToKnowUrl" should {
    s"return status $OK and render the What You Need To Know page (with correct link)" when {
      "User is authorised and has originalAmount >= relevantAmount" in {
        enable(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )

        When(s"I call GET $whatYouNeedToKnowUrl")
        val res = IncomeTaxViewChangeFrontend.getPOAWhatYouNeedToKnow

        lazy val document: Document = Jsoup.parse(res.body)
        val continueButton = document.getElementById("continue")

        res should have(
          httpStatus(OK)
        )
        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData()))
        continueButton.attr("href") shouldBe selectReasonUrl

        val content = Option(document.getElementById("p6"))
        content.isDefined shouldBe false

      }
      "User is authorised and has originalAmount < relevantAmount" in {
        enable(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )

        When(s"I call GET $whatYouNeedToKnowUrl")
        val res = IncomeTaxViewChangeFrontend.getPOAWhatYouNeedToKnow

        lazy val document: Document = Jsoup.parse(res.body)
        val continueButton = document.getElementById("continue")

        res should have(
          httpStatus(OK)
        )
        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData()))
        continueButton.attr("href") shouldBe enterPOAAmountUrl

        val content = Option(document.getElementById("p6"))
        content.isDefined shouldBe false
      }

      "User is authorised and has partially paid, and has originalAmount < poaRelevantAmount" in {
        enable(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(
            originalAmount = 2000,
            outstandingAmount = 1000,
            taxYear = (testTaxYear - 1).toString,
            dueDate = testDate.toString,
            poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(
            originalAmount = 2000,
            outstandingAmount = 1000,
            taxYear = (testTaxYear - 1).toString,
            dueDate = testDate.toString,
            poaRelevantAmount = Some(3000))
        )

        When(s"I call GET $whatYouNeedToKnowUrl")
        val res = IncomeTaxViewChangeFrontend.getPOAWhatYouNeedToKnow

        lazy val document: Document = Jsoup.parse(res.body)
        val continueButton = document.getElementById("continue")

        res should have(
          httpStatus(OK)
        )
        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData()))
        continueButton.attr("href") shouldBe enterPOAAmountUrl

        val content = Option(document.getElementById("p6"))
        content.isDefined shouldBe true
      }
    }
    s"return status $SEE_OTHER and redirect to the home page" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )

        When(s"I call GET $whatYouNeedToKnowUrl")
        val res = IncomeTaxViewChangeFrontend.getPOAWhatYouNeedToKnow

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
        )
      }
    }
  }
  s"return $INTERNAL_SERVER_ERROR" when {
    "no non-crystallised financial details are found" in {
      enable(AdjustPaymentsOnAccount)

      And("I wiremock stub empty financial details response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
        OK, testEmptyFinancialDetailsModelJson
      )
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
        OK, testEmptyFinancialDetailsModelJson
      )

      When(s"I call GET $whatYouNeedToKnowUrl")
      val res = IncomeTaxViewChangeFrontend.getPOAWhatYouNeedToKnow

      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }
  }

}
