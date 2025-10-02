/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optIn.oldJourney

import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import controllers.ControllerISpecHelper
import controllers.optIn.oldJourney.OptInCheckYourAnswersControllerISpec._
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import helpers.ITSAStatusUpdateConnectorStub
import helpers.servicemocks.AuditStub.verifyAudit
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.{NavBarFs, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import play.mvc.Http.Status.BAD_REQUEST
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

import scala.concurrent.Future

object OptInCheckYourAnswersControllerISpec {
  val headingText = "Check your answers"
  val optInSummary = "Opting in will mean you need to submit your quarterly updates through compatible software."
  val optInSummaryNextYear = "If you opt in from the next tax year onwards, from 6 April 2023 you will need to submit " +
    "your quarterly updates through compatible software."
  val optin = "Opt in from"
  val selectTaxYear = "2022 to 2023 tax year onwards"
  val selectTaxYearNextYear = "2023 to 2024 tax year onwards"
  val change = "Change"

  val emptyBodyString = ""
}

class OptInCheckYourAnswersControllerISpec extends ControllerISpecHelper {

  val forYearEnd = dateService.getCurrentTaxYear.endYear
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val itsaStatusUpdateConnector: ITSAStatusUpdateConnector = app.injector.instanceOf[ITSAStatusUpdateConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/check-your-answers"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"render opt-in check-your-answers page" in {
            disable(NavBarFs)
            enable(ReportingFrequencyPage, SignUpFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            val intent = currentTaxYear
            await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent))

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(headingText),

              elementTextBySelector(".govuk-summary-list__key")(optin),
              elementTextBySelector(".govuk-summary-list__value")(selectTaxYear),
              elementTextBySelector("#change")(change),

              elementTextBySelector("#optIn-summary")(optInSummary),
              elementTextByID("confirm-button")("Confirm and save"),
              elementTextByID("cancel-button")("Cancel"),
            )
          }
          s"render opt-in check-your-answers page 2" in {
            disable(NavBarFs)
            enable(ReportingFrequencyPage, SignUpFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            val intent = currentTaxYear.nextYear
            await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent))

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(headingText),

              elementTextBySelector(".govuk-summary-list__key")(optin),
              elementTextBySelector(".govuk-summary-list__value")(selectTaxYearNextYear),
              elementTextBySelector("#change")(change),

              elementTextBySelector("#optIn-summary")(optInSummaryNextYear),

              elementTextByID("confirm-button")("Confirm and save"),
              elementTextByID("cancel-button")("Cancel"),
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to optIn complete page" in {
            disable(NavBarFs)
            enable(ReportingFrequencyPage, SignUpFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear))

            ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
              Status.NO_CONTENT, emptyBodyString
            )

            val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
            verifyAudit()

            result should have(
              httpStatus(Status.SEE_OTHER),
              redirectURI(controllers.optIn.oldJourney.routes.OptInCompletedController.show(isAgent).url)
            )
          }

          "redirect to the optIn error page" when {
            "no tax-year choice is made" in {
              disable(NavBarFs)
              enable(ReportingFrequencyPage, SignUpFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary, currentTaxYear))

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
                BAD_REQUEST, Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString()
              )

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
              verifyAudit()

              result should have(
                httpStatus(Status.SEE_OTHER),
                redirectURI(controllers.optIn.oldJourney.routes.OptInErrorController.show(isAgent).url)
              )
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
  }

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value,
                                    nextYearStatus: ITSAStatus.Value, intent: TaxYear): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString,
              statusToString(status = currentYearStatus),
              statusToString(status = nextYearStatus))),
            Some(intent.toString)
          ))))
  }
}