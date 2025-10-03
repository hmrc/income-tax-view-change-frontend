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

package controllers

import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks._
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful

class DeductionsSummaryControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/$testYear/allowances-and-deductions"
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
            "render the deduction summary page" in {
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
              IncomeTaxCalculationStub.stubGetCalculationResponseWithFlagResponse(testNino, testYear, "LATEST")(
                status = OK,
                body = liabilityCalculationModelSuccessful
              )

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationWithFlagResponse(testNino, testYear, "LATEST")

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "deduction_breakdown.heading"),
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
