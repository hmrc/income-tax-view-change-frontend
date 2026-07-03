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

package returns.controllers

import common.controllers.ControllerISpecHelper
import common.enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import common.helpers.servicemocks.IncomeTaxBusinessDetailsStub
import common.models.admin.PostFinalisationAmendmentsR18
import play.api.http.Status.*
import common.testConstants.BaseIntegrationTestConstants.*
import common.testConstants.IncomeSourceIntegrationTestConstants.*

class TaxYearsControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/tax-years"
  }
  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "render the forecast income summary page" when {
              "the user has firstAccountingPeriodEndDate and hence valid tax years" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

                val res = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxBusinessDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                Then("The view should have the correct headings and all tax years display")
                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "taxYears.heading"),
                  nElementsWithClass("govuk-summary-list__row")(6),
                  elementTextBySelectorList("dl", "div:nth-child(1)", "dt")(
                    expectedValue = s"${getCurrentTaxYearEnd.getYear - 1} to ${getCurrentTaxYearEnd.getYear} current tax year"
                  )
                )
              }

              "render the amendment guidance text when PostFinalisationAmendmentsR18 FS is enabled" in {
                stubAuthorised(mtdUserRole, List(PostFinalisationAmendmentsR18))
                IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  multipleBusinessesAndPropertyResponseWoMigration
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                res should have(
                  httpStatus(OK),
                  elementTextByID("pfa-amendment-text")
                  ("You can view the tax year summary pages to also see your options for amending the filed return for that year.")
                )
              }
            }

            "return 500 Internal Server " when {
              "no firstAccountingPeriodEndDate exists for both business and property" in {
                stubAuthorised(mtdUserRole)

                IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val res = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxBusinessDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                res should have(
                  httpStatus(BAD_REQUEST)
                )
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
