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
import controllers.constants.ConfirmOptOutControllerConstants._
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub, OptOutSessionRepositoryHelper}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.mvc.Http.Status.{NO_CONTENT, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmOptOutControllerISpec extends ComponentSpecBase {

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  s"GET $confirmOptOutPageUrl" when {

    "user is authorised" should {
      s"render confirm single year opt out page $confirmOptOutPageUrl" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear(dateService),
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = NoStatus)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmOptOut()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(expectedTitle(dateService)),
          elementTextByID("summary")(summary),
          elementTextByID("info-message")(infoMessage),
        )
      }
    }

    "user is authorised" should {
      s"render confirm multi-year opt out page $confirmOptOutPageUrl" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear(dateService),
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary)

        assert(optOutSessionDataRepository.saveIntent(TaxYear.getTaxYearModel("2023-2024").get).futureValue)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmOptOut()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(optOutExpectedTitle),
          elementTextBySelector(".govuk-summary-list__value")("2023 to 2024 tax year onwards"),
          elementTextByID("optOut-summary")(summary),
          elementTextByID("optOut-warning")(infoMessage),
        )
      }
    }
  }

  s"POST $submitConfirmOptOutPageUrl" when {

    "user confirms opt-out for one-year scenario" should {

      "show opt-out complete page and send audit event" in {

        val taxYear = TaxYear(2022, 2023)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(
          currentTaxYear(dateService),
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus
        )

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(
          taxableEntityId = propertyOnlyResponse.nino,
          status = Status.NO_CONTENT,
          responseBody = emptyBodyString
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmedPageUrl)
        )

        verifyAuditEvent(
          OptOutAuditModel(
            saUtr = csbTestUser.saUtr,
            credId = csbTestUser.credId,
            userType = csbTestUser.userType,
            agentReferenceNumber = csbTestUser.arn,
            mtditid = csbTestUser.mtditid,
            nino = testNino,
            outcome = Outcome(isSuccessful = true, None, None),
            optOutRequestedFromTaxYear = taxYear.previousYear.formatTaxYearRange,
            currentYear = taxYear.formatTaxYearRange,
            beforeITSAStatusCurrentYearMinusOne = Voluntary,
            beforeITSAStatusCurrentYear = NoStatus,
            beforeITSAStatusCurrentYearPlusOne = NoStatus,
            afterAssumedITSAStatusCurrentYearMinusOne = Annual,
            afterAssumedITSAStatusCurrentYear = NoStatus,
            afterAssumedITSAStatusCurrentYearPlusOne = NoStatus,
            currentYearMinusOneCrystallised = false
          )
        )
      }
    }

    "user confirms opt-out for one-year scenario and missing header" should {

      "show opt-out complete page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear(dateService),
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
          Status.NO_CONTENT, emptyBodyString,
          Map("missing-header-name" -> "missing-header-value")
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(Status.SEE_OTHER),
          redirectURI(confirmedPageUrl)
        )

      }
    }

    "Error scenario" when {

      "user confirms opt-out for one-year scenario and update fails" should {

        "show Opt Out error page" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          helper.stubOptOutInitialState(
            currentTaxYear = currentTaxYear(dateService),
            previousYearCrystallised = false,
            previousYearStatus = Voluntary,
            currentYearStatus = NoStatus,
            nextYearStatus = NoStatus
          )

          ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
            BAD_REQUEST, Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString()
          )

          val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

          result should have(
            httpStatus(SEE_OTHER)
          )

        }
      }

      "user confirms opt-out for multi-year scenario and update fails" should {

        "show Opt Out error page" in {

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
              optOutRequestedFromTaxYear = taxYear.previousYear.formatTaxYearRange,
              currentYear = taxYear.formatTaxYearRange,
              beforeITSAStatusCurrentYearMinusOne = Voluntary,
              beforeITSAStatusCurrentYear = Voluntary,
              beforeITSAStatusCurrentYearPlusOne = Voluntary,
              afterAssumedITSAStatusCurrentYearMinusOne = Voluntary,
              afterAssumedITSAStatusCurrentYear = Voluntary,
              afterAssumedITSAStatusCurrentYearPlusOne = Annual,
              currentYearMinusOneCrystallised = false
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

          val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(optOutErrorPageUrl)
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

                .withRequestBody(matchingJsonPath("$.detail.beforeITSAStatusCurrentYearMinusOne", equalTo(optOutAuditModel.beforeITSAStatusCurrentYearMinusOne.toString)))
                .withRequestBody(matchingJsonPath("$.detail.beforeITSAStatusCurrentYear", equalTo(optOutAuditModel.beforeITSAStatusCurrentYear.toString)))
                .withRequestBody(matchingJsonPath("$.detail.beforeITSAStatusCurrentYearPlusOne", equalTo(optOutAuditModel.beforeITSAStatusCurrentYearPlusOne.toString)))
                .withRequestBody(matchingJsonPath("$.detail.afterAssumedITSAStatusCurrentYearMinusOne", equalTo(optOutAuditModel.afterAssumedITSAStatusCurrentYearMinusOne.toString)))
                .withRequestBody(matchingJsonPath("$.detail.afterAssumedITSAStatusCurrentYear", equalTo(optOutAuditModel.afterAssumedITSAStatusCurrentYear.toString)))
                .withRequestBody(matchingJsonPath("$.detail.afterAssumedITSAStatusCurrentYearPlusOne", equalTo(optOutAuditModel.afterAssumedITSAStatusCurrentYearPlusOne.toString)))
                .withRequestBody(matchingJsonPath("$.detail.currentYearMinusOneCrystallised", equalTo(optOutAuditModel.currentYearMinusOneCrystallised.toString)))
            )
          }

          verifyAuditEvent(auditModel)
        }
      }
    }
  }
}
