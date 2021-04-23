
package controllers.agent.nextPaymentDue

import java.time.LocalDate

import assets.BaseIntegrationTestConstants._
import assets.ChargeListIntegrationTestConstants.whatYouOweFinancialDataWithoutOutstandingCharges
import assets.IncomeSourceIntegrationTestConstants._
import assets.OutstandingChargesIntegrationTestConstants._
import assets.PaymentDueTestConstraints.getCurrentTaxYearEnd
import audit.models.{WhatYouOweRequestAuditModel, WhatYouOweResponseAuditModel}
import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi, Payment}
import controllers.Assets.INTERNAL_SERVER_ERROR
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.core.AccountingPeriodModel
import models.financialDetails.WhatYouOweChargesList
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json

class PaymentDueControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(Payment)
    enable(NewFinancialDetailsApi)
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

  val currentTaxYearEnd = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd = currentTaxYearEnd - 2

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

  "YearOfMigration exists" when {
    "with a multiple charge from financial details and BCD and ACI charges from CESA" in {
      enable(AgentViewer)
      enable(NewFinancialDetailsApi)
      enable(Payment)
      stubAuthorisedAgentUser(authorised = true)

      WhatYouOweChargesList()

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

      AuditStub.verifyAuditContainsDetail(WhatYouOweRequestAuditModel(Some("1"), Some("Agent"), Some("1234567890"), "AA123456A", None, "XAITSA123456").detail)

      // TODO: use charges aligned with stubbed data
//      AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(Some("1"), Some("Agent"), Some("1234567890"), "AA123456A", None, "XAITSA123456",
//        whatYouOweFinancialDataWithoutOutstandingCharges).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(true),
        isElementVisibleById("balancing-charge-type-table-head")(true),
        isElementVisibleById("balancing-charge-type-0")(true),
        isElementVisibleById("balancing-charge-type-1")(true),
        isElementVisibleById("payment-type-dropdown-title")(true),
        isElementVisibleById("payment-details-content-0")(true),
        isElementVisibleById("payment-details-content-1")(true),
        isElementVisibleById("over-due-payments-heading")(false),
        isElementVisibleById("due-in-thirty-days-payments-heading")(true),
        isElementVisibleById("due-in-thirty-days-type-0")(true),
        isElementVisibleById("due-in-thirty-days-type-1")(true),
        isElementVisibleById("future-payments-heading")(false),
        isElementVisibleById("payment-days-note")(true),
        isElementVisibleById("credit-on-account")(true),
        isElementVisibleById("payment-button")(true),
        isElementVisibleById("sa-note-migrated")(true),
        isElementVisibleById("outstanding-charges-note-migrated")(true)
      )
    }

    "with a multiple charge, without BCD and ACI charges from CESA and payment disabled" in {
      enable(AgentViewer)
      enable(NewFinancialDetailsApi)
      disable(Payment)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd.toInt, Some(currentTaxYearEnd.toString)))


      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05"
      )(OK, testValidFinancialDetailsModelJson(
        2000, 2000, currentTaxYearEnd.toString, LocalDate.now().minusDays(1).toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(false),
        isElementVisibleById("balancing-charge-type-table-head")(false),
        isElementVisibleById("balancing-charge-type-0")(false),
        isElementVisibleById("balancing-charge-type-1")(false),
        isElementVisibleById("payment-type-dropdown-title")(true),
        isElementVisibleById("payment-details-content-0")(true),
        isElementVisibleById("payment-details-content-1")(true),
        isElementVisibleById("over-due-payments-heading")(true),
        isElementVisibleById("over-due-type-0")(true),
        isElementVisibleById("over-due-type-1")(true),
        isElementVisibleById("due-in-thirty-days-payments-heading")(false),
        isElementVisibleById("future-payments-heading")(false),
        isElementVisibleById(s"payment-days-note")(false),
        isElementVisibleById(s"credit-on-account")(false),
        isElementVisibleById(s"payment-button")(false),
        isElementVisibleById(s"sa-note-migrated")(true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(true)
      )
    }

    "with multiple charges and one charge equals zero" in {
      enable(Payment)
      enable(NewFinancialDetailsApi)
      enable(AgentViewer)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      val mixedJson = Json.obj(
        "financialDetails" -> Json.arr(
          chargeJson(Some(3400), Some(1000), Some(3400), currentTaxYearEnd.toString),
          chargeJson(Some(1000.00), Some(100.00), Some(3400), currentTaxYearEnd.toString, "SA Payment on Account 1", LocalDate.now().plusDays(1).toString),
          chargeJson(Some(1000.00), Some(0.00), Some(3400), currentTaxYearEnd.toString, "SA Payment on Account 2", LocalDate.now().minusDays(1).toString)
        ))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        OK, mixedJson)

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
        OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
        isElementVisibleById("pre-mtd-payments-heading")(true),
        isElementVisibleById("balancing-charge-type-table-head")(true),
        isElementVisibleById("balancing-charge-type-0")(true),
        isElementVisibleById("balancing-charge-type-1")(true),
        isElementVisibleById("payment-type-dropdown-title")(true),
        isElementVisibleById("payment-details-content-0")(true),
        isElementVisibleById("payment-details-content-1")(true),
        isElementVisibleById("over-due-payments-heading")(false),
        isElementVisibleById("over-due-type-0")(false),
        isElementVisibleById("over-due-type-1")(false),
        isElementVisibleById("due-in-thirty-days-payments-heading")(true),
        isElementVisibleById("due-in-thirty-days-type-0")(true),
        isElementVisibleById("future-payments-heading")(false),
        isElementVisibleById(s"payment-days-note")(true),
        isElementVisibleById(s"sa-note-migrated")(true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(true)
      )
    }

    "redirect to an internal server error page when both connectors return internal server error" in {
      enable(NewFinancialDetailsApi)
      enable(AgentViewer)
      enable(Payment)
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
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${currentTaxYearEnd.toInt - 1}-04-06", s"${currentTaxYearEnd.toInt}-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to an internal server error page when financial connector return internal server error" in {
      enable(NewFinancialDetailsApi)
      enable(AgentViewer)
      enable(Payment)
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
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${currentTaxYearEnd.toInt - 1}-04-06", s"${currentTaxYearEnd.toInt}-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to an internal server error page when Outstanding charges connector return internal server error" in {
      enable(NewFinancialDetailsApi)
      enable(AgentViewer)
      enable(Payment)
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

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"${currentTaxYearEnd.toInt - 1}-04-06", s"${currentTaxYearEnd.toInt}-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )

    }
  }

  s"return $OK " when {
    "YearOfMigration does not exists" when {
      "with a no charge" in {
        enable(Payment)
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd, None))


        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06",
          s"$currentTaxYearEnd-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(false),
          isElementVisibleById("balancing-charge-type-table-head")(false),
          isElementVisibleById("balancing-charge-type-0")(false),
          isElementVisibleById("balancing-charge-type-1")(false),
          isElementVisibleById("payment-type-dropdown-title")(false),
          isElementVisibleById("payment-details-content-0")(false),
          isElementVisibleById("payment-details-content-1")(false),
          isElementVisibleById("over-due-payments-heading")(false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(false),
          isElementVisibleById("future-payments-heading")(false),
          isElementVisibleById(s"payment-days-note")(false),
          isElementVisibleById(s"credit-on-account")(false),
          isElementVisibleById(s"payment-button")(false),
          isElementVisibleById(s"no-payments-due")(true),
          isElementVisibleById(s"sa-note")(false),
          isElementVisibleById(s"outstanding-charges-note")(false)
        )
      }
    }

    "YearOfMigration exists but not the first year" when {
      "with a no charge" in {
        val testTaxYear = LocalDate.now().getYear - 3
        enable(Payment)
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear-1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino,
          s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, testTaxYear.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(false),
          isElementVisibleById("balancing-charge-type-table-head")(false),
          isElementVisibleById("balancing-charge-type-0")(false),
          isElementVisibleById("balancing-charge-type-1")(false),
          isElementVisibleById("payment-type-dropdown-title")(false),
          isElementVisibleById("payment-details-content-0")(false),
          isElementVisibleById("payment-details-content-1")(false),
          isElementVisibleById("over-due-payments-heading")(false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(false),
          isElementVisibleById("future-payments-heading")(false),
          isElementVisibleById(s"payment-days-note")(false),
          isElementVisibleById(s"credit-on-account")(false),
          isElementVisibleById(s"payment-button")(false),
          isElementVisibleById(s"no-payments-due")(true),
          isElementVisibleById(s"sa-note")(false),
          isElementVisibleById(s"outstanding-charges-note")(false)
        )
      }
    }

    "YearOfMigration exists and No valid charges exists" when {
      "with a no charge" in {
        enable(Payment)
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
            "financialDetails" -> Json.arr(
              chargeJson(Some(3400), Some(1000), Some(3400), currentTaxYearEnd.toString, "test"),
              chargeJson(Some(1000.00), None, Some(3400), currentTaxYearEnd.toString, "4444"),
              chargeJson(Some(1000.00), Some(3000.00), Some(3400), currentTaxYearEnd.toString, "5555")
            ))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
          testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(false),
          isElementVisibleById("balancing-charge-type-table-head")(false),
          isElementVisibleById("balancing-charge-type-0")(false),
          isElementVisibleById("payment-type-dropdown-title")(false),
          isElementVisibleById("payment-details-content-0")(false),
          isElementVisibleById("payment-details-content-1")(false),
          isElementVisibleById("over-due-payments-heading")(false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(false),
          isElementVisibleById("future-payments-heading")(false),
          isElementVisibleById(s"payment-days-note")(false),
          isElementVisibleById(s"credit-on-account")(false),
          isElementVisibleById(s"payment-button")(false),
          isElementVisibleById(s"no-payments-due")(true),
          isElementVisibleById(s"sa-note")(false),
          isElementVisibleById(s"outstanding-charges-note")(false)
        )
      }
    }

    "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" when {
      "only BCD charge" in {
        enable(Payment)
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "financialDetails" -> Json.arr(
            chargeJson(Some(3400), Some(1000), Some(3400), currentTaxYearEnd.toString, "test"),
            chargeJson(Some(1000.00), None, Some(3400), currentTaxYearEnd.toString, "3333"),
            chargeJson(Some(1000.00), Some(3000.00), Some(3400), currentTaxYearEnd.toString, "4444")
          ))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)



        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd -1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(true),
          isElementVisibleById("balancing-charge-type-table-head")(true),
          isElementVisibleById("balancing-charge-type-0")(true),
          isElementVisibleById("balancing-charge-type-1")(true),
          isElementVisibleById("payment-type-dropdown-title")(true),
          isElementVisibleById("payment-details-content-0")(true),
          isElementVisibleById("payment-details-content-1")(true),
          isElementVisibleById("over-due-payments-heading")(false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(false),
          isElementVisibleById("future-payments-heading")(false),
          isElementVisibleById(s"payment-days-note")(true),
          isElementVisibleById(s"credit-on-account")(true),
          isElementVisibleById(s"payment-button")(true),
          isElementVisibleById(s"no-payments-due")(false),
          isElementVisibleById(s"sa-note-migrated")(true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(true)
        )
      }
    }

    "YearOfMigration exists with valid financial details charges and invalid outstanding charges" when {
      "only BCD charge" in {
        enable(Payment)
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05"
        )(OK, testValidFinancialDetailsModelJson(
          2000, 2000, currentTaxYearEnd.toString, s"${currentTaxYearEnd+1}-01-01"))

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetails)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitle("What you owe - Your client’s Income Tax details - GOV.UK"),
          isElementVisibleById("pre-mtd-payments-heading")(false),
          isElementVisibleById("balancing-charge-type-table-head")(false),
          isElementVisibleById("balancing-charge-type-0")(false),
          isElementVisibleById("balancing-charge-type-1")(false),
          isElementVisibleById("payment-type-dropdown-title")(true),
          isElementVisibleById("payment-details-content-0")(true),
          isElementVisibleById("payment-details-content-1")(true),
          isElementVisibleById("over-due-payments-heading")(false),
          isElementVisibleById("due-in-thirty-days-payments-heading")(false),
          isElementVisibleById("future-payments-heading")(true),
          isElementVisibleById("future-payments-type-0")(true),
          isElementVisibleById("future-payments-type-1")(true),
          isElementVisibleById(s"payment-days-note")(true),
          isElementVisibleById(s"credit-on-account")(true),
          isElementVisibleById(s"payment-button")(true),
          isElementVisibleById(s"no-payments-due")(false),
          isElementVisibleById(s"sa-note-migrated")(true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(true)
        )
      }
    }

  }
}
