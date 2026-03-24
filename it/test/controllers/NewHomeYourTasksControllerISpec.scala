package controllers

import enums.ChargeType.ITSA_NI
import enums.MTDIndividual
import helpers.WiremockHelper
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{CreditsRefundsRepay, NewHomePage, PenaltiesAndAppeals}
import models.core.{AccountingPeriodModel, CessationModel}
import models.creditsandrefunds.CreditsModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseIntegrationTestConstants.{testIncomeSource, testMtditid, testNino}
import testConstants.BusinessDetailsIntegrationTestConstants.{address, b2CessationDate, b2TradingStart}
import testConstants.IncomeSourceIntegrationTestConstants.{documentText, financialDetailJson, noDunningLock, noInterestLock, testValidFinancialDetailsModelJson, testValidFinancialDetailsModelJsonSingleCharge, twoDunningLocks, twoInterestLocks}
import testConstants.NextUpdatesIntegrationTestConstants.currentDate

class NewHomeYourTasksControllerISpec extends ControllerISpecHelper {

  val path = "/your-tasks"

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(currentDate, currentDate.plusYears(1))),
      None,
      Some(getCurrentTaxYearEnd),
      Some(b2TradingStart),
      None,
      Some(CessationModel(Some(b2CessationDate))),
      address = Some(address)
    )),
    properties = Nil
  )

  val genericCharge = "4910"
  val lateSubmissionPenalty = "4027"
  val firstLatePaymentPenalty = "4028"

  object YourTasksViewMessages {
    val noTasksHeading = "No tasks"
    val noTasksContent = "You have no tasks to complete at the moment."

    val overdueChargeHeading = "Check what you owe and make a payment"
    val overdueChargeContent = "You have an overdue amount of £100"
    val overdueChargeTag = "Was due 29 Mar 2018"

    val overdueLspHeading = "View your late submission penalty"
    val overdueLspContent = "You have a late submission penalty."
    val overdueLspTag = "Was due 29 Mar 2018"

    val overdueLppHeading = "View your late payment penalty"
    val overdueLppContent = "You have a late payment penalty."
    val overdueLppTag = "Was due 29 Mar 2018"

    val upcomingChargeHeading = "Check what you owe and make a payment"
    val upcomingChargeContent = "You have an upcoming payment."
    val upcomingChargeTag = "Due 29 Mar 2100"
  }

  "GET /your-tasks" when {
    "an authenticateduser" should {
      "render the your tasks page" which {
        "displays the no tasks card" when {
          "the user has no current tasks" in new TestSetup() {
            enable(NewHomePage, CreditsRefundsRepay)
            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitle(MTDIndividual, "home.heading.new"),
              elementTextByID("noTaskCard-heading")(YourTasksViewMessages.noTasksHeading),
              elementTextByID("noTaskCard-content")(YourTasksViewMessages.noTasksContent)
            )
          }

          "the user has tasks relating to penalties and financials but they are a supporting agent" in {

          }
        }

        "display overdue task cards" when {
          "the user has an overdue annual submission" in {

          }
          "the user has an overdue quarterly submission" in {

          }
          "the user has multiple overdue annual submissions" in {

          }
          "the user has multiple overdue quarterly submissions" in {

          }
          "the user has an overdue charge" in new TestSetup(chargesJson = overdueChargeJson) {
            enable(NewHomePage, CreditsRefundsRepay)
            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitle(MTDIndividual, "home.heading.new"),
              elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueChargeHeading),
              elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueChargeContent),
              elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueChargeTag),
              elementCountBySelector(".govuk-tag--red")(1)
            )
          }
          "the user has multiple overdue charges" in {

          }
          "the user has an overdue late submission penalty" in new TestSetup(chargesJson = overdueLspJson) {
            enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitle(MTDIndividual, "home.heading.new"),
              elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueLspHeading),
              elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueLspContent),
              elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueLspTag),
              elementCountBySelector(".govuk-tag--red")(1)
            )
          }
          "the user has multiple overdue late submission penalties" in {

          }
          "the user has an overdue late payment penalty" in new TestSetup(chargesJson = overdueLppJson) {
            enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitle(MTDIndividual, "home.heading.new"),
              elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueLppHeading),
              elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueLppContent),
              elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueLppTag),
              elementCountBySelector(".govuk-tag--red")(1)
            )
          }

          "the user has multiple overdue late payment penalties" in {

          }
          "the user has overdue charges & overdue submissions" in {

          }
        }

        "not display the overdue quarterly submissions cards" when {
          "user's ITSA status is Annual" in {

          }
          "user's ITSA status is Exempt" in {

          }
          "user's ITSA status is Digitally Exempt" in {

          }
        }

        "display dateless submission cards" when {
          "the user has money in their account" in {

          }
        }
        "not display the money in your account task" when {
          "the user has no money in their account" in {

          }
          "creditsRefundsRepayEnabled FS is disabled" in {

          }
        }

        "shows the due dates in the correct tag colour" when {
          "task is overdue" in {

          }
          "upcoming task is due today" in {

          }
          "upcoming task is due within 30 days" in {

          }
          "upcoming task is due in over 30 days" in {

          }
        }

        "show upcoming task cards" when {
          "user has an upcoming annual submission" in {

          }
          "user has an upcoming quarterly submission" in {

          }
          "user has an upcoming charge" in new TestSetup(chargesJson = upcomingChargeJson) {
            enable(NewHomePage, CreditsRefundsRepay)
            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitle(MTDIndividual, "home.heading.new"),
              elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingChargeHeading),
              elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingChargeContent),
              elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingChargeTag),
              elementCountBySelector(".govuk-tag--green")(1)
            )
          }
          "user has an upcoming late submission penalty" in {

          }
          "user has an upcoming late payment penalty" in {

          }
          "user has multiple upcoming submissions and upcoming charges" in {

          }
        }

        "not display the upcoming quarterly submissions cards" when {
          "user's ITSA status is Annual" in {

          }
          "user's ITSA status is Exempt" in {

          }
          "user's ITSA status is Digitally Exempt" in {

          }
        }

        "display multiple cards" when {
          "the user has multiple submissions, charges and money in their account" in {

          }
        }
      }
    }
  }

  class TestSetup(currentItsaStatus: ITSAStatus = ITSAStatus.Annual,
                  creditAmount: BigDecimal = 0,
                  obligationsModel: ObligationsModel = noObligationsModel,
                  chargesJson: JsValue = noChargesModel()) {

    private val poa1Description: String = "ITSA- POA 1"
    private val poa2Description: String = "ITSA- POA 2"

    val testCreditModel = CreditsModel(0.0, 0.0, 0.0, creditAmount, None, None, Nil)
    val response: String = Json.toJson(testCreditModel).toString()

    val url = s"/income-tax-view-change/$testNino/financial-details/credits/from/2022-04-06/to/2023-04-05"

    MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(status = OK, response = incomeSourceDetailsModel)
    WiremockHelper.stubGet(url, OK, response)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, "2022-04-06", "2023-04-05")(OK, chargesJson)
    ITSAStatusDetailsStub.stubGetITSAStatusDetails(currentItsaStatus.toString, "2022-23")
    IncomeTaxViewChangeStub.stubGetNextUpdates(nino = testNino, deadlines = obligationsModel)
  }

  private val overdueChargeJson = baseChargesModel(genericCharge, "2018-03-29")
  private val overdueLspJson = baseChargesModel(lateSubmissionPenalty, "2018-03-29")
  private val overdueLppJson = baseChargesModel(firstLatePaymentPenalty, "2018-03-29")
  private val upcomingChargeJson = baseChargesModel(genericCharge, "2100-03-29")

  private val noObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", Some(currentDate), "testPeriodKey", StatusFulfilled)
      ))
  ))

  private def noChargesModel(): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(),
    "financialDetails" -> Json.arr()
  )

  private def baseChargesModel(mainTransaction: String, date: String) = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> 2018,
        "transactionId" -> "1040000123",
        "documentDescription" -> "TRM New Charge",
        "documentText" -> "TRM New Charge",
        "outstandingAmount" -> 100,
        "originalAmount" -> 100,
        "documentDate" -> "2018-03-29",
        "interestFromDate" -> "2018-03-29",
        "interestEndDate" -> "2018-03-29",
        "interestRate" -> "3",
        "accruingInterestAmount" -> 100,
        "interestOutstandingAmount" -> 80.0,
        "effectiveDateOfPayment" -> "2018-03-29",
        "documentDueDate" -> date,
        "poaRelevantAmount" -> 0
      )
    ),
    "financialDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2018",
        "mainType" -> "SA Balancing Charge",
        "mainTransaction" -> mainTransaction,
        "transactionId" -> "1040000123",
        "chargeType" -> ITSA_NI,
        "chargeReference" -> "ABCD1234",
        "originalAmount" -> 100,
        "items" -> Json.arr(
          Json.obj("amount" -> 10000,
            "clearingDate" -> "2019-08-13",
            "dueDate" -> date))
      )
    )
  )

}
