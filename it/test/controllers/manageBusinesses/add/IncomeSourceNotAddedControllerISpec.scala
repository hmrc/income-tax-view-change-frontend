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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.NavBarFs
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class IncomeSourceNotAddedControllerISpec extends ControllerISpecHelper {

  val continueButtonText: String = messagesAPI("")
  val pageTitle: String = messagesAPI("standardError.heading")

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/error-business-not-added"
      case UkProperty => s"$pathStart/add-uk-property/error-uk-property-not-added"
      case ForeignProperty => s"$pathStart/add-foreign-property/error-foreign-property-not-added"
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => noPropertyOrBusinessResponse
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the self employment business not added error page" when {
              "using the manage businesses journey" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole,pageTitle)
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