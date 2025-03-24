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
import helpers.servicemocks.{AddressLookupStub, IncomeTaxViewChangeStub}
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole, isChange: Boolean): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = if(isChange) "/change-business-address-lookup" else "/business-address"
    pathStart + "/manage-your-businesses/add-sole-trader" + pathEnd
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole, isChange = false)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to address lookup" in {
            enable(IncomeSourcesNewJourney)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            AddressLookupStub.stubPostInitialiseAddressLookup()

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI("TestRedirect")
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }

    val changePath = getPath(mtdUserRole, isChange = true)

    s"GET $changePath" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to address lookup" in {
            enable(IncomeSourcesNewJourney)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            AddressLookupStub.stubPostInitialiseAddressLookup()

            val result = buildGETMTDClient(changePath, additionalCookies).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI("TestRedirect")
            )
          }
        }
        testAuthFailures(changePath, mtdUserRole)
      }
    }
  }
}
