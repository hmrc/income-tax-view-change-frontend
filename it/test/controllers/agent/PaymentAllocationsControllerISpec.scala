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

package controllers.agent

import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.PaymentAllocation
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PaymentAllocationIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

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
    testMtditid, testNino, None, paymentHistoryBusinessAndPropertyResponse,
    None, Some("1234567890"), None, Some(Agent), Some("1")
  )(FakeRequest())

  lazy val fixedDate : LocalDate = LocalDate.of(2020, 11, 29)


  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      Some("Test Trading Name"),
      None,
      Some(getCurrentTaxYearEnd),
      None,
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = List(
      PropertyDetailsModel(
        "testId2",
        Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
        None,
        None,
        Some(getCurrentTaxYearEnd),
        None,
        cashOrAccruals = false
      )
    )
  )


  s"GET ${controllers.routes.PaymentAllocationsController.viewPaymentAllocationAgent(docNumber).url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber)

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
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
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }

    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }

      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }
    }
    s"return $NOT_FOUND" when {
      "the PaymentAllocation feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        disable(PaymentAllocation)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show.url)
        )
      }
    }

    s"return $OK and display the Payment Allocations page" in {
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
      IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
      IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino,
        paymentAllocationChargesModel.documentDetails.head.paymentLot.get,
        paymentAllocationChargesModel.documentDetails.head.paymentLotItem.get)(OK, testValidPaymentAllocationsModelJson)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPaymentAllocation(docNumber, clientDetailsWithConfirmation)

      result should have(
        httpStatus(OK),
        pageTitleAgent("paymentAllocation.heading")
      )

      verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser, paymentAllocationViewModel).detail)
    }

    s"return $OK and display the Payment Allocations page and new LPI layout" in {
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
        pageTitleAgent(titleInternalServer, isErrorPage = true)
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
