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

package controllers.optOut

import audit.models.{OptOutAuditModel, Outcome}
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import controllers.ControllerISpecHelper
import controllers.constants.ConfirmOptOutControllerConstants._
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ITSAStatusUpdateConnectorStub, OptOutSessionRepositoryHelper}
import models.admin.{NavBarFs, OptOutFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.mvc.Http.Status.{NO_CONTENT, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmOptOutControllerISpec extends ControllerISpecHelper {

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/optout/review-confirm-taxyear"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"render confirm multi-year opt out page with supported ITSAStatus for each year" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear(dateService),
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = Voluntary,
              nextYearStatus = Voluntary)

            assert(optOutSessionDataRepository.saveIntent(TaxYear.getTaxYearModel("2023-2024").get).futureValue)

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(optOutExpectedTitle),
              elementTextBySelector(".govuk-summary-list__value")("2023 to 2024 tax year onwards"),
              elementTextByID("optOut-summary")(summary),
              elementTextByID("optOut-warning")(infoMessage),
            )
          }
          s"render confirm multi-year opt out page when NoStatus for CY+1 is present" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear(dateService),
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = Annual,
              nextYearStatus = NoStatus)

            assert(optOutSessionDataRepository.saveIntent(TaxYear.getTaxYearModel("2023-2024").get).futureValue)

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(expectedTitle(dateService)),
              elementTextByID("summary")(summary),
              elementTextByID("info-message")(infoMessage),
            )
          }
          s"throw an exception for multi-year opt out page when when NoStatus for CY-1 is present" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            assertThrows[RuntimeException] {
              helper.stubOptOutInitialState(currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = NoStatus,
                currentYearStatus = Voluntary,
                nextYearStatus = Annual)
            }          }
          s"throw an exception for multi-year opt out page when when NoStatus for CY is present" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            assertThrows[RuntimeException] {
              helper.stubOptOutInitialState(currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = NoStatus,
                nextYearStatus = Annual)
            }          }
          s"throw an exception for multi-year opt out page when unsupported statuses are present" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            assertThrows[RuntimeException] {
              helper.stubOptOutInitialState(currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = DigitallyExempt,
                nextYearStatus = Exempt)
            }          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to confirmed page" when {
            "user confirms opt-out for multi-year scenario with supported ITSAStatus for each year" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val taxYear = TaxYear(2022, 2023)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              helper.stubOptOutInitialState(
                currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = Annual,
                nextYearStatus = Annual
              )

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(
                taxableEntityId = propertyOnlyResponse.nino,
                status = Status.NO_CONTENT,
                responseBody = emptyBodyString
              )

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(routes.ConfirmedOptOutController.show(isAgent).url)
              )

              val mtdUser = testUser(mtdUserRole)

              verifyAuditEvent(
                OptOutAuditModel(
                  saUtr = mtdUser.saUtr,
                  credId = mtdUser.credId,
                  userType = mtdUser.userType,
                  agentReferenceNumber = mtdUser.arn,
                  mtditid = mtdUser.mtditid,
                  nino = testNino,
                  outcome = Outcome(isSuccessful = true, None, None),
                  optOutRequestedFromTaxYear = taxYear.previousYear.formatAsShortYearRange,
                  currentYear = taxYear.formatAsShortYearRange,
                  `beforeITSAStatusCurrentYear-1` = Voluntary,
                  beforeITSAStatusCurrentYear = Annual,
                  `beforeITSAStatusCurrentYear+1` = Annual,
                  `afterAssumedITSAStatusCurrentYear-1` = Annual,
                  afterAssumedITSAStatusCurrentYear = Annual,
                  `afterAssumedITSAStatusCurrentYear+1` = Annual,
                  `currentYear-1Crystallised` = false
                )
              )
            }
            "user confirms opt-out for multi-year scenario with supported ITSAStatus for each year and missing header" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              helper.stubOptOutInitialState(currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = Annual,
                nextYearStatus = Annual)

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
                Status.NO_CONTENT, emptyBodyString,
                Map("missing-header-name" -> "missing-header-value")
              )

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

              result should have(
                httpStatus(Status.SEE_OTHER),
                redirectURI(routes.ConfirmedOptOutController.show(isAgent).url)
              )
            }
            "user confirms opt-out for multi-year scenario when CY+1 is NoStatus" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val taxYear = TaxYear(2022, 2023)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              helper.stubOptOutInitialState(
                currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = Annual,
                nextYearStatus = NoStatus
              )

              ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(
                taxableEntityId = propertyOnlyResponse.nino,
                status = Status.NO_CONTENT,
                responseBody = emptyBodyString
              )

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(routes.ConfirmedOptOutController.show(isAgent).url)
              )

              val mtdUser = testUser(mtdUserRole)

              verifyAuditEvent(
                OptOutAuditModel(
                  saUtr = mtdUser.saUtr,
                  credId = mtdUser.credId,
                  userType = mtdUser.userType,
                  agentReferenceNumber = mtdUser.arn,
                  mtditid = mtdUser.mtditid,
                  nino = testNino,
                  outcome = Outcome(isSuccessful = true, None, None),
                  optOutRequestedFromTaxYear = taxYear.previousYear.formatAsShortYearRange,
                  currentYear = taxYear.formatAsShortYearRange,
                  `beforeITSAStatusCurrentYear-1` = Voluntary,
                  beforeITSAStatusCurrentYear = Annual,
                  `beforeITSAStatusCurrentYear+1` = NoStatus,
                  `afterAssumedITSAStatusCurrentYear-1` = Annual,
                  afterAssumedITSAStatusCurrentYear = Annual,
                  `afterAssumedITSAStatusCurrentYear+1` = NoStatus,
                  `currentYear-1Crystallised` = false
                )
              )
            }
          }
          "Redirect to OptOut Error page" when {
            "user confirms opt-out for multi-year scenario and update fails" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val taxYear: TaxYear = TaxYear(2023, 2024)

              val auditModel =
                OptOutAuditModel(
                  saUtr = csbTestUser.saUtr,
                  credId = csbTestUser.credId,
                  userType = csbTestUser.userType,
                  agentReferenceNumber = csbTestUser.arn,
                  mtditid = csbTestUser.mtditid,
                  nino = testNino,
                  outcome = Outcome(isSuccessful = false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")),
                  optOutRequestedFromTaxYear = taxYear.previousYear.formatAsShortYearRange,
                  currentYear = taxYear.formatAsShortYearRange,
                  `beforeITSAStatusCurrentYear-1` = Voluntary,
                  beforeITSAStatusCurrentYear = Voluntary,
                  `beforeITSAStatusCurrentYear+1` = Voluntary,
                  `afterAssumedITSAStatusCurrentYear-1` = Voluntary,
                  afterAssumedITSAStatusCurrentYear = Voluntary,
                  `afterAssumedITSAStatusCurrentYear+1` = Annual,
                  `currentYear-1Crystallised` = false
                )

              stubFor(
                post(urlEqualTo("/write/audit"))
                  .withRequestBody(equalToJson(Json.toJson(auditModel).toString(), true, true))
                  .willReturn(aResponse().withStatus(NO_CONTENT))
              )

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              helper.stubOptOutInitialState(
                currentTaxYear(dateService),
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = Voluntary,
                nextYearStatus = Voluntary
              )

              ITSAStatusUpdateConnectorStub
                .stubItsaStatusUpdate(
                  taxableEntityId = propertyOnlyResponse.nino,
                  status = BAD_REQUEST,
                  responseBody = Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString(),
                )

              optOutSessionDataRepository.saveIntent(TaxYear.getTaxYearModel("2023-2024").get).futureValue shouldBe true

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(routes.OptOutErrorController.show(isAgent).url)
              )

              def verifyAuditEvent(optOutAuditModel: OptOutAuditModel): Unit = {
                verify(
                  postRequestedFor(urlEqualTo("/write/audit"))

                    .withRequestBody(matchingJsonPath("$.auditSource", equalTo("income-tax-view-change-frontend")))
                    .withRequestBody(matchingJsonPath("$.auditType", equalTo(optOutAuditModel.auditType)))
                    .withRequestBody(matchingJsonPath("$.tags.transactionName", equalTo(optOutAuditModel.transactionName)))

                    .withRequestBody(matchingJsonPath("$.detail.nino", equalTo(optOutAuditModel.nino)))
                    .withRequestBody(matchingJsonPath("$.detail.outcome.isSuccessful", equalTo(optOutAuditModel.outcome.isSuccessful.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.outcome.failureCategory", equalTo(optOutAuditModel.outcome.failureCategory.getOrElse(""))))
                    .withRequestBody(matchingJsonPath("$.detail.outcome.failureReason", equalTo(optOutAuditModel.outcome.failureReason.getOrElse(""))))

                    .withRequestBody(matchingJsonPath("$.detail.beforeITSAStatusCurrentYear-1", equalTo(optOutAuditModel.`beforeITSAStatusCurrentYear-1`.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.beforeITSAStatusCurrentYear", equalTo(optOutAuditModel.beforeITSAStatusCurrentYear.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.beforeITSAStatusCurrentYear+1", equalTo(optOutAuditModel.`beforeITSAStatusCurrentYear+1`.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.afterAssumedITSAStatusCurrentYear-1", equalTo(optOutAuditModel.`afterAssumedITSAStatusCurrentYear-1`.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.afterAssumedITSAStatusCurrentYear", equalTo(optOutAuditModel.afterAssumedITSAStatusCurrentYear.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.afterAssumedITSAStatusCurrentYear+1", equalTo(optOutAuditModel.`afterAssumedITSAStatusCurrentYear+1`.toString)))
                    .withRequestBody(matchingJsonPath("$.detail.currentYear-1Crystallised", equalTo(optOutAuditModel.`currentYear-1Crystallised`.toString)))
                )
              }

              verifyAuditEvent(auditModel)
            }
            "throw an exception when CY is NoStatus" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              assertThrows[RuntimeException] {
                helper.stubOptOutInitialState(
                  currentTaxYear(dateService),
                  previousYearCrystallised = false,
                  previousYearStatus = Voluntary,
                  currentYearStatus = NoStatus,
                  nextYearStatus = Annual
                )
              }
            }
            "throw an exception when CY-1 is NoStatus" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              assertThrows[RuntimeException] {
                helper.stubOptOutInitialState(
                  currentTaxYear(dateService),
                  previousYearCrystallised = false,
                  previousYearStatus = NoStatus,
                  currentYearStatus = Voluntary,
                  nextYearStatus = Annual
                )
              }
            }
          }

        }
        testAuthFailures(path, mtdUserRole, Some(Map.empty))
      }
    }
  }
}
