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

package controllers.incomeSources.add

import controllers.ControllerISpecHelper
import helpers.servicemocks.{AddressLookupStub, IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ControllerISpecHelper {

  val path = "/income-sources/add/business-address"
  val changePath = "/income-sources/add/change-business-address-lookup"

  s"GET $path" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "redirect to address lookup" in {
        enable(IncomeSourcesFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        AddressLookupStub.stubPostInitialiseAddressLookup()

        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("TestRedirect")
        )
      }
    }
      testAuthFailuresForMTDIndividual(path)
  }

  s"GET $changePath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "redirect to address lookup" in {
        enable(IncomeSourcesFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        AddressLookupStub.stubPostInitialiseAddressLookup()

        val result = buildGETMTDClient(changePath).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("TestRedirect")
        )
      }
    }
    testAuthFailuresForMTDIndividual(changePath)
  }
}
