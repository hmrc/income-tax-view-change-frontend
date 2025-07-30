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

package controllers.optOut

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{OptOutSessionRepositoryHelper, WiremockHelper}
import models.admin.{NavBarFs, OptOutFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import play.api.http.Status.OK
import play.api.libs.json.Json
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.ITSAStatusTestConstants.successITSAStatusResponseJson
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmedOptOutControllerISpec extends ControllerISpecHelper {

  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/optout/confirmed"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"render confirm single year opt out page" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            val responseBody = Json.arr(successITSAStatusResponseJson)
            val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

            WiremockHelper.stubGet(url, OK, responseBody.toString())

            helper.stubOptOutInitialState(currentTaxYear,
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = Mandated,
              nextYearStatus = Mandated)

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              pageTitle(mtdUserRole, "optout.confirmedOptOut.heading")
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
