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

package controllers.incomeSources.add

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, NavBarFs}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesResponse

class AddIncomeSourcesControllerISpec extends ControllerISpecHelper {

  val pageTitleMsgKey = "incomeSources.add.addIncomeSources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val addBusinessLink: String = messagesAPI("incomeSources.add.addIncomeSources.selfEmployment.link")
  val businessNameMessage: String = messagesAPI("incomeSources.add.addIncomeSources.selfEmployment.heading")
  val ukPropertyHeading: String = messagesAPI("incomeSources.add.addIncomeSources.ukProperty.heading")
  val addUKPropertyLink: String = messagesAPI("incomeSources.add.addIncomeSources.ukProperty.link")
  val foreignPropertyHeading: String = messagesAPI("incomeSources.add.addIncomeSources.foreignProperty.heading")
  val foreignPropertyLink: String = messagesAPI("incomeSources.add.addIncomeSources.foreignProperty.link")

  def getPath(mtdRole: MTDUserRole, isChange: Boolean): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/income-sources/add/new-income-sources"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole, isChange = false)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Add Income Source page" in {
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
            val res = buildGETMTDClient(path, additionalCookies).futureValue
            verifyIncomeSourceDetailsCall(testMtditid)

            res should have(
              httpStatus(OK),
              pageTitle(mtdUserRole, pageTitleMsgKey),
              elementTextByID("self-employment-h2")(businessNameMessage),
              elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
              elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
              elementTextByID("self-employment-link")(addBusinessLink),
              elementTextByID("uk-property-h2")(ukPropertyHeading),
              elementTextByID("uk-property-link")(addUKPropertyLink),
              elementTextByID("foreign-property-h2")(foreignPropertyHeading),
              elementTextByID("foreign-property-link")(foreignPropertyLink),
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
