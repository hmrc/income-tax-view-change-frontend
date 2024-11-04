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

import controllers.optOut.OptOutChooseTaxYearControllerISpec._
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, OptOutSessionRepositoryHelper}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status
import play.api.http.Status.OK
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class OptOutChooseTaxYearControllerISpec extends ComponentSpecBase {

  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  private val previousTaxYear = currentTaxYear.previousYear

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def testShowHappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

    s"calling GET $chooseOptOutTaxYearPageUrl" should {
      s"render page for show choose multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
        "User is authorised" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          helper.stubOptOutInitialState(currentTaxYear,
            previousYearCrystallised = false,
            previousYearStatus = Voluntary,
            currentYearStatus = Voluntary,
            nextYearStatus = Voluntary)

          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.renderChooseOptOutTaxYearPageInMultiYearJourney()
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            elementTextByID("heading")(headingText),
            elementTextByID("description1")(description1Text),
            elementTextBySelector("div.govuk-radios__item:nth-child(3) > label:nth-child(2)")(radioLabel3),
          )
        }
      }
    }
  }

  def testSubmitHappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

    s"calling GET $chooseOptOutTaxYearPageUrl" should {
      s"render page for submit choice for multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
        "User is authorised" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          helper.stubOptOutInitialState(currentTaxYear,
            previousYearCrystallised = false,
            previousYearStatus = Voluntary,
            currentYearStatus = Voluntary,
            nextYearStatus = Voluntary)

          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

          val formData: Map[String, Seq[String]] = Map(
            ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(previousTaxYear.toString),
            ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))
          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptOutChooseTaxYear(formData)
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(Status.SEE_OTHER),
            //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
            //todo add more asserts as part MISUV-7538
          )
        }
      }
    }
  }

  def testSubmitUnhappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

    s"calling GET $chooseOptOutTaxYearPageUrl" should {
      s"render page with error for submit choice for multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
        "User is authorised" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          helper.stubOptOutInitialState(currentTaxYear,
            previousYearCrystallised = false,
            previousYearStatus = Voluntary,
            currentYearStatus = Voluntary,
            nextYearStatus = Voluntary)

          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

          val formData: Map[String, Seq[String]] = Map(
            ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(),
            ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))
          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptOutChooseTaxYear(formData)
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(Status.BAD_REQUEST),
            //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
            //todo add more asserts as part MISUV-7538
          )
        }
      }
    }


    s"render page for submit choice for multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
      "User is authorised" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary)

        IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

        val formData: Map[String, Seq[String]] = Map(
          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(previousTaxYear.toString),
          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))
        val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptOutChooseTaxYear(formData)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(Status.SEE_OTHER),
          //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
          //todo add more asserts as part MISUV-7538
        )
      }
    }
  }

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "ABC123456789",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusFulfilled
        ))
    ),
    GroupedObligationsModel(
      identification = "ABC123456789",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#004",
          StatusFulfilled
        ))
    )
  ))

  "OptOutChooseTaxYearController - Individual" when {
    testShowHappyCase(isAgent = false)
    testSubmitHappyCase(isAgent = false)
    testSubmitUnhappyCase(isAgent = false)
  }

  "OptOutChooseTaxYearController - Agent" when {
    testShowHappyCase(isAgent = true)
    testSubmitHappyCase(isAgent = true)
    testSubmitUnhappyCase(isAgent = true)
  }

}

object OptOutChooseTaxYearControllerISpec {
  val headingText = "Opting out of quarterly reporting"
  val description1Text = "You can opt out from any of the tax years available and report annually from that year onwards. This means you’ll then report annually for all of your current businesses and any that you add in future."
  val radioLabel3 = "2023 to 2024 onwards"
}