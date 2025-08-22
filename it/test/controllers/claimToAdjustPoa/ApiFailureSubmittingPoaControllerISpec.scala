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

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesResponse

class ApiFailureSubmittingPoaControllerISpec extends ControllerISpecHelper {

  def getPath(mtdUserRole: MTDUserRole) = {
    val pathStart = if(mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/error-poa-not-updated"
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
            s"render the Adjusting your payments on account page" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK, multipleBusinessesResponse
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "claimToAdjustPoa.apiFailure.heading")
                )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
