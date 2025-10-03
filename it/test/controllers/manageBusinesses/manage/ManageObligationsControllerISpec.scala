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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.Manage
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.{NavBarFs, OptInOptOutContentUpdateR17}
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import models.incomeSourceDetails.ManageIncomeSourceData
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

  val continueButtonText: String = messagesAPI(s"$reusedPrefix.income.sources.button")

  val year = 2022
  val obligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    testQuarterlyObligationDates, Seq.empty, 2023, showPrevTaxYears = false
  )

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Manage))
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => foreignPropertyOnlyResponse
    }
  }

  def getExpectedHeading(
                          incomeSourceType: IncomeSourceType,
                          isAnnualChange: Boolean,
                          useOptInOptOutR17: Boolean
                        ): String = {
    val tradingNameOrPropertyMessage = incomeSourceType match {
      case SelfEmployment => business1.tradingName.getOrElse("")
      case _ => messagesAPI(s"$prefix.${incomeSourceType.messagesSuffix}")
    }

    val (messageKey, changeVerb) =
      if (useOptInOptOutR17) {
        val verb = if (isAnnualChange) messagesAPI(s"$prefix.OptInOptOutContentUpdateR17.panel.optedOut")
        else messagesAPI(s"$prefix.OptInOptOutContentUpdateR17.panel.signedUp")
        (s"$prefix.OptInOptOutContentUpdateR17.title", verb)
      } else {
        val frequency = if (isAnnualChange) "annually" else "quarterly"
        (s"$prefix.title", frequency)
      }

    messagesAPI(messageKey, tradingNameOrPropertyMessage, changeVerb, "2023", "2024")
  }



  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => s"/business-will-report"
      case UkProperty => s"/uk-property-will-report"
      case ForeignProperty => s"/foreign-property-will-report"
    }
    pathStart + "/manage-your-businesses/manage" + pathEnd
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
                  pageTitle(mtdUserRole, getExpectedHeading(incomeSourceType, isAnnualChange = true, useOptInOptOutR17 = false)),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }

              "valid url parameters provided and change to is quarterly" in {
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
                  pageTitle(mtdUserRole, getExpectedHeading(incomeSourceType, isAnnualChange = false, useOptInOptOutR17 = false)),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }

              "valid url parameters provided and change to is annual when OptInOptOutContentUpdateR17 is enabled" in {
                enable(OptInOptOutContentUpdateR17)
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
                  pageTitle(mtdUserRole, getExpectedHeading(incomeSourceType, isAnnualChange = true, useOptInOptOutR17 = true)
                  ),
                )
              }

              "valid url parameters provided and change to is quarterly when OptInOptOutContentUpdateR17 is enabled" in {
                enable(OptInOptOutContentUpdateR17)
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
                  pageTitle(mtdUserRole, getExpectedHeading(incomeSourceType, isAnnualChange = false, useOptInOptOutR17 = true)
                  ),
                )
              }

            }

            "render the error page" when {
              "there is no incomeSourceId in the session" in {
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
          testAuthFailures(path + s"?changeTo=$annual&taxYear=$taxYear", mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "redirect to ManageIncomeSources" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

              val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
              if(mtdUserRole != MTDIndividual)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)
                )
              else
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url)
                )
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
  }
}
