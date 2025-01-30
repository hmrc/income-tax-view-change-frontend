/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package models.paymentCreditAndRefundHistory

import audit.AuditingService
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.PaymentHistoryController
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import services.{DateServiceInterface, PaymentHistoryService, RepaymentService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.PaymentHistory
import views.html.errorPages.CustomNotFoundError

import scala.concurrent.{ExecutionContext, Future}

class PaymentCreditAndRefundHistorySpec extends PlaySpec with MockitoSugar {

  implicit private val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val mockAuthActions = mock[AuthActions]
  private val mockPaymentHistoryService = mock[PaymentHistoryService]
  private val mockAuditingService = mock[AuditingService]
  private val mockItvcErrorHandler = mock[ItvcErrorHandler]
  private val mockAgentItvcErrorHandler = mock[AgentItvcErrorHandler]
  private val mockRepaymentService = mock[RepaymentService]
  private val mockPaymentHistoryView = mock[PaymentHistory]
  private val mockCustomNotFoundErrorView = mock[CustomNotFoundError]
  private val mockAppConfig = mock[FrontendAppConfig]
  private val mockDateService = mock[DateServiceInterface]
  private val mockLanguageUtils = mock[LanguageUtils]

  private val stubMCC: MessagesControllerComponents = stubMessagesControllerComponents()

  private val viewModelGen: Gen[PaymentCreditAndRefundHistoryViewModel] = for {
    creditsRefundsRepayEnabled <- Gen.oneOf(true, false)
    paymentHistoryAndRefundsEnabled <- Gen.oneOf(true, false)
  } yield PaymentCreditAndRefundHistoryViewModel(creditsRefundsRepayEnabled, paymentHistoryAndRefundsEnabled)

  private def testUser[A](request: Request[A]): MtdItUser[A] = MtdItUser(
    mtditid = "123456789",
    nino = "AA123456A",
    userName = None,
    incomeSources = IncomeSourceDetailsModel(
      nino = "AA123456A",
      mtdbsa = "testMtdbsa",
      yearOfMigration = None,
      businesses = Nil,
      properties = Nil
    ),
    saUtr = None,
    credId = Some("cred-123"),
    userType = Some(AffinityGroup.Individual),
    arn = None
  )(request)

  private val fakeAuthAction = new ActionBuilder[MtdItUser, AnyContent] with ActionTransformer[Request, MtdItUser] {
    override protected def transform[A](request: Request[A]): Future[MtdItUser[A]] =
      Future.successful(testUser(request))

    override def parser: BodyParser[AnyContent] = stubMCC.parsers.default

    override protected def executionContext: ExecutionContext = ec
  }

  when(mockAuthActions.asMTDIndividual).thenReturn(fakeAuthAction)

  private val controller = new PaymentHistoryController(
    mockAuthActions,
    mockAuditingService,
    mockItvcErrorHandler,
    mockAgentItvcErrorHandler,
    mockPaymentHistoryService,
    mockRepaymentService,
    mockPaymentHistoryView,
    mockCustomNotFoundErrorView
  )(mockAppConfig, mockDateService, mockLanguageUtils, stubMCC, ec)

  "PaymentHistoryController" should {
    "return 200 OK when rendering PaymentHistory view with the generated viewModel" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/payment-refund-history")
      implicit val user: MtdItUser[_] = testUser(request)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val viewModel = viewModelGen.sample.get

      when(mockPaymentHistoryService.getPaymentHistoryV2)
        .thenReturn(Future.successful(Right(Seq.empty)))

      when(mockPaymentHistoryService.getRepaymentHistory(any[Boolean])(any[HeaderCarrier], any[MtdItUser[_]]))
        .thenReturn(Future.successful(Right(List.empty[models.repaymentHistory.RepaymentHistory])))

      when(mockPaymentHistoryView.apply(any(), any(), any(), any(), eqTo(Some(viewModel.toString)), any())(any(), any()))
        .thenReturn(play.twirl.api.HtmlFormat.empty)

      val result = controller.show(origin = None)(request)

      status(result) mustBe OK
    }
  }
}

