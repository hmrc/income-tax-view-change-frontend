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
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.incomeSourceDetails.ManageIncomeSourceData
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class ReportingMethodErrorControllerISpec extends ControllerISpecHelper {

  val pageTitle: String = messagesAPI("standardError.heading")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => foreignPropertyOnlyResponse
    }
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => "/error-change-reporting-method-not-saved"
      case UkProperty => "/error-change-reporting-method-not-saved-uk-property"
      case _ => "/error-change-reporting-method-not-saved-foreign-property"
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
            "render the reporting method error page" when {
              "using the manage businesses journey" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))
                val result = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, pageTitle)
                )
              }
            }

            "render the error page" when {
              if (incomeSourceType == SelfEmployment) {
                "the Income Source Id does not exist" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                    manageIncomeSourceData = Some(ManageIncomeSourceData(None)))))
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  result should have(
                    httpStatus(INTERNAL_SERVER_ERROR)
                  )
                }
              } else {
                "the user does not have a property Income Source" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  result should have(
                    httpStatus(INTERNAL_SERVER_ERROR)
                  )
                }
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
    }
  }
}
