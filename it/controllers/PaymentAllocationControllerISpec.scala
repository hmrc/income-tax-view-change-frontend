
package controllers

import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PaymentAllocationIntegrationTestConstants._
import audit.models.PaymentAllocationsResponseAuditModel
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest

class PaymentAllocationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(PaymentAllocation)
  }

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  val docNumber = "docNumber1"

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    paymentHistoryBusinessAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())



  s"GET ${controllers.routes.PaymentAllocationsController.viewPaymentAllocation(docNumber).url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        isAuthorisedUser(authorised = false)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber)

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }


    s"return $NOT_FOUND" when {
      "the payment allocation feature switch is disabled" in {
        disable(PaymentAllocation)
        isAuthorisedUser(authorised = true)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber)

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }

    s"return $OK with the payment allocation page for non LPI" when {
      "the payment allocation feature switch is enabled and with TxmEventsApproved FS enabled" in {
        enable(TxmEventsApproved)
        enable(PaymentAllocation)
        isAuthorisedUser(authorised = true)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
        IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, "paymentLot", "paymentLotItem")(OK, Json.toJson(testValidPaymentAllocationsModel))
        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000872")(OK, validPaymentAllocationChargesJson)
        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000873")(OK, validPaymentAllocationChargesJson)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber)

        Then("The Payment allocation page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitle("Payment made to HMRC - Business Tax account - GOV.UK")
        )

        verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, paymentAllocationViewModel).detail)
      }
    }

    s"return $OK with the payment allocation page for LPI" when {
      "the payment allocation feature switch is enabled and with TxmEventsApproved FS enabled" in {
        enable(TxmEventsApproved)
        enable(PaymentAllocation)
        isAuthorisedUser(authorised = true)
        stubUserDetails()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(nino = testNino, from = s"${getCurrentTaxYearEnd.getYear - 1}-04-06",
          to = s"${getCurrentTaxYearEnd.getYear}-04-05")(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
        IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, "paymentLot", "paymentLotItem")(OK, Json.toJson(testValidLpiPaymentAllocationsModel))
        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000872")(OK, validPaymentAllocationChargesJson)
        IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000873")(OK, validPaymentAllocationChargesJson)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber)

        Then("The Payment allocation page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitle("Payment made to HMRC - Business Tax account - GOV.UK"),
          elementAttributeBySelector("#payment-allocation-0 a", "href")(
            "/report-quarterly/income-and-expenses/view/tax-years/9999/charge?id=PAYID01&latePaymentCharge=true"),
          elementTextBySelector("#payment-allocation-0 a")("Late payment interest for Balancing payment 9999")
        )

        verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, lpiPaymentAllocationViewModel).detail)
      }
    }
  }
}
