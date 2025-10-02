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
import controllers.optIn.oldJourney.ConfirmTaxYearControllerISpec._
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import helpers.ITSAStatusUpdateConnectorStub
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.{NavBarFs, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Mandated, Voluntary}
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

object ConfirmTaxYearControllerISpec {
  val headingText = "Confirm and opt in for 2022 to 2023 tax year"
  val desc = "Opting in will mean you need to submit your quarterly updates through compatible software."
  val text: String = "If you have submitted any income and expenses for this tax year to HMRC, this will be deleted from our records. " +
    "So make sure you keep hold of this information because you will need to include it in your quarterly updates."
  val emptyBodyString = ""
  val confirmButton = "Confirm and save"
  val cancelButton = "Cancel"
}

object ConfirmNextTaxYearMessages {
  val headingText = "Confirm and opt in from 2023 to 2024 tax year onwards"
  val desc = "If you opt in for the next tax year, from 6 April 2023 you will need to submit your quarterly updates through compatible software."
  val emptyBodyString = ""
  val confirmButton = "Confirm and save"
  val cancelButton = "Cancel"
}

class ConfirmTaxYearControllerISpec extends ControllerISpecHelper {

  val forCurrentYearEnd: Int = dateService.getCurrentTaxYear.endYear
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forCurrentYearEnd)

  val forNextYearEnd: Int = forCurrentYearEnd + 1
  val nextTaxYear: TaxYear = TaxYear.forYearEnd(forNextYearEnd)

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val itsaStatusUpdateConnector: ITSAStatusUpdateConnector = app.injector.instanceOf[ITSAStatusUpdateConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/confirm-tax-year"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the confirm tax year for the current tax year" in {
            disable(NavBarFs)
            enable(ReportingFrequencyPage, SignUpFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            val intent = currentTaxYear
            setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent).futureValue shouldBe true

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(headingText),

              elementTextByID("confirm-tax-year-desc")(desc),
              elementTextByID("insetText_confirmYear")(text),

              elementTextByID("confirm-button")(confirmButton),
              elementTextByID("cancel-button")(cancelButton),
            )
          }

          "render the confirm tax year for the next tax year" in {
            disable(NavBarFs)
            enable(ReportingFrequencyPage, SignUpFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            val intent = nextTaxYear
            setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Annual, intent).futureValue shouldBe true

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(ConfirmNextTaxYearMessages.headingText),

              elementTextByID("confirm-tax-year-desc")(ConfirmNextTaxYearMessages.desc),

              elementTextByID("confirm-button")(confirmButton),
              elementTextByID("cancel-button")(cancelButton),
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to completed page" when {
            "the user is opting in for current tax year" in {
              disable(NavBarFs)
              enable(ReportingFrequencyPage, SignUpFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear))

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
                Status.NO_CONTENT, emptyBodyString
              )

              whenReady(buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty)) { result =>
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(Status.SEE_OTHER),
                  redirectURI(controllers.optIn.oldJourney.routes.OptInCompletedController.show(isAgent).url)
                )
              }
            }

            "the user is opting in for next tax year" in {
              disable(NavBarFs)
              enable(ReportingFrequencyPage, SignUpFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Annual, intent = nextTaxYear))

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
                Status.NO_CONTENT, emptyBodyString
              )

              whenReady(buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty)) { result =>
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(Status.SEE_OTHER),
                  redirectURI(controllers.optIn.oldJourney.routes.OptInCompletedController.show(isAgent).url)
                )
              }
            }
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

              whenReady(buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty)) { result =>
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(Status.SEE_OTHER),
                  redirectURI(controllers.optIn.oldJourney.routes.OptInErrorController.show(isAgent).url)
                )
              }
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
              currentTaxYear.toString, statusToString(status = currentYearStatus),
              statusToString(status = nextYearStatus))), Some(intent.toString)))))
  }

}