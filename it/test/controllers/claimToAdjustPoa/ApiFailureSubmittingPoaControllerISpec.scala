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
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import helpers.servicemocks.SessionDataStub.stubGetSessionDataResponseSuccess
import models.admin.AdjustPaymentsOnAccount
import org.scalatest.Assertion
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesResponse

class ApiFailureSubmittingPoaControllerISpec extends ComponentSpecBase {

  val isAgent = false

  def apiFailureSubmittingPoaUrl: String = controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent).url

  def amendablePoaUrl: String = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent).url

  def homeUrl: String = if (isAgent) {
    controllers.routes.HomeController.showAgent.url
  } else {
    controllers.routes.HomeController.show().url
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    stubGetSessionDataResponseSuccess()
    if (isAgent) {
      MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, false)
      //stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
    Given("Income Source Details with multiple business and property")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      OK, multipleBusinessesResponse
    )
  }

  def checkPageTitleOk(res: WSResponse): Assertion = {
    if (isAgent) {
      res should have(
        pageTitleAgent("claimToAdjustPoa.apiFailure.heading")
      )
    } else {
      res should have(
        pageTitleIndividual("claimToAdjustPoa.apiFailure.heading")
      )
    }
  }

  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(
      s"""${
        if (isAgent) {
          "/agents"
        } else ""
      }${url}""", additionalCookies = clientDetailsWithConfirmation)
  }

  "calling GET" should {
    s"return status $OK" when {
      s"user visits $apiFailureSubmittingPoaUrl with the AdjustPaymentsOnAccount FS enabled" in {
        enable(AdjustPaymentsOnAccount)

        When(s"I call GET")
        val res = get("/adjust-poa/error-poa-not-updated")

        res should have(
          httpStatus(OK)
        )
        checkPageTitleOk(res)
      }
    }
    s"return status $SEE_OTHER" when {
      s"user visits $apiFailureSubmittingPoaUrl with the AdjustPaymentsOnAccount FS disabled" in {
        disable(AdjustPaymentsOnAccount)

        When(s"I call GET")
        val res = get("/adjust-poa/error-poa-not-updated")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }
  }
}
