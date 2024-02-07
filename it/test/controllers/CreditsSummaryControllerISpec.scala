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

package controllers

import audit.models.CreditSummaryAuditing.{CreditsSummaryModel, toCreditSummaryDetailsSeq}
import audit.models.IncomeSourceDetailsResponseAuditModel
import auth.{MtdItUserOptionNino, MtdItUserWithNino}
import config.featureswitch.{CutOverCredits, MFACreditsAndDebits}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import helpers.{ComponentSpecBase, CreditsSummaryDataHelper}
import play.api.http.Status.OK
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson, testValidFinancialDetailsModelCreditAndRefundsJsonV2}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class CreditsSummaryControllerISpec extends ComponentSpecBase with CreditsSummaryDataHelper {

  val calendarYear = "2018"
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  implicit val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]

  val testUser = MtdItUserOptionNino(
    mtditid = testMtditid,
    nino = Some(testNino),
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None
  )(FakeRequest())

  s"Navigating to /report-quarterly/income-and-expenses/view/credits-from-hmrc/$testTaxYear" should {
    "display the credit summary page" when {
      "a valid response is received" in {
        import audit.models.CreditSummaryAuditing._

        enable(MFACreditsAndDebits)
        enable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          testNino,
          s"${testTaxYear - 1}-04-06",
          s"$testTaxYear-04-05")(
          OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(
            -1400,
            -1400,
            testTaxYear.toString,
            LocalDate.now().plusYears(1).toString)
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid, 1)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        AuditStub.verifyAuditContainsDetail(
          IncomeSourceDetailsResponseAuditModel(
            mtdItUser = testUser,
            selfEmploymentIds = List.empty,
            propertyIncomeIds = Nil,
            yearOfMigration = None
          ).detail
        )

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messages("credits.heading", s"$calendarYear"))
        )

        AuditStub.verifyAuditContainsDetail(
          CreditsSummaryModel(
            saUTR = testSaUtr,
            nino = testNino,
            userType = testUserTypeIndividual.toString,
            credId = credId,
            mtdRef = testMtditid,
            creditOnAccount = "5",
            creditDetails = toCreditSummaryDetailsSeq(chargesList)(msgs)
          ).detail
        )
      }
    }

    "display the credit summary page" when {
      "MFACreditsAndDebits and CutOverCredits feature switches are off" in {
        disable(MFACreditsAndDebits)
        disable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK,
          propertyOnlyResponseWithMigrationData(
            testTaxYear - 1,
            Some(testTaxYear.toString))
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          testNino,
          s"${testTaxYear - 1}-04-06",
          s"$testTaxYear-04-05")(
          OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(
            -1400,
            -1400,
            testTaxYear.toString,
            LocalDate.now().plusYears(1).toString)
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messages("credits.heading", s"$calendarYear"))
        )

        AuditStub.verifyAuditDoesNotContainsDetail(
          CreditsSummaryModel(
            saUTR = testSaUtr,
            nino = testNino,
            userType = testUserTypeIndividual.toString,
            credId = credId,
            mtdRef = testMtditid,
            creditOnAccount = "5",
            creditDetails = toCreditSummaryDetailsSeq(chargesList)(msgs)
          ).detail
        )
      }
    }

    "correctly audit a list of credits" when {
      "the list contains Balancing Charge Credits" in {
        import audit.models.CreditSummaryAuditing._

        enable(MFACreditsAndDebits)
        enable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          testNino,
          s"${testTaxYear - 1}-04-06",
          s"$testTaxYear-04-05")(
          OK,
          testValidFinancialDetailsModelCreditAndRefundsJsonV2(
            -1400,
            -1400,
            testTaxYear.toString,
            LocalDate.now().plusYears(1).toString)
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid, 1)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        AuditStub.verifyAuditContainsDetail(
          IncomeSourceDetailsResponseAuditModel(
            mtdItUser = testUser,
            selfEmploymentIds = List.empty,
            propertyIncomeIds = Nil,
            yearOfMigration = None
          ).detail
        )

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messages("credits.heading", s"$calendarYear"))
        )

        AuditStub.verifyAuditContainsDetail(
          CreditsSummaryModel(
            saUTR = testSaUtr,
            nino = testNino,
            userType = testUserTypeIndividual.toString,
            credId = credId,
            mtdRef = testMtditid,
            creditOnAccount = "5",
            creditDetails = toCreditSummaryDetailsSeq(chargesListV2)(msgs)
          ).detail
        )
      }
    }
  }
}
