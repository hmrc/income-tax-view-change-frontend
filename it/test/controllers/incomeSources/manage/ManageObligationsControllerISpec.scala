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

package controllers.incomeSources.manage

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import models.incomeSourceDetails.{ManageIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.{testObligationsModel, testQuarterlyObligationDates}


class ManageObligationsControllerISpec extends ControllerISpecHelper {

  val annual = "annual"
  val quarterly = "quarterly"
  val taxYear = "2023-2024"

  val prefix: String = "incomeSources.add.manageObligations"
  val reusedPrefix: String = "business.added"

  def continueButtonText(incomeSource: IncomeSourceType): String = messagesAPI(s"$reusedPrefix.${incomeSource.messagesSuffix}.income.sources.button")

  val year = 2022
  val obligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    testQuarterlyObligationDates, Seq.empty, 2023, showPrevTaxYears = false
  )

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => foreignPropertyOnlyResponse
    }
  }

  def getExpectedHeading(incomeSourceType: IncomeSourceType, isAnnualChange: Boolean): String = {
    val changeToKey = if(isAnnualChange) "annually" else "quarterly"
    val tradingNameOrProperyMessage = incomeSourceType match {
      case SelfEmployment => business1.tradingName.getOrElse("")
      case _ => messagesAPI(s"$prefix.${incomeSourceType.messagesSuffix}")
    }
    messagesAPI(s"$prefix.title", tradingNameOrProperyMessage, changeToKey, "2023", "2024")
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => s"/business-will-report"
      case UkProperty => s"/uk-property-will-report"
      case ForeignProperty => s"/foreign-property-will-report"
    }
    pathStart + "/income-sources/manage" + pathEnd
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdUserRole =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Manage Obligations Page" when {
              "valid url parameters provided and change to is annual" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"MANAGE-${incomeSourceType.key}",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(annual), Some(2024), Some(true))))))
                IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

                val pathWithValidQueryParams = path + s"?changeTo=$annual&taxYear=$taxYear"
                val result = buildGETMTDClient(pathWithValidQueryParams, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)


                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getExpectedHeading(incomeSourceType, true)),
                  elementTextByID("continue-button")(continueButtonText(incomeSourceType))
                )
              }

              "valid url parameters provided and change to is quarterly" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"MANAGE-${incomeSourceType.key}",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(quarterly), Some(2024), Some(true))))))
                IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

                val pathWithValidQueryParams = path + s"?changeTo=$quarterly&taxYear=$taxYear"
                val result = buildGETMTDClient(pathWithValidQueryParams, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getExpectedHeading(incomeSourceType, false)),
                  elementTextByID("continue-button")(continueButtonText(incomeSourceType))
                )
              }
            }

            "redirect to the home page" when {
              "the income sources feature switch is disabled" in {
                disable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))
                val pathWithValidQueryParams = path + s"?changeTo=$quarterly&taxYear=$taxYear"
                val result = buildGETMTDClient(pathWithValidQueryParams, additionalCookies).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

            if (incomeSourceType == SelfEmployment) {
              "render the error page" when {
                "there is no incomeSourceId in the session" in {
                  enable(IncomeSourcesFs)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))
                  await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"MANAGE-${incomeSourceType.key}",
                    manageIncomeSourceData = None)))
                  IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

                  val pathWithValidQueryParams = path + s"?changeTo=$annual&taxYear=$taxYear"
                  val result = buildGETMTDClient(pathWithValidQueryParams, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  result should have(
                    httpStatus(INTERNAL_SERVER_ERROR)
                  )
                }
              }
            }
          }
          testAuthFailures(path + s"?changeTo=$annual&taxYear=$taxYear", mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "redirect to ManageIncomeSources" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

              val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(routes.ManageIncomeSourceController.show(mtdUserRole != MTDIndividual).url)
              )
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
  }
}
