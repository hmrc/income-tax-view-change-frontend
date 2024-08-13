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

package controllers.optin


import controllers.optin.ChooseYearControllerISpec.{description1Text, headingText, taxYearChoiceOne, taxYearChoiceTwo}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.TaxYear
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel, StatusFulfilled}
import play.api.http.Status.OK
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

object ChooseYearControllerISpec {
  val headingText = "Voluntarily opting in to reporting quarterly"
  val description1Text = "If you opt in to the next tax year, you will not have to submit a quarterly update until then."
  val taxYearChoiceOne = "2022 to 2023 onwards"
  val taxYearChoiceTwo = "2023 to 2024 onwards"
}

class ChooseYearControllerISpec extends ComponentSpecBase {

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val nextTaxYear = currentTaxYear.nextYear
  val previousTaxYear = currentTaxYear.previousYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe(true)
  }

  def testShowHappyCase(isAgent: Boolean): Unit = {

    val chooseOptInTaxYearPageUrl = routes.ChooseYearController.show(isAgent).url

    s"calling GET $chooseOptInTaxYearPageUrl" should {
      s"render page for show choose multi-year opt-in tax-year $chooseOptInTaxYearPageUrl" when {
        "User is authorised" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          //todo mock optInService.availableOptInTaxYear
//          stubOptOutInitialState(previousYearCrystallised = false,
//            previousYearStatus = Voluntary,
//            currentYearStatus = Voluntary,
//            nextYearStatus = Voluntary)
//          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.renderChooseOptInTaxYearPageInMultiYearJourney()
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            elementTextByID("heading")(headingText),
            elementTextByID("description1")(description1Text),
            elementTextBySelector("#whichTaxYear.govuk-fieldset legend.govuk-fieldset__legend.govuk-fieldset__legend--m")("Which tax year do you want to opt in from?"),
            elementTextBySelector("div.govuk-radios__item:nth-child(1) > label:nth-child(2)")(taxYearChoiceOne),
            elementTextBySelector("div.govuk-radios__item:nth-child(2) > label:nth-child(2)")(taxYearChoiceTwo),
          )
        }
      }
    }
  }

//  def testSubmitHappyCase(isAgent: Boolean): Unit = {
//
//    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url
//
//    s"calling GET $chooseOptOutTaxYearPageUrl" should {
//      s"render page for submit choice for multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
//        "User is authorised" in {
//
//          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
//
//          stubOptOutInitialState(previousYearCrystallised = false,
//            previousYearStatus = Voluntary,
//            currentYearStatus = Voluntary,
//            nextYearStatus = Voluntary)
//
//          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)
//
//          val formData: Map[String, Seq[String]] = Map(
//            ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(previousTaxYear.toString),
//            ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))
//          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptOutChooseTaxYear(formData)
//          verifyIncomeSourceDetailsCall(testMtditid)
//
//          result should have(
//            httpStatus(Status.SEE_OTHER),
//            //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
//            //todo add more asserts as part MISUV-7538
//          )
//        }
//      }
//    }
//  }
//
//  def testSubmitUnhappyCase(isAgent: Boolean): Unit = {
//
//    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url
//
//    s"calling GET $chooseOptOutTaxYearPageUrl" should {
//      s"render page with error for submit choice for multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
//        "User is authorised" in {
//
//          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
//
//          stubOptOutInitialState(previousYearCrystallised = false,
//            previousYearStatus = Voluntary,
//            currentYearStatus = Voluntary,
//            nextYearStatus = Voluntary)
//
//          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)
//
//          val formData: Map[String, Seq[String]] = Map(
//            ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(),
//            ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))
//          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptOutChooseTaxYear(formData)
//          verifyIncomeSourceDetailsCall(testMtditid)
//
//          result should have(
//            httpStatus(Status.BAD_REQUEST),
//            //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
//            //todo add more asserts as part MISUV-7538
//          )
//        }
//      }
//    }
//
//
//    s"render page for submit choice for multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
//      "User is authorised" in {
//
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
//
//        stubOptOutInitialState(previousYearCrystallised = false,
//          previousYearStatus = Voluntary,
//          currentYearStatus = Voluntary,
//          nextYearStatus = Voluntary)
//
//        IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)
//
//        val formData: Map[String, Seq[String]] = Map(
//          ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(previousTaxYear.toString),
//          ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))
//        val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptOutChooseTaxYear(formData)
//        verifyIncomeSourceDetailsCall(testMtditid)
//
//        result should have(
//          httpStatus(Status.SEE_OTHER),
//          //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
//          //todo add more asserts as part MISUV-7538
//        )
//      }
//    }
//  }

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusFulfilled
        ))
    ),
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
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

  "ChooseYearController - Individual" when {
    testShowHappyCase(isAgent = false)
//    testSubmitHappyCase(isAgent = false)
//    testSubmitUnhappyCase(isAgent = false)
  }

  "ChooseYearController - Agent" when {
    testShowHappyCase(isAgent = true)
//    testSubmitHappyCase(isAgent = true)
//    testSubmitUnhappyCase(isAgent = true)
  }

//  private def stubOptOutInitialState(previousYearCrystallised: Boolean,
//                                     previousYearStatus: ITSAStatus.Value,
//                                     currentYearStatus: ITSAStatus.Value,
//                                     nextYearStatus: ITSAStatus.Value): Unit = {
//    repository.set(
//      UIJourneySessionData(testSessionId,
//        OptOutJourney.Name,
//        optOutSessionData =
//          Some(OptOutSessionData(
//            Some(OptOutContextData(
//              previousYearCrystallised,
//              statusToString(previousYearStatus),
//              statusToString(currentYearStatus),
//              statusToString(nextYearStatus))), None))))
//  }

}