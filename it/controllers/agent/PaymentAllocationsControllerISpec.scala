
package controllers.agent

import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PaymentAllocationIntegrationTestConstants._
import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import java.time.LocalDate

class PaymentAllocationsControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PaymentAllocation)
  }

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  val docNumber = "docNumber1"

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    paymentHistoryBusinessAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some("1")
  )(FakeRequest())

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val clientDetailsWithoutConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid
  )

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        Some(getCurrentTaxYearEnd)
      )
    )
  )


  s"GET ${controllers.agent.routes.PaymentAllocationsController.viewPaymentAllocation(docNumber).url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber)

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }

    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber)

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Your client’s Income Tax details - GOV.UK")
        )
      }
    }

    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }

      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
    s"return $NOT_FOUND" when {
      "the PaymentAllocation feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)

        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Your client’s Income Tax details - GOV.UK")
        )
      }
    }

    s"return $OK and display the Payment Allocations page and with TxmEventsApproved FS enabled" in {
      enable(TxmEventsApproved)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
      IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino,
        paymentAllocationChargesModel.documentDetails.head.paymentLot.get,
        paymentAllocationChargesModel.documentDetails.head.paymentLotItem.get)(OK, testValidPaymentAllocationsModelJson)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000872")(OK, validPaymentAllocationChargesJson)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000873")(OK, validPaymentAllocationChargesJson)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)

      result should have(
        httpStatus(OK),
        pageTitle("Payment made to HMRC - Your client’s Income Tax details - GOV.UK")
      )

      verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, paymentAllocationViewModel).detail)
    }

    s"return $OK and display the Payment Allocations page and with TxmEventsApproved FS enabled and new LPI layout" in {
      enable(TxmEventsApproved)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(nino = testNino, from = s"${getCurrentTaxYearEnd.getYear - 1}-04-06",
        to = s"${getCurrentTaxYearEnd.getYear}-04-05")(OK, testValidFinancialDetailsModelJson(10.34, 1.2))
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
      IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, "paymentLot", "paymentLotItem")(OK, Json.toJson(testValidLpiPaymentAllocationsModel))
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000872")(OK, validPaymentAllocationChargesJson)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000873")(OK, validPaymentAllocationChargesJson)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)

      result should have(
        httpStatus(OK)
      )

      verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, lpiPaymentAllocationViewModel).detail)
    }

    s"return $INTERNAL_SERVER_ERROR when the payment allocations call fails" in {
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(INTERNAL_SERVER_ERROR, Json.obj())

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)

      result should have(
        httpStatus(INTERNAL_SERVER_ERROR),
        pageTitle("Sorry, there is a problem with the service - Your client’s Income Tax details - GOV.UK")
      )
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => {
          enable(PaymentAllocation)
          IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)
        })
    }
  }
}
