/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.claimToAdjustPOA

import config.featureswitch.AdjustPaymentsOnAccount
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class AmendablePOAControllerISpec extends ComponentSpecBase {

  private val amendPoaUrl = controllers.claimToAdjustPOA.routes.AmendablePOAController.show(isAgent = false).url
  private val testTaxYear = 2024

  s"calling GET $amendPoaUrl" should {
    s"return status $OK and render the Adjusting your payments on account page" when {
      "User is authorised" in {
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

        When(s"I call GET $amendPoaUrl")
        val res = IncomeTaxViewChangeFrontend.getAdjustPaymentsOnAccount

        res should have(
          httpStatus(OK)
        )
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

        When(s"I call GET $amendPoaUrl")
        val res = IncomeTaxViewChangeFrontend.getAdjustPaymentsOnAccount

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

      When(s"I call GET $amendPoaUrl")
      val res = IncomeTaxViewChangeFrontend.getAdjustPaymentsOnAccount

      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }
  }
}
