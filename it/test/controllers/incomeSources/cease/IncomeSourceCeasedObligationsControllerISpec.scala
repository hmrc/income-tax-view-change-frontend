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

package controllers.incomeSources.cease

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSources, NavBarFs}
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testEndDate2022, testMtditid, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel

import java.time.LocalDate

class IncomeSourceCeasedObligationsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val prefix: String = "business-ceased.obligation"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    incomeSourceType match {
      case SelfEmployment => pathStart + "/income-sources/cease/cease-business-success"
      case UkProperty => pathStart + "/income-sources/cease/cease-uk-property-success"
      case _ => pathStart + "/income-sources/cease/cease-foreign-property-success"
    }
  }

  def getIncomeSourceResponse(incomeSourceType: IncomeSourceType) = incomeSourceType match {
    case SelfEmployment => businessOnlyResponse
    case UkProperty => ukPropertyOnlyResponse
    case ForeignProperty => foreignPropertyOnlyResponse
  }

  def getExpectedTitle(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => b1TradingName + " " + messagesAPI(s"$prefix.heading1.base")
      case UkProperty => messagesAPI("business-ceased.obligation.heading1.uk-property.part2") + " " + messagesAPI("business-ceased.obligation.heading1.base")
      case ForeignProperty => messagesAPI("business-ceased.obligation.heading1.foreign-property.part2") + " " + messagesAPI("business-ceased.obligation.heading1.base")
    }
  }

  mtdAllRoles.foreach { mtdUserRole =>
    List(UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Business Ceased obligations page" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSources)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))
              IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)
              if (incomeSourceType == SelfEmployment) {
                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"CEASE-${incomeSourceType.key}", ceaseIncomeSourceData =
                  Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(true))))))
              }

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getExpectedTitle(incomeSourceType)),
                elementTextByID("continue-button")(continueButtonText)
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
    }
  }
}
