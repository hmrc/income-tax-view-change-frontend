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

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import mocks.auth.MockAuthActions
import mocks.services.{MockCreditHistoryService, MockDateService, MockFinancialDetailsService}
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.Mockito.{mock, reset, when}
import play.api
import play.api.Application
import testConstants.BaseTestConstants.{testNino, testUserTypeAgent, testUserTypeIndividual}
import testUtils.TestSupport

import java.time.LocalDate

class ChargeSummaryControllerTest extends MockAuthActions
  with MockFinancialDetailsService
  with TestSupport
  with MockCreditHistoryService
  with MockDateService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(documentDetailWithDueDate)
    reset(documentDetail)
  }

  lazy val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])
  lazy val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])


  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[DocumentDetail].toInstance(documentDetail),
      api.inject.bind[DocumentDetailWithDueDate].toInstance(documentDetailWithDueDate)
    ).build()

  override lazy val tsTestUser: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeIndividual), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  override lazy val tsTestUserAgent: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeAgent), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))


  lazy val testController = app.injector.instanceOf[ChargeSummaryController]

  val interestEndDate: LocalDate = LocalDate.of(2024, 11, 5)

  "For ChargeSummaryController.mandatoryViewDataPresent " when {
    "viewing view-section-1" should {
      "return true when original amount is present" in {
        val isLatePaymentCharge: Boolean = false
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(false)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(interestEndDate))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = testController.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

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

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(interestEndDate))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = testController.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }

      "error when interest end date is missing" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(None)
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = testController.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(_) => fail(s"should have failed due to missing interest end date")
          case Left(_) =>
        }
      }

      "return true when latePaymentInterestAmount is present" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(interestEndDate))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = testController.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }

      }

      "error when latePaymentInterestAmount is missing" in {

        val isLatePaymentCharge: Boolean = true
        val documentDetailWithDueDate: DocumentDetailWithDueDate = mock(classOf[DocumentDetailWithDueDate])

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(None)
        when(documentDetail.latePaymentInterestAmount).thenReturn(None)
        when(documentDetail.interestOutstandingAmount).thenReturn(None)

        val outcome = testController.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

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

        val documentDetail: DocumentDetail = mock(classOf[DocumentDetail])
        when(documentDetailWithDueDate.documentDetail).thenReturn(documentDetail)
        when(documentDetail.isPayeSelfAssessment).thenReturn(true)

        when(documentDetail.originalAmount).thenReturn(10)
        when(documentDetail.interestEndDate).thenReturn(Some(interestEndDate))
        when(documentDetail.latePaymentInterestAmount).thenReturn(Some(BigDecimal.valueOf(10)))

        val outcome = testController.mandatoryViewDataPresent(isLatePaymentCharge, documentDetailWithDueDate)(tsTestUser)

        outcome match {
          case Right(b) => assert(b, s"should be true but got: $b")
          case Left(e) => fail(s"should have passed but got error: $e")
        }
      }
    }
  }
}