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

package controllers.reportingObligations.optOut

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{OptOutSessionRepositoryHelper, WiremockHelper}
import models.admin.{NavBarFs, OptOutFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSessionId}
import testConstants.ITSAStatusTestConstants.{successITSAStatusResponseJson2021, successITSAStatusResponseJson2022, successITSAStatusResponseJson2023}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

import java.time.LocalDate

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

  mtdAllRoles.foreach {
    mtdUserRole =>
      val path = getPath(mtdUserRole)
      val additionalCookies = getAdditionalCookies(mtdUserRole)

      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"render confirm single year opt out page" in {

              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetAllObligations(
                nino = testNino,
                fromDate = LocalDate.of(2021, 1, 1),
                toDate = LocalDate.of(2022, 1, 1),
                deadlines = ObligationsModel(Seq(GroupedObligationsModel(
                  identification = "fakeId",
                  obligations = List(
                    SingleObligationModel(
                      start = LocalDate.of(2021, 1, 1),
                      end = LocalDate.of(2022, 1, 1),
                      due = LocalDate.of(2023, 1, 1),
                      obligationType = "Quarterly",
                      dateReceived = Some(LocalDate.of(2021, 1, 1)),
                      periodKey = "#004",
                      status = StatusFulfilled,
                    )
                  )
                )))
              )

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              val calcResponseBody =
                """
                  |{
                  |  "calculationId": "TEST_ID",
                  |  "calculationTimestamp": "TEST_STAMP",
                  |  "calculationType": "TEST_TYPE",
                  |  "crystallised": false
                  |}
                  |""".stripMargin

              WiremockHelper.stubGet("/income-tax-view-change/list-of-calculation-results/AA123456A/2022", 200, calcResponseBody)

              val responseBody = Json.arr(successITSAStatusResponseJson2021, successITSAStatusResponseJson2022, successITSAStatusResponseJson2023)

              val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

              WiremockHelper.stubGet(url, OK, responseBody.toString())

              helper.stubOptOutInitialState(
                currentTaxYear = currentTaxYear,
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = Mandated,
                nextYearStatus = Mandated,
                selectedOptOutYear = Some("2022-2023")
              )

              val result: WSResponse = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optout.confirmedOptOut.heading")
              )
            }

            s"redirect to cannot-go-back page when session data are invalid" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

              helper.stubOptOutInitialState(
                currentTaxYear = currentTaxYear,
                previousYearCrystallised = false,
                previousYearStatus = Annual,
                currentYearStatus = Annual,
                nextYearStatus = Annual,
                selectedOptOutYear = Some("2021-2022")
              )

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              val responseBody = Json.arr(successITSAStatusResponseJson2021, successITSAStatusResponseJson2022, successITSAStatusResponseJson2023)

              val url = s"/income-tax-view-change/itsa-status/status/AA123456A/21-22?futureYears=true&history=false"

              WiremockHelper.stubGet(url, OK, responseBody.toString())

              val result: WSResponse = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              val isAgent = mtdUserRole != MTDIndividual
              val expectedRedirectPath = controllers.reportingObligations.routes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(false)).url

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(expectedRedirectPath)
              )

            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
  }
}
