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
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class CeaseIncomeSourcesControllerISpec extends ControllerISpecHelper {

  val pageTitleMsgKey = "cease-income-sources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val ceaseMessage: String = messagesAPI("cease-income-sources.cease")
  val startDateMessage: String = messagesAPI("cease-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("cease-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("cease-income-sources.table-head.business-name")
  val startDate = "1 January 2017"
  val ceasedBusinessMessage: String = messagesAPI("cease-income-sources.ceased-businesses.h1")
  val ceasedBusinessName: String = "ceasedBusiness"

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses/cease/cease-an-income-source"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Cease Income Source page" when {
            "Income source details are enabled for UK property" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesNewJourney)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("table-head-business-name")(businessNameMessage),
                elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
                elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
                elementTextByID("cease-link-business-0")(ceaseMessage),
                elementTextByID("cease-link-business-1")(ceaseMessage),
                elementTextByID("table-head-date-started-uk")(startDateMessage),
                elementTextByID("table-row-trading-start-date-uk")(startDate)
              )
            }
            "Income source details are enabled for for foreign property" in {
              stubAuthorised(mtdUserRole)
              enable(IncomeSourcesNewJourney)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("ceased-businesses-heading")(ceasedBusinessMessage),
                elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
                elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
                elementTextByID("cease-link-foreign")(ceaseMessage),
                elementTextByID("table-head-date-started-foreign")(startDateMessage),
                elementTextByID("table-row-trading-start-date-foreign")(startDate)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}