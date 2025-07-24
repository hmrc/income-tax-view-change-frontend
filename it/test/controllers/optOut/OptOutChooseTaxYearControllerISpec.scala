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

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import helpers.OptOutSessionRepositoryHelper
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{NavBarFs, OptOutFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.OK
import play.mvc.Http.Status
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class OptOutChooseTaxYearControllerISpec extends ControllerISpecHelper {

  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  private val previousTaxYear = currentTaxYear.previousYear
  private val nextTaxYear = currentTaxYear.nextYear

  val headingText = "Opting out of quarterly reporting"
  val descriptionText = "You can opt out from any of the tax years available and report annually from that year onwards. This means you’ll then report annually for all of your current businesses and any that you add in future."
  val radioLabel3 = "2023 to 2024 onwards"


  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/optout/choose-taxyear"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the choose tax year page" in {
            enable(OptOutFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear,
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = Voluntary,
              nextYearStatus = Voluntary)

            IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(headingText),
              elementTextByID("description")(descriptionText),
              elementTextBySelector("div.govuk-radios__item:nth-child(3) > label:nth-child(2)")(radioLabel3),
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to check your answers page" when {
            "previous tax year selected" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
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

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(Status.SEE_OTHER),
                //todo add more asserts in MISUV-8006
              )
            }

            "current tax year selected" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              helper.stubOptOutInitialState(currentTaxYear,
                previousYearCrystallised = false,
                previousYearStatus = Voluntary,
                currentYearStatus = Voluntary,
                nextYearStatus = Voluntary)

              IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

              val formData: Map[String, Seq[String]] = Map(
                ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(currentTaxYear.toString),
                ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(Status.SEE_OTHER),
                //todo add more asserts in MISUV-8006
              )
            }

              "next tax year selected" in {
                enable(OptOutFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

                helper.stubOptOutInitialState(currentTaxYear,
                  previousYearCrystallised = false,
                  previousYearStatus = Voluntary,
                  currentYearStatus = Voluntary,
                  nextYearStatus = Voluntary)

                IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

                val formData: Map[String, Seq[String]] = Map(
                  ConfirmOptOutMultiTaxYearChoiceForm.choiceField -> Seq(nextTaxYear.toString),
                  ConfirmOptOutMultiTaxYearChoiceForm.csrfToken -> Seq(""))

                val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(Status.SEE_OTHER),
                  //todo add more asserts in MISUV-8006
                )
              }
            }

          "return a BadRequest" when {
            "the form is invalid" in {
              enable(OptOutFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
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
              val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(Status.BAD_REQUEST),
                //elementTextByID("heading")("Confirm and opt out for the 2021 to 2022 tax year"),
                //todo add more asserts as part MISUV-7538
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole, Some(Map.empty))
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
}
