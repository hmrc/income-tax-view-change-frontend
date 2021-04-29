/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.agent

import java.time.{LocalDate, LocalDateTime}

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.PaymentHistoryTestConstraints.getCurrentTaxYearEnd
import assets.messages.{DeductionsSummaryMessages => messages}
import config.featureswitch.{AgentViewer, DeductionBreakdown, FeatureSwitching}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._

class DeductionsSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(DeductionBreakdown)
    enable(AgentViewer)
  }

  val clientDetails: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  "Calling the DeductionsSummaryController.showDeductionsSummary(taxYear)" should {
      "return the correct deductions summary page" in {
        And("I wiremock stub a successful Deductions Source Details response with single Business and Property income")
        enable(AgentViewer)
        enable(DeductionBreakdown)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = IncomeSourceDetailsModel(
            mtdbsa = testMtditid,
            yearOfMigration = None,
            businesses = List(BusinessDetailsModel(
              "testId",
              AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
              None, None, None, None, None, None, None, None,
              Some(getCurrentTaxYearEnd)
            )),
            property = None
          )
        )

        And("I stub a successful calculation response for 2017-18")
        IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/$testYear/income")
        val res = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear, clientDetails)


        res should have(
          httpStatus(OK),
          pageTitle(messages.agentsDeductionsSummaryTitle),
        )
      }
    "return the correct deductions summary page when DeductionBreakdown feature switch is disabled" in {

      And("I wiremock stub a successful Deductions Source Details response with single Business and Property income")
      enable(AgentViewer)
      disable(DeductionBreakdown)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = IncomeSourceDetailsModel(
          mtdbsa = testMtditid,
          yearOfMigration = None,
          businesses = List(BusinessDetailsModel(
            "testId",
            AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
            None, None, None, None, None, None, None, None,
            Some(getCurrentTaxYearEnd)
          )),
          property = None
        )
      )

      And("I stub a successful calculation response for 2017-18")
      IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
        status = OK,
        body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
      )
      IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
        status = OK,
        body = estimatedCalculationFullJson
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/$testYear/deductions")
      val res = IncomeTaxViewChangeFrontend.getDeductionsSummary(testYear, clientDetails)


      res should have(
        httpStatus(SEE_OTHER),
        redirectURI("/report-quarterly/income-and-expenses/view/agents/calculation/2018"))
    }
  }
}
