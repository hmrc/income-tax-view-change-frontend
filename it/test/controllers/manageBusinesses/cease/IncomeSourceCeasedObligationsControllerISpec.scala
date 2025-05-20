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

package controllers.manageBusinesses.cease

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel

import java.time.LocalDate

class IncomeSourceCeasedObligationsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val businessCeasedObligationsShowUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url
  val foreignPropertyCeasedObligationsShowUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url
  val ukPropertyCeasedObligationsShowUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url
  val testDate: String = "2020-11-10"
  val prefix: String = "business-ceased.obligation"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Self Assessment - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    incomeSourceType match {
      case SelfEmployment => pathStart + "/manage-your-businesses/cease-sole-trader/cease-success"
      case UkProperty => pathStart + "/manage-your-businesses/cease-uk-property/cease-success"
      case _ => pathStart + "/manage-your-businesses/cease-foreign-property/cease-success"
    }
  }

  def getIncomeSourceResponse(incomeSourceType: IncomeSourceType) = incomeSourceType match {
    case SelfEmployment => businessOnlyResponse
    case UkProperty => ukPropertyOnlyResponse
    case ForeignProperty => foreignPropertyOnlyResponse
  }

  def getExpectedTitle(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messagesAPI(s"$prefix.title", b1TradingName)
      case UkProperty => messagesAPI(s"$prefix.title", messagesAPI(s"$prefix.uk-property"))
      case ForeignProperty => messagesAPI(s"$prefix.title", messagesAPI(s"$prefix.foreign-property"))
    }
  }

  def setupTestMongoData(incomeSourceType: IncomeSourceType) = {
    val incomeSourceId = incomeSourceType match {
      case SelfEmployment => testSelfEmploymentId
      case _ => testPropertyIncomeId
    }
    await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"CEASE-${incomeSourceType.key}", ceaseIncomeSourceData =
      Some(CeaseIncomeSourceData(incomeSourceId = Some(incomeSourceId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(true))))))
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
              enable(IncomeSourcesNewJourney)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)
              setupTestMongoData(incomeSourceType)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getExpectedTitle(incomeSourceType)),
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
    }
  }
}
