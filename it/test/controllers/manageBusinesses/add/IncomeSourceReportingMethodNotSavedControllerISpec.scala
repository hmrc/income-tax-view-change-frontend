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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.NavBarFs
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class IncomeSourceReportingMethodNotSavedControllerISpec extends ControllerISpecHelper {

  object TestConstants {
    val selfEmployment: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.se.incomeSource")
    val seParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmployment)

    val ukProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.uk.incomeSource")
    val ukParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", ukProperty)

    val foreignProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.fp.incomeSource")
    val foreignParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", foreignProperty)

    val continueButtonText: String = messagesAPI("base.continue")

    def getParagraph(incomeSourceType: IncomeSourceType): String = {
      incomeSourceType match {
        case SelfEmployment => seParagraph
        case UkProperty => ukParagraph
        case ForeignProperty => foreignParagraph
      }
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => noPropertyOrBusinessResponse
    }
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/error-business-reporting-method-not-saved"
      case UkProperty => s"$pathStart/add-uk-property/error-uk-property-reporting-method-not-saved"
      case ForeignProperty => s"$pathStart/add-foreign-property/error-foreign-property-reporting-method-not-saved"
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the reporting method not saved page" when {
              "using the manage businesses journey" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, expectedText),
                  elementTextByID("paragraph-1")(TestConstants.getParagraph(incomeSourceType))
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
