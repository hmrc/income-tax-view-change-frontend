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

package controllers.manageBusinesses.manage

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, completedUIJourneySessionData, emptyUIJourneySessionData}

class CannotGoBackErrorControllerISpec extends ControllerISpecHelper {
  val title: String = messagesAPI("cannotGoBack.heading")
  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Manage))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => "/manage-business-cannot-go-back"
      case UkProperty => "/manage-uk-property-cannot-go-back"
      case _ => "/manage-foreign-property-cannot-go-back"
    }
    pathStart + "/manage-your-businesses/manage" + pathEnd
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the cannot go back error page" when {
              "the journey is completed" in {
                enable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
                await(sessionService.setMongoData(completedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "cannotGoBack.heading")
                )
              }
            }

            "redirect to the home page" when {
              "the income sources feature switch is disabled" in {
                disable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

            "render the error page" when {
              "mongo is empty" in {
                enable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
                await(sessionService.setMongoData(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType))))
                val result = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
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

