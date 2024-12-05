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
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, NavBarFs}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class ManageIncomeSourceControllerISpec extends ControllerISpecHelper {

  val pageTitleMsgKey = "view-income-sources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val chooseMessage: String = messagesAPI("view-income-sources.choose")
  val startDateMessage: String = messagesAPI("view-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("view-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("view-income-sources.table-head.business-name")
  val ukPropertyStartDate: String = "1 January 2017"
  val foreignPropertyStartDate: String = "1 January 2017"
  val ceasedBusinessMessage: String = messagesAPI("view-income-sources.ceased-businesses-h2")
  val ceasedBusinessName: String = "ceasedBusiness"

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    pathStart + "/manage/view-and-manage-income-sources"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the View Income Source page" when {
            "the user has multiple businesses and property" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              val fallBackLink = if(mtdUserRole == MTDIndividual) {
                "/report-quarterly/income-and-expenses/view"
              } else {
                "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
              }
              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("table-head-business-name")(businessNameMessage),
                elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
                elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
                elementTextByID("view-link-business-1")(chooseMessage),
                elementTextByID("table-head-date-started-uk")(startDateMessage),
                elementTextByID("table-row-trading-start-date-uk")(ukPropertyStartDate),
                elementAttributeBySelector("#back-fallback", "href")(fallBackLink),
              )
            }

            "the user has foreign property and ceased business" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("ceased-businesses-heading")(ceasedBusinessMessage),
                elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
                elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
                elementTextByID("table-head-date-started-foreign")(startDateMessage),
                elementTextByID("table-row-trading-start-date-foreign")(foreignPropertyStartDate)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}