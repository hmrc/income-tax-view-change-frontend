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

package controllers.optOut.newJourney

import controllers.ControllerISpecHelper
import controllers.constants.ConfirmOptOutUpdateControllerConstants._
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ITSAStatusUpdateConnectorStub, OptOutSessionRepositoryHelper}
import models.admin.{NavBarFs, OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage}
import models.itsaStatus.ITSAStatus._
import play.api.http.Status
import play.api.http.Status.OK
import play.mvc.Http.Status.SEE_OTHER
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmOptOutUpdateControllerISpec extends ControllerISpecHelper {

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole, taxYear: String = currentTaxYear(dateService).startYear.toString): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/optout/check-your-answers/$taxYear"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"render check opt-out update answers page" in {
            enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear(dateService),
              previousYearCrystallised = false,
              previousYearStatus = Annual,
              currentYearStatus = Voluntary,
              nextYearStatus = Annual,
              Some(currentTaxYear(dateService).toString))

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK)
            )
          }
        }
        testAuthFailures(path, mtdUserRole)

        "has already completed the journey (according to session data)" should {
          s"redirect to the cannot go back page" in {
            enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear(dateService),
              previousYearCrystallised = false,
              previousYearStatus = Annual,
              currentYearStatus = Voluntary,
              nextYearStatus = Annual,
              Some(currentTaxYear(dateService).toString),
              journeyIsComplete = true
            )

            val redirectUrl: String = controllers.routes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(false)).url
            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(redirectUrl)
            )
          }
        }
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to the completion page" when {
            "user confirms opt-out for one-year scenario" in {
              enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              helper.stubOptOutInitialState(
                currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = NoStatus,
                nextYearStatus = NoStatus
              )

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(
                taxableEntityId = propertyOnlyResponse.nino,
                status = Status.NO_CONTENT,
                responseBody = ""
              )

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.optOut.routes.ConfirmedOptOutController.show(isAgent).url)
              )

            }
          }

        }
        testAuthFailures(path, mtdUserRole, Some(Map.empty))
      }
    }
  }
}
