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

import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub, OptOutSessionRepositoryHelper}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus._
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.{BAD_REQUEST, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmOptOutControllerISpec extends ComponentSpecBase {
  private val isAgent: Boolean = false
  private val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url
  private val submitConfirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.submit(isAgent).url

  private val confirmedPageUrl = controllers.optOut.routes.ConfirmedOptOutController.show(isAgent).url

  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  private val previousYear = currentTaxYear.addYears(-1)

  private val expectedTitle = s"Confirm and opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year"
  private val summary = "If you opt out, you can submit your tax return through your HMRC online account or software."
  private val infoMessage = s"In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."
  private val emptyBodyString = ""

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)
  private val optOutExpectedTitle = s"Check your answers"

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  s"calling GET $confirmOptOutPageUrl" should {

    s"render confirm single year opt out page $confirmOptOutPageUrl" when {
      "User is authorised" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = NoStatus)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmOptOut()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(expectedTitle),
          elementTextByID("summary")(summary),
          elementTextByID("info-message")(infoMessage),
        )
      }
    }

    s"render confirm multi-year opt out page $confirmOptOutPageUrl" when {
      "User is authorised" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
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

  s"calling POST $submitConfirmOptOutPageUrl" when {
    s"user confirms opt-out for one-year scenario" should {
      "show opt-out complete page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmedPageUrl)
        )

      }
    }

    s"user confirms opt-out for one-year scenario and missing header" should {
      "show opt-out complete page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
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

    s"user confirms opt-out for one-year scenario and update fails" should {
      "show Opt Out error page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          BAD_REQUEST, Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString()
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(SEE_OTHER)
        )

      }
    }

    s"user confirms opt-out for multi-year scenario and update fails" should {
      "show Opt Out error page" in {
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
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
