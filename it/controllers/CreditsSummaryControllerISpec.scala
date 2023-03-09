
package controllers

import audit.models.CreditSummaryAuditing.{CreditsSummaryModel, toCreditSummaryDetailsSeq}
import audit.models.IncomeSourceDetailsResponseAuditModel
import auth.MtdItUserWithNino
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

  val testUser = MtdItUserWithNino(
    mtditid = testMtditid,
    nino = testNino,
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
            propertyIncomeId = None,
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

    "display an empty credit summary page" when {
      "MFACreditsAndDebits and CutOverCredits feature switches are off" in {
        disable(MFACreditsAndDebits)
        disable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK,
          propertyOnlyResponseWithMigrationData(
            testTaxYear - 1,
            Some(testTaxYear.toString))
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid)

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
        enable(R7cTxmEvents)

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
            propertyIncomeId = None,
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
