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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import java.time.LocalDate

class IncomeSourceRFCheckDetailsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/reporting-frequency-check-details"
      case UkProperty => s"$pathStart/add-uk-property/reporting-frequency-check-details"
      case ForeignProperty => s"$pathStart/add-foreign-property/reporting-frequency-check-details"
    }
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdUserRole =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)

      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"render the ${incomeSourceType.journeyType} Reporting Frequency Check Details Page" when {
              "income source new journey is enabled" in {
                enable(IncomeSourcesNewJourney)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
                await(sessionService.createSession(IncomeSourceJourneyType(Add, incomeSourceType)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "income-sources.check-details-reporting-frequency.title"),
                  elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("No"),
                  elementTextByID("confirm-button")("Confirm and continue")
                )
              }
            }
          }

          "income source new journey is enabled" in {
            disable(IncomeSourcesNewJourney)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
            await(sessionService.createSession(IncomeSourceJourneyType(Add, incomeSourceType)))

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              if (mtdUserRole == MTDIndividual) redirectURI("/report-quarterly/income-and-expenses/view") else redirectURI("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
            )
          }
        }
      }
      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"submit the data after the user has checked their details" in {
              val isAgent = !(mtdUserRole == MTDIndividual)

              enable(IncomeSourcesNewJourney)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
              await(sessionService.createSession(IncomeSourceJourneyType(Add, incomeSourceType)))

              val journeyType = incomeSourceType match {
                case SelfEmployment => "ADD-SE"
                case UkProperty => "ADD-UK"
                case ForeignProperty => "ADD-FP"
              }

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, journeyType,
                addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("incomeSourceId"), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

              val result = buildPOSTMTDPostClient(path, additionalCookies, Map("current-year-checkbox" -> Seq("true"), "next-year-checkbox" -> Seq("true"))).futureValue

              val redirectUri = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedController.showAgent(incomeSourceType).url else controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(incomeSourceType).url

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(redirectUri)
              )
            }
          }
        }
      }
    }
  }
}
