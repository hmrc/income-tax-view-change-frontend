/*
 * Copyright 2025 HM Revenue & Customs
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
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.{IncomeSourceReportingFrequencySourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class AddSoleTraderChooseTaxYearISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/add/sole-trader/choose-taxyear"
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.set(UIJourneySessionData(
      testSessionId,
      "IncomeSourceReportingFrequencyJourney",
      incomeSourceReportingFrequencyData = Some(IncomeSourceReportingFrequencySourceData(false, false)))
    )
  }

  override def afterEach(): Unit = {
    super.afterEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  mtdAllRoles.foreach { case mtdUserRole =>
      val path = getPath(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated" when {
            "income sources new journey is enabled" in {
              enable(IncomeSourcesNewJourney)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path).futureValue

              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                elementTextByID("choose-tax-year-heading")("Sole Trader Which tax year do you want to report quarterly for?"),
                elementTextByID("choose-tax-year-subheading")("Sole Trader"),
                elementTextBySelector("[for='current-year-checkbox']")("2024 to 2025"),
                elementTextBySelector("[for='next-year-checkbox']")("2025 to 2026"),
                elementTextByID("continue-button")("Continue"),
              )
            }

            "income sources new journey is disabled" in {
              disable(IncomeSourcesNewJourney)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path).futureValue

              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                if(mtdUserRole == MTDIndividual) redirectURI("/report-quarterly/income-and-expenses/view") else redirectURI("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole, Some(Map(
          "current-year-checkbox" -> Seq("Test")
        )))
      }
    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated" should {
          "submit the reporting frequency for the income source" in {
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

            val result = buildPOSTMTDPostClient(path, body = Map("current-year-checkbox" -> Seq("true"), "next-year-checkbox" -> Seq("true"))).futureValue

            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK)
            )
          }

          "return an error if the form is invalid" in {
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

            val result = buildPOSTMTDPostClient(path, body = Map("Invalid" -> Seq("Invalid"))).futureValue

            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(BAD_REQUEST),
              elementTextByID("choose-tax-year-heading")("Sole Trader Which tax year do you want to report quarterly for?"),
              elementTextByID("choose-tax-year-subheading")("Sole Trader"),
              elementTextBySelector("[for='current-year-checkbox']")("2024 to 2025"),
              elementTextBySelector("[for='next-year-checkbox']")("2025 to 2026"),
              elementTextByID("continue-button")("Continue"),
              elementTextByID("error-summary-title")("There is a problem"),
              elementTextByID("error-summary-link")("Select the tax years you want to report quarterly")
            )
          }
        }
      }
      testAuthFailures(path, mtdUserRole, Some(Map(
        "current-year-checkbox" -> Seq("Test")
      )))
    }
  }
}
