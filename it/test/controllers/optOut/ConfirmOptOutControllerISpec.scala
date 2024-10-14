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
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import controllers.constants.ConfirmOptOutControllerConstants._
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub, OptOutSessionRepositoryHelper}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus._
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.mvc.Http.Status
import play.mvc.Http.Status.SEE_OTHER
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponse}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

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
          taxableEntityId = propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
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
            mtdItUser = MtdItUser(
              mtditid = testMtditid,
              nino = testNino,
              userName = None,
              incomeSources = multipleBusinessesAndPropertyResponse,
              btaNavPartial = None,
              saUtr = Some("1234567890"),
              credId = Some("12345-credId"),
              userType = Some(Individual),
              arn = None
            )(FakeRequest()),
            nino = testNino,
            outcome = Outcome(isSuccessful = true, None, None),
            optOutRequestedFromTaxYear = taxYear.previousYear.formatTaxYearRange,
            currentYear = taxYear.formatTaxYearRange,
            beforeITSAStatusCurrentYearMinusOne = Voluntary,
            beforeITSAStatusCurrentYear = NoStatus,
            beforeITSAStatusCurrentYearPlusOne = NoStatus,
            afterAssumedITSAStatusCurrentYearMinusOne = Annual,
            afterAssumedITSAStatusCurrentYear = NoStatus,
            afterAssumedITSAStatusCurrentYearPlusOne = Annual,
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

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
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

          ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
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
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          helper.stubOptOutInitialState(currentTaxYear(dateService),
            previousYearCrystallised = false,
            previousYearStatus = Voluntary,
            currentYearStatus = Voluntary,
            nextYearStatus = Voluntary)

          ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
            BAD_REQUEST, Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString(),
          )

          assert(optOutSessionDataRepository.saveIntent(TaxYear.getTaxYearModel("2023-2024").get).futureValue)


          val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

          result should have(
            httpStatus(SEE_OTHER)
          )
        }
      }
    }
  }
}
