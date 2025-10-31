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

import audit.models.ManageIncomeSourceCheckYourAnswersAuditModel
import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.incomeSourceDetails.{LatencyDetails, ManageIncomeSourceData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class CheckYourAnswersControllerISpec extends ControllerISpecHelper {

  val annual = "annual"
  val quarterly = "quarterly"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2024"
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

  val prefix: String = "manageBusinesses.check-answers"

  val continueButtonText: String = messagesAPI("manageBusinesses.check-answers.confirm")

  val pageTitle = messagesAPI(s"$prefix.text")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses/manage" else "/agents/manage-your-businesses/manage"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => "/business-check-your-answers"
      case UkProperty => "/uk-property-check-your-answers"
      case ForeignProperty => "/foreign-property-check-your-answers"
    }
    pathStart + pathEnd
  }

  def verifyCheckAnswersAudit(incomeSourceType: IncomeSourceType, mtdUserRole: MTDUserRole) = {
    val businessName = incomeSourceType match {
      case SelfEmployment => "business"
      case UkProperty => "UK property"
      case ForeignProperty => "Foreign property"
    }
    AuditStub.verifyAuditContainsDetail(
      ManageIncomeSourceCheckYourAnswersAuditModel(
        true, incomeSourceType.journeyType,
        "MANAGE", "Annually",
        "2023-2024", businessName
      )(getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)).detail)

  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => singleBusinessResponseInLatencyPeriod(latencyDetails)
      case UkProperty => singleUKPropertyResponseInLatencyPeriod(latencyDetails)
      case ForeignProperty => singleForeignPropertyResponseInLatencyPeriod(latencyDetails)
    }
  }

  def setupMongoSessionData(incomeSourceType: IncomeSourceType) = {
    val incomeId = incomeSourceType match {
      case SelfEmployment => testSelfEmploymentId
      case _ => testPropertyIncomeId
    }
    await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"MANAGE-${incomeSourceType.key}",
      manageIncomeSourceData = Some(ManageIncomeSourceData(Some(incomeId), Some(annual), Some(taxYear.toInt), Some(false))))))
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdUserRole =>
      val isAgent = mtdUserRole != MTDIndividual
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Check your answers page" when {
              "all session parameters are valid" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))
                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                if (incomeSourceType == SelfEmployment) {
                  await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                    manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(annual), Some(taxYear.toInt), Some(false))))))
                } else {
                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                }

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, pageTitle),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"redirect to manage obligations" when {
              "submitted with valid session data" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))
                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                setupMongoSessionData(incomeSourceType)

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                verifyCheckAnswersAudit(incomeSourceType, mtdUserRole)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.ManageObligationsController.show(isAgent, incomeSourceType).url)
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
  }
}
