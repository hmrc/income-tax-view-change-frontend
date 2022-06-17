
package controllers

import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, PaymentAllocation, R7bTxmEvents}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PaymentAllocationIntegrationTestConstants._

class PaymentAllocationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(PaymentAllocation)
    enable(R7bTxmEvents)
  }

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  val docNumber = "docNumber1"

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
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

        Then(s"The custom not found page is returned to the user")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        )
      }
    }

    s"return $OK with the payment allocation page for non LPI" when {
      "the payment allocation feature switch is enabled" in {
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
          pageTitleIndividual("paymentAllocation.heading"),
        )

        verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, paymentAllocationViewModel, false).detail)
      }
    }

    s"return $OK with the payment allocation page for LPI" when {
      "the payment allocation feature switch is enabled" in {
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
          pageTitleIndividual("paymentAllocation.heading"),
          elementAttributeBySelector("#payment-allocation-0 a", "href")(
            "/report-quarterly/income-and-expenses/view/tax-years/9999/charge?id=PAYID01&latePaymentCharge=true"),
          elementTextBySelector("#payment-allocation-0 a")(s"${messagesAPI("paymentAllocation.paymentAllocations.balancingCharge.text")} 9999")
        )

        verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, lpiPaymentAllocationViewModel, false).detail)
      }
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber))
    }
  }
}
