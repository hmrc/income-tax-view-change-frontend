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

package controllers.manageBusinesses

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{DisplayBusinessStartDate, IncomeSourcesNewJourney, NavBarFs}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty}

class ManageYourBusinessesControllerISpec extends ControllerISpecHelper {

  val showIndividualViewIncomeSourceControllerUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  val pageTitleMsgKey = "manage.your.businesses.heading"
  val soleTraderBusinessesHeading = messagesAPI("manage.your.businesses.self-employed-h2")
  val propertyBusinessesHeading = messagesAPI("manage.your.businesses.property-h2")
  val dateStarted: String = messagesAPI("manage.your.businesses.datestarted")
  val businessName: String = messagesAPI("manage.your.businesses.name")
  val ukPropertyHeading: String = messagesAPI("manage.your.businesses.UK")
  val foreignPropertyHeading: String = messagesAPI("manage.your.businesses.Foreign")
  val ceasedBusinessHeading: String = messagesAPI("manage.your.businesses.ceasedBusinesses.heading")
  val ukPropertyStartDate: String = "1 January 2017"
  val foreignPropertyStartDate: String = "1 January 2018"

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the manage your businesses page" when {
            "the income sources is enabled and the user has multiple businesses and uk property with display business start date enabled" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("manage-your-businesses-h1")(messagesAPI(pageTitleMsgKey)),
                elementTextByID("self-employed-h1")(soleTraderBusinessesHeading),
                elementTextByID("business-type-0")("Fruit Ltd"),
                elementTextByID("business-trade-name-0")("business"),
                elementTextByID("business-date-0")(ukPropertyStartDate),
                elementTextByID("business-date-1")(foreignPropertyStartDate),
                elementTextByID("property-h2")(propertyBusinessesHeading),
                elementTextByID("uk-date")(ukPropertyStartDate),
                elementAttributeBySelector("#back-fallback", "href")(homeUrl(mtdUserRole)),
              )
            }

            "the income sources is enabled and the user has foreign property and ceased business with display business start date enabled" in {
              enable(IncomeSourcesNewJourney, DisplayBusinessStartDate)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("manage-your-businesses-h1")(messagesAPI(pageTitleMsgKey)),
                elementTextByID("foreign-date")(ukPropertyStartDate),
                elementTextByID("ceasedBusinesses-heading")(ceasedBusinessHeading)
              )
            }

            "the income sources is enabled and the user has multiple businesses and uk property with display business start date disabled" in {
              enable(IncomeSourcesNewJourney)
              disable(DisplayBusinessStartDate)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("manage-your-businesses-h1")(messagesAPI(pageTitleMsgKey)),
                elementTextByID("self-employed-h1")(soleTraderBusinessesHeading),
                elementTextByID("business-type-0")("Fruit Ltd"),
                elementTextByID("business-trade-name-0")("business"),
                elementTextByID("business-date-0")(""),
                elementTextByID("business-date-1")(""),
                elementTextByID("property-h2")(propertyBusinessesHeading),
                elementTextByID("uk-date")(""),
                elementAttributeBySelector("#back-fallback", "href")(homeUrl(mtdUserRole)),
              )
            }

            "the income sources is enabled and the user has foreign property and ceased business with display business start date disabled" in {
              enable(IncomeSourcesNewJourney)
              disable(DisplayBusinessStartDate)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("manage-your-businesses-h1")(messagesAPI(pageTitleMsgKey)),
                elementTextByID("foreign-date")(""),
                elementTextByID("ceasedBusinesses-heading")(ceasedBusinessHeading)
              )
            }
          }

          "redirect to the home page" when {
            "the income sources feature switch is disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              enable(DisplayBusinessStartDate)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
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