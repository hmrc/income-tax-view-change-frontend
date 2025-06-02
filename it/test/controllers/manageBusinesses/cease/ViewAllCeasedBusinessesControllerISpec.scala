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
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{DisplayBusinessStartDate, IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesWithBothPropertiesAndCeasedBusiness}

class ViewAllCeasedBusinessesControllerISpec extends ControllerISpecHelper {

  val pageTitleMsgKey = "Businesses that have ceased"
  val soleTraderBusinessName1: String = "thirdBusiness"
  val ceaseMessage: String = "Cease"
  val startDateMessage: String = "Date started"
  val ceasedDateMessage: String = "Date ended"
  val businessNameMessage: String = "Business name"
  val propertyEndDate: String = "1 January 2020"
  val propertyStartDate: String = "1 January 2018"
  val propertyStartDate1: String = "31 December 2019"
  val ceasedBusinessName: String = "ceasedBusiness"

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses/ceased-businesses"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Cease Income Source page" when {
            "the user has multipleBusinessesWithBothPropertiesAndCeasedBusiness" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("ceased-businesses-table-head-name")(businessNameMessage),
                elementTextByID("ceased-business-table-row-trading-name-0")(soleTraderBusinessName1),
                elementTextByID("ceased-business-table-row-date-ended-0")(propertyEndDate),
                elementTextByID("ceased-businesses-table-head-date-started")(startDateMessage),
                elementTextByID("ceased-business-table-row-date-started-0")(propertyStartDate),
              )
            }

            "DisplayBusinessStartDate FS is disabled" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesNewJourney)
              disable(DisplayBusinessStartDate)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("ceased-businesses-table-head-name")(businessNameMessage),
                elementTextByID("ceased-business-table-row-trading-name-0")(soleTraderBusinessName1),
                elementTextByID("ceased-business-table-row-date-ended-0")(propertyEndDate),
                elementTextByID("ceased-businesses-table-head-date-started")(""),
                elementTextByID("ceased-business-table-row-date-started-0")(""),
              )
            }

            "the user has foreignPropertyAndCeasedBusiness " in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("ceased-businesses-table-head-name")(businessNameMessage),
                elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
                elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
                elementTextByID("ceased-businesses-table-head-date-started")(startDateMessage),
                elementTextByID("ceased-business-table-row-date-ended-0")(propertyStartDate1),
                elementTextByID("ceased-business-table-row-date-started-0")(propertyStartDate)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}