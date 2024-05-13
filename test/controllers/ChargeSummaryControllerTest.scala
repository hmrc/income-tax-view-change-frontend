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
import connectors.FinancialDetailsConnector
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, SessionTimeoutPredicate}
import forms.IncomeSourcesFormsSpec.tsTestUser
import models.admin.{ChargeHistory, CodingOut}
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import org.mockito.Mockito
import org.mockito.Mockito.{mock, spy, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.mvc.MessagesControllerComponents
import services.{DateServiceInterface, FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.ChargeSummary
import views.html.errorPages.CustomNotFoundError

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class ChargeSummaryControllerTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  val authenticate: AuthenticationPredicate = mock(classOf[AuthenticationPredicate])
  val checkSessionTimeout: SessionTimeoutPredicate = mock(classOf[SessionTimeoutPredicate])
  val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate = mock(classOf[IncomeSourceDetailsPredicate])
  val financialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
  val auditingService: AuditingService = mock(classOf[AuditingService])
  val itvcErrorHandler: ItvcErrorHandler = mock(classOf[ItvcErrorHandler])
  val financialDetailsConnector: FinancialDetailsConnector = mock(classOf[FinancialDetailsConnector])
  val chargeSummaryView: ChargeSummary = mock(classOf[ChargeSummary])
  val retrievebtaNavPartial: NavBarPredicate = mock(classOf[NavBarPredicate])
  val incomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])
  val authorisedFunctions: FrontendAuthorisedFunctions = mock(classOf[FrontendAuthorisedFunctions])
  val customNotFoundErrorView: CustomNotFoundError = mock(classOf[CustomNotFoundError])

  implicit val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  implicit val dateService: DateServiceInterface = mock(classOf[DateServiceInterface])
  implicit val languageUtils: LanguageUtils = mock(classOf[LanguageUtils])
  implicit val  mcc: MessagesControllerComponents = mock(classOf[MessagesControllerComponents])
  implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler = mock(classOf[AgentItvcErrorHandler])

  val controller: ChargeSummaryController = spy(new ChargeSummaryController(authenticate, checkSessionTimeout, retrieveNinoWithIncomeSources,
    financialDetailsService, auditingService, itvcErrorHandler, financialDetailsConnector, chargeSummaryView,
    retrievebtaNavPartial, incomeSourceDetailsService, authorisedFunctions, customNotFoundErrorView))

  before {
    Mockito.reset(controller)
  }

  "For ChargeSummaryController.mandatoryViewDataPresent " when {

    "viewing view-section-1" should {

      "return true when original amount is present" in {

        val isLatePaymentCharge: Boolean = false
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)
        controller.disable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(false)

        when(documentDetail.originalAmount).thenReturn(Some(10))
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }

      "error when original amount is missing" in {

        val isLatePaymentCharge: Boolean = false
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)
        controller.disable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(false)

        when(documentDetail.originalAmount).thenReturn(None)
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(_) => fail(s"should have failed due to missing original value")
          case Left(_) =>
        }
      }

    }

    "viewing view-section-2" should {

      "return true when interest end date is present" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)
        controller.disable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(Some(10))
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
        controller.disable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(Some(10))
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
        controller.disable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(Some(10))
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
        controller.disable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(Some(10))
        when(documentDetail.interestEndDate).thenReturn(None)
        when(documentDetail.latePaymentInterestAmount).thenReturn(None)

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
        controller.enable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(Some(10))
        when(documentDetail.interestEndDate).thenReturn(Some(LocalDate.now()))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }

      "error when original amount is missing" in {

        val isLatePaymentCharge: Boolean = false
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        controller.enable(ChargeHistory)
        controller.enable(CodingOut)

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(None)
        when(documentDetail.interestEndDate).thenReturn(None)
        when(documentDetail.latePaymentInterestAmount).thenReturn(None)

        val outcome = controller.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(_) => fail(s"should have failed due to missing original amount")
          case Left(_) =>
        }
      }
    }
  }
}