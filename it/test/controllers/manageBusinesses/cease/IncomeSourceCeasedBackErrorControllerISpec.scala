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

package controllers.manageBusinesses.cease

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, completedUIJourneySessionData}

class IncomeSourceCeasedBackErrorControllerISpec extends ControllerISpecHelper {

  val title = messagesAPI("cannotGoBack.heading")
  val headingSE = messagesAPI("cannotGoBack.sole-trader-ceased")
  val headingUk = messagesAPI("cannotGoBack.uk-property-ceased")
  val headingFP = messagesAPI("cannotGoBack.foreign-property-ceased")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def specificHeading(incomeSourceType: IncomeSourceType) = incomeSourceType match {
    case SelfEmployment => headingSE
    case UkProperty => headingUk
    case ForeignProperty => headingFP
  }

  def expectedTitle(incomeSourceType: IncomeSourceType): String = {
    s"$title - ${specificHeading(incomeSourceType)}"
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    val endPath = incomeSourceType match {
      case SelfEmployment => s"/manage-your-businesses/cease-sole-trader/cease-business-cannot-go-back"
      case UkProperty =>  "/manage-your-businesses/cease-uk-property/cease-uk-property-cannot-go-back"
      case _ => "/manage-your-businesses/cease-foreign-property/cease-foreign-property-cannot-go-back"
    }
    pathStart + endPath
  }

  mtdAllRoles.foreach { mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the back error page" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesNewJourney)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(completedUIJourneySessionData(IncomeSourceJourneyType(Cease, incomeSourceType))))
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, expectedTitle(incomeSourceType))
              )
            }

            "redirect to home page" when {
              "FS disabled" in {
                stubAuthorised(mtdUserRole)
                disable(NavBarFs)
                disable(IncomeSourcesNewJourney)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole)

        }
      }
    }
  }
}