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

package controllers.agent.manageBusinesses.manage

import controllers.agent.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import auth.MtdItUser
import models.admin.IncomeSourcesFs
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Manage
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSources, NavBarFs}
import models.incomeSourceDetails.{LatencyDetails, ManageIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class ConfirmReportingMethodSharedControllerISpec extends ControllerISpecHelper {

  val annual = "Annual"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2023-2024"
  val timestamp = "2023-01-31T09:26:17Z"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val latencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
      taxYear1 = taxYear1.toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = taxYear2.toString,
      latencyIndicator2 = annuallyIndicator
    )

  val pathSE = "/agents/manage-your-businesses/manage/confirm-you-want-to-report"
  val pathUK = "/agents/manage-your-businesses/manage/confirm-you-want-to-report-uk-property "
  val pathOV = "/agents/manage-your-businesses/manage/confirm-you-want-to-report-foreign-property"

  private lazy val checkYourAnswersController = controllers.manageBusinesses.manage.routes
    .CheckYourAnswersController

  val checkYourAnswersShowUKPropertyUrl: String = checkYourAnswersController
    .show(isAgent = false, UkProperty).url
  val checkYourAnswersShowForeignPropertyUrl: String = checkYourAnswersController
    .show(isAgent = false, ForeignProperty).url
  val checkYourAnswersShowSelfEmploymentUrl: String = checkYourAnswersController
    .show(isAgent = false, SelfEmployment).url

  val prefix: String = "incomeSources.manage.propertyReportingMethod"

  val continueButtonText: String = messagesAPI("base.confirm-this-change")

  val pageTitle = messagesAPI(s"$prefix.heading.annual")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))


  s"GET $pathSE" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        " is authenticated, with a valid MTD enrolment" should {
          "render the Confirm Reporting Method page" when {
            "all query parameters are valid" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              val result = buildGETMTDClient(pathSE, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitleAgent(pageTitle),
                elementTextByID("confirm-button")(continueButtonText)
              )
            }
          }

          "redirect to home page" when {
            "Income Sources FS is Disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              val result = buildGETMTDClient(pathSE, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.show().url)
              )
            }
          }

        }
        testAuthFailuresForMTDAgent(pathSE, isSupportingAgent)
      }
    }
  }

  s"GET $pathUK" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        " is authenticated, with a valid MTD enrolment" should {
          "render the Confirm Reporting Method page" when {
            "all query parameters are valid" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val result = buildGETMTDClient(pathUK, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitleAgent(pageTitle),
                elementTextByID("confirm-button")(continueButtonText)
              )
            }
          }

          "redirect to home page" when {
            "Income Sources FS is Disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val result = buildGETMTDClient(pathUK, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.show().url)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(pathUK, isSupportingAgent)
      }
    }
  }

  s"GET $pathOV" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        " is authenticated, with a valid MTD enrolment" should {
          "render the Confirm Reporting Method page" when {
            "all query parameters are valid" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val result = buildGETMTDClient(pathOV, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitleAgent(pageTitle),
                elementTextByID("confirm-button")(continueButtonText)
              )
            }
          }

          "redirect to home page" when {
            "Income Sources FS is Disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val result = buildGETMTDClient(pathOV, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.show().url)
              )
            }
          }

        }
        testAuthFailuresForMTDAgent(pathUK, isSupportingAgent)
      }
    }
  }


  s"POST $pathSE" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        " is authenticated, with a valid MTD enrolment" should {
          s"redirect to $checkYourAnswersShowSelfEmploymentUrl" when {
            "called with a valid form" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathSE, additionalCookies, formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(checkYourAnswersShowSelfEmploymentUrl)
              )

              testAuthFailuresForMTDAgent(pathSE, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

          s"return ${Status.BAD_REQUEST}" when {
            "called with a invalid form" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              val formData =  Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathSE, additionalCookies, formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )

              testAuthFailuresForMTDAgent(pathSE, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              val formData =  Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathSE, additionalCookies, formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.show().url)
              )

              testAuthFailuresForMTDAgent(pathSE, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

        }
      }
    }
  }

  s"POST $pathUK" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        " is authenticated, with a valid MTD enrolment" should {
          s"redirect to $checkYourAnswersShowUKPropertyUrl" when {
            "called with a valid form" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val formData =   Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathUK, additionalCookies, formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(checkYourAnswersShowUKPropertyUrl)
              )

              testAuthFailuresForMTDAgent(pathUK, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

          s"return ${Status.BAD_REQUEST}" when {
            "called with a invalid form" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val formData =  Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathUK, additionalCookies, formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )

              testAuthFailuresForMTDAgent(pathUK, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val formData =   Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathUK, additionalCookies, formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.show().url)
              )

              testAuthFailuresForMTDAgent(pathUK, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

        }
      }
    }
  }

  s"POST $pathOV" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        " is authenticated, with a valid MTD enrolment" should {
          s"redirect to $checkYourAnswersShowForeignPropertyUrl" when {
            "called with a valid form" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val formData =   Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathOV, additionalCookies, formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(checkYourAnswersShowForeignPropertyUrl)
              )

              testAuthFailuresForMTDAgent(pathOV, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

          s"return ${Status.BAD_REQUEST}" when {
            "called with a invalid form" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val formData =  Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathOV, additionalCookies, formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )

              testAuthFailuresForMTDAgent(pathOV, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val formData =   Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathOV, additionalCookies, formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.show().url)
              )

              testAuthFailuresForMTDAgent(pathOV, isSupportingAgent, optBody = Some(Map
              (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
            }
          }

        }
      }
    }
  }
}
