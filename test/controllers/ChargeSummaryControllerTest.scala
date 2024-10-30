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

import audit.AuditingService
import auth.FrontendAuthorisedFunctions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.IncomeSourcesFormsSpec.tsTestUser
import models.admin.ChargeHistory
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import org.mockito.Mockito
import org.mockito.Mockito.{mock, spy, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.mvc.MessagesControllerComponents
import services.{ChargeHistoryService, DateServiceInterface, FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import utils.AuthenticatorPredicate
import views.html.ChargeSummary
import views.html.errorPages.CustomNotFoundError

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class ChargeSummaryControllerTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  val financialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
  val auditingService: AuditingService = mock(classOf[AuditingService])
  val itvcErrorHandler: ItvcErrorHandler = mock(classOf[ItvcErrorHandler])
  val chargeHistoryService: ChargeHistoryService = mock(classOf[ChargeHistoryService])
  val chargeSummaryView: ChargeSummary = mock(classOf[ChargeSummary])
  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])
  val authorisedFunctions: FrontendAuthorisedFunctions = mock(classOf[FrontendAuthorisedFunctions])
  val customNotFoundErrorView: CustomNotFoundError = mock(classOf[CustomNotFoundError])
  val authenticator: AuthenticatorPredicate = mock(classOf[AuthenticatorPredicate])

  implicit val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  implicit val dateService: DateServiceInterface = mock(classOf[DateServiceInterface])
  implicit val languageUtils: LanguageUtils = mock(classOf[LanguageUtils])
  implicit val  mcc: MessagesControllerComponents = mock(classOf[MessagesControllerComponents])
  implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler = mock(classOf[AgentItvcErrorHandler])

  val controller: ChargeSummaryController = spy(new ChargeSummaryController(authenticator,
    financialDetailsService, auditingService, itvcErrorHandler, chargeSummaryView, incomeSourceDetailsService,
    chargeHistoryService, authorisedFunctions, customNotFoundErrorView))

  before {
    Mockito.reset(controller)
  }

  "For ChargeSummaryController.mandatoryViewDataPresent " when {

    "viewing view-section-1" should {

      "return true when original amount is present" in {

        val isLatePaymentCharge: Boolean = false
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(false)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }
    }

    "viewing view-section-2" should {

      "return true when interest end date is present" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }

      "error when interest end date is missing" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(None)
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(_) => fail(s"should have failed due to missing interest end date")
          case Left(_) =>
        }
      }

      "return true when latePaymentInterestAmount is present" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }

      "error when latePaymentInterestAmount is missing" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(None)
        when(documentDetail.latePaymentInterestAmount).thenReturn(None)
        when(documentDetail.interestOutstandingAmount).thenReturn(None)

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(_) => fail(s"should have failed due to missing latePaymentInterestAmount")
          case Left(_) =>
        }
      }

    }


    "viewing view-section-3" should {

      "return true when original amount is present" in {

        val isLatePaymentCharge: Boolean = false
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }
    }
  }
}