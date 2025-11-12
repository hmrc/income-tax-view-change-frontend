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
import models.UIJourneySessionData
import models.admin.{NavBarFs, ReportingFrequencyPage}
import models.incomeSourceDetails.{CeaseIncomeSourceData, IncomeSourceDetailsModel}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.IncomeSourceIntegrationTestConstants._
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

  def getIncomeSourceResponse(incomeSourceType: IncomeSourceType, allCeased: Boolean = false): IncomeSourceDetailsModel = incomeSourceType match {
    case SelfEmployment => if(allCeased) businessOnlyResponseAllCeased else businessOnlyResponseWithLatency
    case UkProperty => if(allCeased) ukPropertyOnlyResponseAllCeased else ukPropertyOnlyResponseWithLatency
    case ForeignProperty => if(allCeased) foreignPropertyOnlyResponseAllCeased else foreignPropertyOnlyResponseWithLatency
  }

  def getExpectedTitle(incomeSourceType: IncomeSourceType, allCeased: Boolean = false): String = {
    incomeSourceType match {
      case SelfEmployment => if(allCeased) messagesAPI(s"$prefix.title", messagesAPI(s"$prefix.sole-trader")) else messagesAPI(s"$prefix.title", b1TradingName)
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
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Business Ceased obligations page with remaining business content when only one business in latency exists and RF FS is turned on" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(ReportingFrequencyPage)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)
              setupTestMongoData(incomeSourceType)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getExpectedTitle(incomeSourceType)),
                elementTextByID("remaining-business")("Because your remaining business is new, it is set to be opted out of" +
                  " Making Tax Digital for Income Tax for up to 2 tax years." + " You can decide at any time to sign back up on your reporting obligations page."),
                elementTextByID("remaining-business-link")("your reporting obligations")
              )
            }
            "render the Business Ceased obligations page with remaining business content when only one business in latency exists and RF FS is turned OFF" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              disable(ReportingFrequencyPage)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)
              setupTestMongoData(incomeSourceType)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getExpectedTitle(incomeSourceType)),
                elementTextByID("remaining-business")("Because your remaining business is new, it is set to be opted out of" +
                  " Making Tax Digital for Income Tax for up to 2 tax years."),
                elementTextByID("remaining-business-link")("")
              )
            }
            "render the Business Ceased obligations page with remaining business content when  all business ceased and RF FS is turned on" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(ReportingFrequencyPage)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType, true))
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)
              setupTestMongoData(incomeSourceType)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getExpectedTitle(incomeSourceType, true)),
                elementTextByID("all-business-ceased")("In future, any new business you add will be opted out of Making Tax Digital for Income Tax." +
                  " Find out more about your reporting obligations."),
                elementTextByID("all-business-ceased-link")("your reporting obligations")
              )
            }
            "render the Business Ceased obligations page with remaining business content when  all business ceased and RF FS is turned OFF" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              disable(ReportingFrequencyPage)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType, true))
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)
              setupTestMongoData(incomeSourceType)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getExpectedTitle(incomeSourceType, true)),
                elementTextByID("all-business-ceased")("In future, any new business you add will be opted out of Making Tax Digital for Income Tax."),
                elementTextByID("all-business-ceased-link")("")
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
    }
  }
}
