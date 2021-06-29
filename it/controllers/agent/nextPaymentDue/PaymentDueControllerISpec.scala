
package controllers.agent.nextPaymentDue

import assets.BaseIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.OutstandingChargesIntegrationTestConstants._
import auth.MtdItUser
import config.featureswitch.{AgentViewer, FeatureSwitching, TxmEventsApproved}
import controllers.Assets.INTERNAL_SERVER_ERROR
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name
import assets.PaymentDueTestConstraints.getCurrentTaxYearEnd
import audit.models.{WhatYouOweRequestAuditModel, WhatYouOweResponseAuditModel}

import java.time.LocalDate
import assets.FinancialDetailsIntegrationTestConstants.{whatYouOweDataFullDataWithoutOutstandingCharges, whatYouOweDataWithDataDueIn30Days, whatYouOweFinancialDetailsEmptyBCDCharge, whatYouOweNoChargeList, whatYouOweOutstandingChargesOnly, whatYouOweWithAZeroOutstandingAmount}

class PaymentDueControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
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

  val testArn: String = "1"

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
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

  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))),
    incomeSourceDetailsModel, Some("1234567890"), None, Some("Agent"), Some(testArn)
  )(FakeRequest())

  s"GET ${controllers.agent.nextPaymentDue.routes.PaymentDueController.show().url}" should {
    "SEE_OTHER to " when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

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

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$currentTaxYearEnd - 1-04-06", s"$currentTaxYearEnd-04-05")(OK,
          testValidFinancialDetailsModelJson(
            2000, 2000, currentTaxYearEnd.toString, LocalDate.now().toString))
        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        Then("The user is redirected to")
        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  s"return $NOT_FOUND" when {
    "the agent viewer feature switch is disabled" in {
      disable(AgentViewer)
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

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(
        OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      Then("A not found page is returned to the user")
      result should have(
        httpStatus(NOT_FOUND)
      )
    }
  }

  "YearOfMigration exists and with TxmEventsApproved FS enabled" when {
    "with a multiple charge from financial details and BCD and ACI charges from CESA" in {
      enable(AgentViewer)

      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK,
        testValidFinancialDetailsModelJson(
          2000, 2000, currentTaxYearEnd.toString, LocalDate.now().toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

      AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30Days).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
        isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
        isElementVisibleById("payment-details-content-0")(expectedValue = true),
        isElementVisibleById("payment-details-content-1")(expectedValue = true),
        isElementVisibleById("over-due-payments-heading")(expectedValue = false),
        isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-type-1")(expectedValue = true),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById("payment-days-note")(expectedValue = true),
        isElementVisibleById("credit-on-account")(expectedValue = true),
        isElementVisibleById("payment-button")(expectedValue = true),
        isElementVisibleById("sa-note-migrated")(expectedValue = true),
        isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true)
      )
    }

    "with a multiple charge, without BCD and ACI charges from CESA" in {
      enable(AgentViewer)
      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd.toInt, Some(currentTaxYearEnd.toString)))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05"
      )(OK, testValidFinancialDetailsModelJson(
        2000, 2000, currentTaxYearEnd.toString, LocalDate.now().minusDays(15).toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

      AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
        isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
        isElementVisibleById("payment-details-content-0")(expectedValue = true),
        isElementVisibleById("payment-details-content-1")(expectedValue = true),
        isElementVisibleById("over-due-payments-heading")(expectedValue = true),
        isElementVisibleById("over-due-type-0")(expectedValue = true),
        isElementVisibleById("over-due-type-1")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById(s"payment-days-note")(expectedValue = true),
        isElementVisibleById(s"credit-on-account")(expectedValue = true),
        isElementVisibleById(s"payment-button")(expectedValue = true),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
      )
    }

    "with multiple charges and one charge equals zero" in {
      enable(AgentViewer)
      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      val mixedJson = Json.obj(
        "documentDetails" -> Json.arr(
          documentDetailJson(3400.00, 1000.00, currentTaxYearEnd.toString),
          documentDetailJson(1000.00, 100.00, currentTaxYearEnd.toString, "ITSA- POA 1"),
          documentDetailJson(1000.00, 0, currentTaxYearEnd.toString, "ITSA - POA 2")
        ),
        "financialDetails" -> Json.arr(
          financialDetailJson(currentTaxYearEnd.toString),
          financialDetailJson(currentTaxYearEnd.toString, "SA Payment on Account 1", LocalDate.now().plusDays(1).toString),
          financialDetailJson(currentTaxYearEnd.toString, "SA Payment on Account 2", LocalDate.now().minusDays(1).toString)
        ))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        OK, mixedJson)

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
        OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

      AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
        isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
        isElementVisibleById("payment-details-content-0")(expectedValue = true),
        isElementVisibleById("payment-details-content-1")(expectedValue = true),
        isElementVisibleById("over-due-payments-heading")(expectedValue = false),
        isElementVisibleById("over-due-type-0")(expectedValue = false),
        isElementVisibleById("over-due-type-1")(expectedValue = false),
        isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById(s"payment-days-note")(expectedValue = true),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
      )
    }

    "redirect to an internal server error page when both connectors return internal server error" in {
      enable(AgentViewer)
      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
        s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(
        INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${currentTaxYearEnd.toInt - 1}-04-06", s"${currentTaxYearEnd.toInt}-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to an internal server error page when financial connector return internal server error" in {
      enable(AgentViewer)
      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
        s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(
        OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${currentTaxYearEnd.toInt - 1}-04-06", s"${currentTaxYearEnd.toInt}-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to an internal server error page when Outstanding charges connector return internal server error" in {
      enable(AgentViewer)
      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
        testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        OK, testValidFinancialDetailsModelJson(
          2000, 2000, currentTaxYearEnd.toString, LocalDate.now().toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
        INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${currentTaxYearEnd.toInt - 1}-04-06", s"${currentTaxYearEnd.toInt}-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )

    }
  }

  "YearOfMigration exists and with TxmEventsApproved FS disabled" when {
    "with a multiple charge from financial details and BCD and ACI charges from CESA" in {
      enable(AgentViewer)
      disable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK,
        testValidFinancialDetailsModelJson(
          2000, 2000, currentTaxYearEnd.toString, LocalDate.now().toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30Days).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
        isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
        isElementVisibleById("payment-details-content-0")(expectedValue = true),
        isElementVisibleById("payment-details-content-1")(expectedValue = true),
        isElementVisibleById("over-due-payments-heading")(expectedValue = false),
        isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-type-1")(expectedValue = true),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById("payment-days-note")(expectedValue = true),
        isElementVisibleById("credit-on-account")(expectedValue = true),
        isElementVisibleById("payment-button")(expectedValue = true),
        isElementVisibleById("sa-note-migrated")(expectedValue = true),
        isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true)
      )
    }

    "with a multiple charge, without BCD and ACI charges from CESA" in {
      enable(AgentViewer)
      disable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd.toInt, Some(currentTaxYearEnd.toString)))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05"
      )(OK, testValidFinancialDetailsModelJson(
        2000, 2000, currentTaxYearEnd.toString, LocalDate.now().minusDays(15).toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
        isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
        isElementVisibleById("payment-details-content-0")(expectedValue = true),
        isElementVisibleById("payment-details-content-1")(expectedValue = true),
        isElementVisibleById("over-due-payments-heading")(expectedValue = true),
        isElementVisibleById("over-due-type-0")(expectedValue = true),
        isElementVisibleById("over-due-type-1")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById(s"payment-days-note")(expectedValue = true),
        isElementVisibleById(s"credit-on-account")(expectedValue = true),
        isElementVisibleById(s"payment-button")(expectedValue = true),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
      )
    }

    "with multiple charges and one charge equals zero" in {
      enable(AgentViewer)
      disable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      val mixedJson = Json.obj(
        "documentDetails" -> Json.arr(
          documentDetailJson(3400.00, 1000.00, currentTaxYearEnd.toString),
          documentDetailJson(1000.00, 100.00, currentTaxYearEnd.toString, "ITSA- POA 1"),
          documentDetailJson(1000.00, 0, currentTaxYearEnd.toString, "ITSA - POA 2")
        ),
        "financialDetails" -> Json.arr(
          financialDetailJson(currentTaxYearEnd.toString),
          financialDetailJson(currentTaxYearEnd.toString, "SA Payment on Account 1", LocalDate.now().plusDays(1).toString),
          financialDetailJson(currentTaxYearEnd.toString, "SA Payment on Account 2", LocalDate.now().minusDays(1).toString)
        ))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        OK, mixedJson)

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
        OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
        isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
        isElementVisibleById("payment-details-content-0")(expectedValue = true),
        isElementVisibleById("payment-details-content-1")(expectedValue = true),
        isElementVisibleById("over-due-payments-heading")(expectedValue = false),
        isElementVisibleById("over-due-type-0")(expectedValue = false),
        isElementVisibleById("over-due-type-1")(expectedValue = false),
        isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = true),
        isElementVisibleById("due-in-thirty-days-type-0")(expectedValue = true),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById(s"payment-days-note")(expectedValue = true),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
      )
    }
  }

  s"return $OK with TxmEventsApproved FS enabled" when {
    "YearOfMigration does not exists" when {
      "with a no charge" in {
        enable(AgentViewer)
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd, None))


        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06",
          s"$currentTaxYearEnd-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
          isElementVisibleById("payment-details-content-0")(expectedValue = false),
          isElementVisibleById("payment-details-content-1")(expectedValue = false),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById("payment-days-note")(expectedValue = true),
          isElementVisibleById("credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        )
      }
    }

    "YearOfMigration exists but not the first year" when {
      "with a no charge" in {
        val testTaxYear = LocalDate.now().getYear - 3
        enable(AgentViewer)
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
          s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, testTaxYear.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
          isElementVisibleById("payment-details-content-0")(expectedValue = false),
          isElementVisibleById("payment-details-content-1")(expectedValue = false),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById("payment-days-note")(expectedValue = true),
          isElementVisibleById("credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        )
      }
    }

    "YearOfMigration exists and No valid charges exists" when {
      "with a no charge" in {
        enable(AgentViewer)
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "documentDetails" -> Json.arr(
            documentDetailJson(3400.00, 1000.00, currentTaxYearEnd.toString, "test"),
            documentDetailJson(1000.00, 0.00, currentTaxYearEnd.toString, "4444"),
            documentDetailJson(1000.00, 3000.00, currentTaxYearEnd.toString, "5555")
          ),
          "financialDetails" -> Json.arr(
            financialDetailJson(currentTaxYearEnd.toString, "test"),
            financialDetailJson(currentTaxYearEnd.toString, "4444"),
            financialDetailJson(currentTaxYearEnd.toString, "5555")
          )
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
          testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
          isElementVisibleById("payment-details-content-0")(expectedValue = false),
          isElementVisibleById("payment-details-content-1")(expectedValue = false),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById("payment-days-note")(expectedValue = true),
          isElementVisibleById("credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        )
      }
    }

    "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" when {
      "only BCD charge" in {
        enable(AgentViewer)
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "documentDetails" -> Json.arr(
            documentDetailJson(3400.00, 1000.00, currentTaxYearEnd.toString, "test"),
            documentDetailJson(1000.00, 0.00, currentTaxYearEnd.toString, "3333"),
            documentDetailJson(1000.00, 3000.00, currentTaxYearEnd.toString, "4444")
          ),
          "financialDetails" -> Json.arr(
            financialDetailJson(currentTaxYearEnd.toString, "test"),
            financialDetailJson(currentTaxYearEnd.toString, "3333"),
            financialDetailJson(currentTaxYearEnd.toString, "4444")
          ))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly).detail)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
          isElementVisibleById("payment-details-content-0")(expectedValue = true),
          isElementVisibleById("payment-details-content-1")(expectedValue = true),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById(s"payment-days-note")(expectedValue = true),
          isElementVisibleById(s"credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = true),
          isElementVisibleById(s"no-payments-due")(expectedValue = false),
          isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
        )
      }
    }

    "YearOfMigration exists with valid financial details charges and invalid outstanding charges" when {
      "only BCD charge" in {
        enable(AgentViewer)
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        val testTaxYear = LocalDate.now().getYear

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05"
        )(OK, testValidFinancialDetailsModelJson(
          2000, 2000, testTaxYear.toString, LocalDate.now().plusYears( 1).toString))

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(testUser).detail)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge).detail)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
          isElementVisibleById("payment-details-content-0")(expectedValue = true),
          isElementVisibleById("payment-details-content-1")(expectedValue = true),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = true),
          isElementVisibleById("future-payments-type-0")(expectedValue = true),
          isElementVisibleById("future-payments-type-1")(expectedValue = true),
          isElementVisibleById(s"payment-days-note")(expectedValue = true),
          isElementVisibleById(s"credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = true),
          isElementVisibleById(s"no-payments-due")(expectedValue = false),
          isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
        )
      }
    }

  }

  s"return $OK with TxmEventsApproved FS disabled" when {
    disable(TxmEventsApproved)
    "YearOfMigration does not exists" when {
      "with a no charge" in {
        enable(AgentViewer)
        disable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd, None))


        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06",
          s"$currentTaxYearEnd-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
          isElementVisibleById("payment-details-content-0")(expectedValue = false),
          isElementVisibleById("payment-details-content-1")(expectedValue = false),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById("payment-days-note")(expectedValue = true),
          isElementVisibleById("credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        )
      }
    }

    "YearOfMigration exists but not the first year" when {
      "with a no charge" in {
        val testTaxYear = LocalDate.now().getYear - 3
        enable(AgentViewer)
        disable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
          s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, testTaxYear.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
          isElementVisibleById("payment-details-content-0")(expectedValue = false),
          isElementVisibleById("payment-details-content-1")(expectedValue = false),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById("payment-days-note")(expectedValue = true),
          isElementVisibleById("credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        )
      }
    }

    "YearOfMigration exists and No valid charges exists" when {
      "with a no charge" in {
        enable(AgentViewer)
        disable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "documentDetails" -> Json.arr(
            documentDetailJson(3400.00, 1000.00, currentTaxYearEnd.toString, "test"),
            documentDetailJson(1000.00, 0.00, currentTaxYearEnd.toString, "4444"),
            documentDetailJson(1000.00, 3000.00, currentTaxYearEnd.toString, "5555")
          ),
          "financialDetails" -> Json.arr(
            financialDetailJson(currentTaxYearEnd.toString, "test"),
            financialDetailJson(currentTaxYearEnd.toString, "4444"),
            financialDetailJson(currentTaxYearEnd.toString, "5555")
          )
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
          testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = false),
          isElementVisibleById("payment-details-content-0")(expectedValue = false),
          isElementVisibleById("payment-details-content-1")(expectedValue = false),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById("payment-days-note")(expectedValue = true),
          isElementVisibleById("credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        )
      }
    }

    "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" when {
      "only BCD charge" in {
        enable(AgentViewer)
        disable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "documentDetails" -> Json.arr(
            documentDetailJson(3400.00, 1000.00, currentTaxYearEnd.toString, "test"),
            documentDetailJson(1000.00, 0.00, currentTaxYearEnd.toString, "3333"),
            documentDetailJson(1000.00, 3000.00, currentTaxYearEnd.toString, "4444")
          ),
          "financialDetails" -> Json.arr(
            financialDetailJson(currentTaxYearEnd.toString, "test"),
            financialDetailJson(currentTaxYearEnd.toString, "3333"),
            financialDetailJson(currentTaxYearEnd.toString, "4444")
          ))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly).detail)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
          isElementVisibleById("payment-details-content-0")(expectedValue = true),
          isElementVisibleById("payment-details-content-1")(expectedValue = true),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById(s"payment-days-note")(expectedValue = true),
          isElementVisibleById(s"credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = true),
          isElementVisibleById(s"no-payments-due")(expectedValue = false),
          isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
        )
      }
    }

    "YearOfMigration exists with valid financial details charges and invalid outstanding charges" when {
      "only BCD charge" in {
        enable(AgentViewer)
        disable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        val testTaxYear = LocalDate.now().getYear

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05"
        )(OK, testValidFinancialDetailsModelJson(
          2000, 2000, testTaxYear.toString, LocalDate.now().plusYears( 1).toString))

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge).detail)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-table-head")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("payment-type-dropdown-title")(expectedValue = true),
          isElementVisibleById("payment-details-content-0")(expectedValue = true),
          isElementVisibleById("payment-details-content-1")(expectedValue = true),
          isElementVisibleById("over-due-payments-heading")(expectedValue = false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = true),
          isElementVisibleById("future-payments-type-0")(expectedValue = true),
          isElementVisibleById("future-payments-type-1")(expectedValue = true),
          isElementVisibleById(s"payment-days-note")(expectedValue = true),
          isElementVisibleById(s"credit-on-account")(expectedValue = true),
          isElementVisibleById(s"payment-button")(expectedValue = true),
          isElementVisibleById(s"no-payments-due")(expectedValue = false),
          isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true)
        )
      }
    }

  }
}
