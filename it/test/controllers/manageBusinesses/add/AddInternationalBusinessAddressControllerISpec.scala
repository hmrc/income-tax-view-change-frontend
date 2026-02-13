/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.WiremockHelper
import helpers.servicemocks.{AddressLookupStub, IncomeTaxViewChangeStub}
import models.admin.{NavBarFs, OverseasBusinessAddress}
import models.core.CorrelationId
import models.core.CorrelationId.correlationId
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.AddressLookupTestConstants
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddInternationalBusinessAddressControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses/add-sole-trader/international-business-address"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to address lookup" in {
            disable(NavBarFs)
            enable(OverseasBusinessAddress)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            AddressLookupStub.stubPostInitialiseAddressLookup()

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI("TestRedirect")
            )
            val requestBody = if(mtdUserRole == MTDIndividual) AddressLookupTestConstants.internationalRequestBodyInvididual else AddressLookupTestConstants.internationalRequestBodyAgent

            WiremockHelper.verifyPost("/api/v2/init",
              Some(Json.stringify(requestBody)))
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
