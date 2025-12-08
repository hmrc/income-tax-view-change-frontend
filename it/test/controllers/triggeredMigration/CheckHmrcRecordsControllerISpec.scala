/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.triggeredMigration

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.TriggeredMigration
import org.scalatest.Assertion
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants._

class CheckHmrcRecordsControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/check-your-active-businesses/hmrc-record"
  }

  object CheckHmrcRecordsMessages {
    val heading = "Check HMRC records only list your active businesses"
    val title = "Check HMRC records only list your active businesses"
    val desc = "You now have quarterly deadlines for your sole trader and/or property businesses listed here."
    val inset = "Making sure this page is correct will help avoid both missing deadlines for your active businesses and having deadlines for an income source you may have closed down or sold."
    val bulletStart = "If necessary, you must:"
    val bullet1 = "add any businesses that are missing"
    val bullet2 = "cease any that you no longer get income from"

    val yourActiveBusinessesHeading = "Your active businesses"
    val soleTraderHeading = "Sole trader businesses"
    val addASoleTraderBusinessText = "Add a sole trader business"
    val soleTraderGuidance = "You’re self-employed if you run your own business as an individual and work for yourself. This is also known as being a ’sole trader’. If you work through a limited company, you’re not a sole trader."

    val propertyHeading = "Property businesses"
    val ukPropertyHeading = "UK property"
    val foreignPropertyHeading = "Foreign property"
    val addAPropertyBusinessText = "Add a property business"
    val addForeignPropertyBusinessText = "Add foreign property business"
    val noActivePropertyText = "If you get income from one or more properties in the UK, you have a UK property business. If the property is abroad, you have a foreign property business. For example: letting houses, flats or holiday homes either on a long or short term basis."

    val ceaseText = "Cease"
    val businessNameText = "Business name"
    val businessStateText = "Business state"
    val activeText = "Active"
    val unknownText = "Unknown"

    val confirmRecordsHeading = "Confirm HMRC records only list your active businesses"
    val confirmRecordsText = "This page only needs to list all your active sole trader and property income sources. Any other business details that are not right, misspelt or out of date, can be amended at a later date."
    val confirmRecordsButton = "Confirm and continue"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is $mtdUserRole" that {
        "is Authenticated, with a valid enrolment" should {
          "render the Check HMRC Records page" that {
            "has an active sole trader business, uk property and foreignProperty" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)


              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)

                checkActiveSoleTrader(result)
                checkActiveForeignProperty(result)
                checkActiveUkProperty(result)
              }
            }
            "has an active sole trader business and uk property only" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)

                checkActiveSoleTrader(result)
                checkActiveUkProperty(result)
                checkPropertyLink(result)
              }
            }
            "has an active sole trader business and foreign property only" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessWithLatency)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)

                checkActiveSoleTrader(result)
                checkActiveForeignProperty(result)
                checkPropertyLink(result)
              }
            }
            "has an active uk property and foreign property only" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyBusiness)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)
                checkActiveForeignProperty(result)
                checkActiveUkProperty(result)
              }
            }
            "has an active sole trader business only" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)

                checkActiveSoleTrader(result)
                checkPropertyLink(result)
                checkNoProperty(result)
              }
            }
            "has an active uk property only" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)
                checkActiveUkProperty(result)
                checkPropertyLink(result)
              }
            }
            "has an active foreign property only" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)
                checkActiveForeignProperty(result)
                checkPropertyLink(result)
              }
            }
            "has no active businesses" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, allCeasedBusinesses)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)
                checkNoProperty(result)
                checkPropertyLink(result)
              }
            }

            "has active businesses with an unknown income source and business name" in {
              enable(TriggeredMigration)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponseWithUnknownAddressName)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                checkCommonContent(result, mtdUserRole)

                result should have(
                  elementTextByID("sole-trader-business-0")(CheckHmrcRecordsMessages.unknownText),
                  elementTextByID("sole-trader-business-name-0")(CheckHmrcRecordsMessages.businessNameText),
                  elementTextByID("sole-trader-business-name-value-0")(CheckHmrcRecordsMessages.unknownText),
                  elementTextByID("sole-trader-business-state-0")(CheckHmrcRecordsMessages.businessStateText),
                  elementTextByID("sole-trader-business-state-value-0")(CheckHmrcRecordsMessages.activeText),
                  elementTextByID("sole-trader-cease-link-0")(CheckHmrcRecordsMessages.ceaseText)
                )
              }
            }
          }
        }
      }
    }
  }

  def checkActiveSoleTrader(res: WSResponse) = {
    res should have(
      elementTextByID("sole-trader-business-0")("Fruit Ltd"),
      elementTextByID("sole-trader-business-name-0")(CheckHmrcRecordsMessages.businessNameText),
      elementTextByID("sole-trader-business-name-value-0")("business"),
      elementTextByID("sole-trader-business-state-0")(CheckHmrcRecordsMessages.businessStateText),
      elementTextByID("sole-trader-business-state-value-0")(CheckHmrcRecordsMessages.activeText),
      elementTextByID("sole-trader-cease-link-0")(CheckHmrcRecordsMessages.ceaseText)
    )
  }

  def checkActiveForeignProperty(res: WSResponse) = {
    res should have(
      elementTextByID("foreign-property-heading")(CheckHmrcRecordsMessages.foreignPropertyHeading),
      elementTextByID("foreign-property-business-state")(CheckHmrcRecordsMessages.businessStateText),
      elementTextByID("foreign-property-business-state-value")(CheckHmrcRecordsMessages.activeText),
      elementTextByID("foreign-property-cease-link")(CheckHmrcRecordsMessages.ceaseText)
    )
  }

  def checkNoProperty(res: WSResponse) = {
    res should have(
      elementTextByID("property-no-active-business-desc")(CheckHmrcRecordsMessages.noActivePropertyText),
    )
  }

  def checkActiveUkProperty(res: WSResponse) = {
    res should have(
      elementTextByID("uk-property-heading")(CheckHmrcRecordsMessages.ukPropertyHeading),
      elementTextByID("uk-property-business-state")(CheckHmrcRecordsMessages.businessStateText),
      elementTextByID("uk-property-business-state-value")(CheckHmrcRecordsMessages.activeText),
      elementTextByID("uk-property-cease-link")(CheckHmrcRecordsMessages.ceaseText)
    )
  }

  def checkPropertyLink(res: WSResponse) = {
    res should have(
      elementTextByID("add-property-link")(CheckHmrcRecordsMessages.addAPropertyBusinessText),
    )
  }

  def checkCommonContent(res: WSResponse, mtdUserRole: MTDUserRole): Assertion = {
    res should have(
      httpStatus(OK),
      pageTitle(mtdUserRole, CheckHmrcRecordsMessages.title),
      elementTextByID("check-hmrc-records-heading")(CheckHmrcRecordsMessages.heading),
      elementTextByID("check-hmrc-records-desc")(CheckHmrcRecordsMessages.desc),
      elementTextByID("check-hmrc-records-inset")(CheckHmrcRecordsMessages.inset),
      elementTextByID("check-hmrc-records-bullet-start")(CheckHmrcRecordsMessages.bulletStart),
      elementTextByID("check-hmrc-records-bullets")(s"${CheckHmrcRecordsMessages.bullet1} ${CheckHmrcRecordsMessages.bullet2}"),
      elementTextByID("your-active-businesses-heading")(CheckHmrcRecordsMessages.yourActiveBusinessesHeading),
      elementTextByID("sole-trader-guidance")(CheckHmrcRecordsMessages.soleTraderGuidance),
      elementTextByID("sole-trader-heading")(CheckHmrcRecordsMessages.soleTraderHeading),
      elementTextByID("property-heading")(CheckHmrcRecordsMessages.propertyHeading),
      elementTextByID("sole-trader-add-link")(CheckHmrcRecordsMessages.addASoleTraderBusinessText),
      elementTextByID("confirm-records-heading")(CheckHmrcRecordsMessages.confirmRecordsHeading),
      elementTextByID("confirm-records-desc")(CheckHmrcRecordsMessages.confirmRecordsText),
      elementTextByID("continue-button")(CheckHmrcRecordsMessages.confirmRecordsButton)
    )
  }
}
