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

package businessDetails.controllers.manageBusinesses.manage

import businessDetails.enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import businessDetails.forms.manageBusinesses.manage.ChangeReportingMethodForm
import businessDetails.models.incomeSourceDetails.ManageIncomeSourceData
import businessDetails.models.updateIncomeSource.UpdateIncomeSourceResponseModel
import businessDetails.services.SessionService
import businessDetails.testConstants.BusinessDetailsIntegrationTestConstants.*
import common.controllers.ControllerISpecHelper
import common.enums.{MTDIndividual, MTDUserRole}
import common.helpers.GetInsourceDetailsStub
import common.models.incomeSourceDetails.LatencyDetails
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import common.testConstants.BaseIntegrationTestConstants.*
import shared.enums.JourneyType.{IncomeSourceJourneyType, Manage}
import shared.models.UIJourneySessionData

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

  def getPath(mtdUserRole: MTDUserRole, incomeSourceType: IncomeSourceType, newReportingMethod: String): String = {
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => "/confirm-you-want-to-report"
      case UkProperty => "/confirm-you-want-to-report-uk-property"
      case _ => "/confirm-you-want-to-report-foreign-property"
    }
    pathStart + "/manage-your-businesses/manage" + pathEnd + s"?taxYear=$taxYear&changeTo=$newReportingMethod"
  }

  private lazy val manageObligationsController = routes.ManageObligationsController

  val newPrefix: String = "manageBusinesses.manage.propertyReportingMethod.new"
  val oldPrefix: String = "manageBusinesses.manage.propertyReportingMethod"

  val continueButtonText: String = "Continue"
  val oldConfirmButtonText: String = "Confirm and save"

  def mainPageTitle(newReportingMethod: String): String = messagesAPI(s"$newPrefix.heading.$newReportingMethod")
  def oldMainPageTitle(newReportingMethod: String): String = messagesAPI(s"$oldPrefix.heading.$newReportingMethod")

  val allReportingMethods: Seq[String] = List("annual", "quarterly")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))

  mtdAllRoles.foreach { mtdUserRole =>
    allReportingMethods.foreach { reportingMethod =>
      val isAgent = mtdUserRole != MTDIndividual
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      val pathSE = getPath(mtdUserRole, SelfEmployment, reportingMethod)
      s"GET $pathSE" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Confirm Reporting Method page" when {
              "all query parameters are valid" in {
                stubAuthorised(mtdUserRole)
                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

                GetInsourceDetailsStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                val result = buildGETMTDClient(pathSE, additionalCookies).futureValue
                GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, mainPageTitle(reportingMethod)),
                  elementTextByID("continue-button")(continueButtonText)

                )
              }
            }

          }
          testAuthFailures(pathSE, mtdUserRole)
        }
      }

      val pathUK = getPath(mtdUserRole, UkProperty, reportingMethod)
      s"GET $pathUK" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Confirm Reporting Method page" when {
              "all query parameters are valid" in {
                stubAuthorised(mtdUserRole)
                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

                GetInsourceDetailsStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val result = buildGETMTDClient(pathUK, additionalCookies).futureValue
                GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, mainPageTitle(reportingMethod)),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }
            }

          }
          testAuthFailures(pathUK, mtdUserRole)
        }
      }

      val pathFP = getPath(mtdUserRole, ForeignProperty, reportingMethod)
      s"GET $pathFP" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Confirm Reporting Method page" when {
              "all query parameters are valid" in {
                stubAuthorised(mtdUserRole)

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

                GetInsourceDetailsStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val result = buildGETMTDClient(pathFP, additionalCookies).futureValue
                GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, mainPageTitle(reportingMethod)),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }

            }

          }
          testAuthFailures(pathFP, mtdUserRole)
        }
      }


      s"POST $pathSE" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"redirect to business will report (completion) page" when {
              "called with a valid form" in {
                stubAuthorised(mtdUserRole)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

                val formData = Map("change-reporting-method-check" -> Seq("Yes"))

                val result = buildPOSTMTDPostClient(pathSE, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(manageObligationsController.show(isAgent, SelfEmployment).url)
                )
              }
            }
          }

          testAuthFailures(pathSE, mtdUserRole, optBody = Some(Map
          (ChangeReportingMethodForm.response -> Seq("Yes")
          )))
        }
      }

      s"POST $pathUK" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"redirect to business will report (completion) page" when {
              "called with a valid form" in {
                stubAuthorised(mtdUserRole)

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val formData = Map("change-reporting-method-check" -> Seq("Yes"))

                val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(manageObligationsController.show(isAgent, UkProperty).url)
                )
              }
            }

            testAuthFailures(pathUK, mtdUserRole, optBody = Some(Map
            (ChangeReportingMethodForm.response -> Seq("Yes")
            )))
          }
        }
      }

      s"POST $pathFP" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"redirect to business will report (completion) page" when {
              "called with a valid form" in {
                stubAuthorised(mtdUserRole)
                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val formData = Map("change-reporting-method-check" -> Seq("Yes"))

                val result = buildPOSTMTDPostClient(pathFP, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(manageObligationsController.show(isAgent, ForeignProperty).url)
                )
              }
            }
            testAuthFailures(pathFP, mtdUserRole,
              Some(Map(ChangeReportingMethodForm.response -> Seq("Yes")
              )))
          }
        }
      }
    }
  }
}