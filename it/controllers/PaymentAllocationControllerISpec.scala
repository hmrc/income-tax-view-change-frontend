
package controllers

import assets.BaseIntegrationTestConstants.{testMtditid, testNino}
import assets.IncomeSourceIntegrationTestConstants.paymentHistoryBusinessAndPropertyResponse
import assets.PaymentAllocationChargesIntegrationTestConstants.{documentDetail, financialDetail, validPaymentAllocationChargesJson}
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, PaymentAllocation}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.paymentAllocationCharges.PaymentAllocationChargesModel
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest

class PaymentAllocationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(PaymentAllocation)
  }

  val singleTestPaymentAllocationCharge: PaymentAllocationChargesModel = PaymentAllocationChargesModel(
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

        IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, docNumber)(OK, validPaymentAllocationChargesJson)

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

        IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, docNumber)(OK, validPaymentAllocationChargesJson)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber)

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }

    s"return $OK with the payment allocation page" when {
      "the payment allocation feature switch is enabled" in {
        enable(PaymentAllocation)
        isAuthorisedUser(authorised = true)
        stubUserDetails()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

        IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, docNumber)(OK, validPaymentAllocationChargesJson)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocationCharges(docNumber)

        Then("The Payment allocation page is returned to the user")
        result should have(
          httpStatus(OK),
          pageTitle("Payment made to HMRC - Business Tax account - GOV.UK")
        )
      }
    }
  }
}
