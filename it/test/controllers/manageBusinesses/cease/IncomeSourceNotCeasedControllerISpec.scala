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
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

class IncomeSourceNotCeasedControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    incomeSourceType match {
      case SelfEmployment => pathStart + "/manage-your-businesses/cease/error-business-not-ceased"
      case UkProperty => pathStart + "/manage-your-businesses/cease/error-uk-property-not-ceased"
      case _ => pathStart + "/manage-your-businesses/cease/error-foreign-property-not-ceased"
    }
  }

  mtdAllRoles.foreach { mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the not ceased page" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesNewJourney)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "standardError.heading", isErrorPage = true)
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
    }
  }
}
