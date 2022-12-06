/*
 * Copyright 2022 HM Revenue & Customs
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


import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import config.featureswitch.{CreditsRefundsRepay, FeatureSwitching, MFACreditsAndDebits}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreditService, DateService, IncomeSourceDetailsService, RepaymentService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utils.CreditAndRefundUtils
import utils.CreditAndRefundUtils.UnallocatedCreditType
import utils.CreditAndRefundUtils.UnallocatedCreditType.maybeUnallocatedCreditType
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditAndRefundController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val creditService: CreditService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val repaymentService: RepaymentService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          dateService: DateService,
                                          val languageUtils: LanguageUtils,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val view: CreditAndRefunds,
                                          val customNotFoundErrorView: CustomNotFoundError)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {
  private val creditsFromHMRC = "HMRC"
  private val cutOverCredits = "CutOver"
  private val payment = "Payment"

  def show(origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.HomeController.show(origin).url,
          itvcErrorHandler = itvcErrorHandler,
          isAgent = false
        )
    }

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getCreditCharges()(implicitly, user) map {
      case _ if isDisabled(CreditsRefundsRepay) =>
        Ok(customNotFoundErrorView()(user, messages))
      case financialDetailsModel: List[FinancialDetailsModel] =>
        val balance: Option[BalanceDetails] = financialDetailsModel.headOption.map(balance => balance.balanceDetails)

        val credits: List[(DocumentDetailWithDueDate, FinancialDetail)] = financialDetailsModel.flatMap(
          financialDetailsModel => sortCreditsGroupedPaymentTypes(financialDetailsModel.getAllDocumentDetailsWithDueDatesAndFinancialDetails())
        )

        val creditAndRefundType: Option[UnallocatedCreditType] = maybeUnallocatedCreditType(credits, balance)

        Ok(view(credits, balance, creditAndRefundType, isAgent, backUrl, isEnabled(MFACreditsAndDebits))(user, user, messages))
      case _ => Logger("application").error(
        s"${if (isAgent) "[Agent]"}[CreditAndRefundController][show] Invalid response from financial transactions")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def sortCreditsGroupedPaymentTypes(credits: List[(DocumentDetailWithDueDate, FinancialDetail)])
  : List[(DocumentDetailWithDueDate, FinancialDetail)] = {
    val sortingOrderCreditType = Map(
      creditsFromHMRC -> 0,
      cutOverCredits -> 1,
      payment -> 2
    )

    def sortCredits(credits: List[(DocumentDetailWithDueDate, FinancialDetail)])
    : List[(DocumentDetailWithDueDate, FinancialDetail)] = {
      credits
        .sortBy(_._1.documentDetail.paymentOrChargeCredit).reverse
    }

    val creditsGroupedPaymentTypes = credits
      .groupBy[String] {
        credits => {
          getCreditTypeGroupKey(credits)
        }
      }
      .toList.sortWith((p1, p2) => sortingOrderCreditType(p1._1) < sortingOrderCreditType(p2._1))
      .map {
        case (documentId, credits) => (documentId, sortCredits(credits))
      }.flatMap {
      case (_, credits) => credits
    }
    creditsGroupedPaymentTypes
  }

  def getCreditTypeGroupKey(credits: (DocumentDetailWithDueDate, FinancialDetail)): String = {
    val isMFA: Boolean = credits._2.validMFACreditType()
    val isCutOverCredit: Boolean = credits._2.validCutoverCreditType()
    val isPayment: Boolean = credits._1.documentDetail.paymentLot.isDefined
    (isMFA, isCutOverCredit, isPayment) match {
      case (true, false, false) => creditsFromHMRC
      case (false, true, false) => cutOverCredits
      case (false, false, true) => payment
      case (_, _, _) => throw new Exception("Credit Type Not Found")
    }
  }

  def showAgent(): Action[AnyContent] = {
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
            implicit mtdItUser =>
              handleRequest(
                backUrl = controllers.routes.HomeController.showAgent.url,
                itvcErrorHandler = itvcErrorHandlerAgent,
                isAgent = true
              )
          }
    }
  }

  def startRefund(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRefundRequest(
          backUrl = "", // TODO: do we need a backUrl
          itvcErrorHandler = itvcErrorHandler,
          isAgent = false
        )
    }

  def refundStatus(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleStatusRefundRequest(
          backUrl = "", // TODO: do we need a backUrl
          itvcErrorHandler = itvcErrorHandler,
          isAgent = false
        )
    }

  private def handleRefundRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getCreditCharges()(implicitly, user) flatMap  {
      case _ if isDisabled(CreditsRefundsRepay) =>
        Future.successful(Ok(customNotFoundErrorView()(user, messages)))

      case financialDetailsModel: List[FinancialDetailsModel] =>
        val balance: Option[BalanceDetails] = financialDetailsModel.headOption.map(balance => balance.balanceDetails)
        repaymentService.start(user.nino, balance.flatMap(_.availableCredit).getOrElse(BigDecimal(0))).flatMap { repayment =>
          repayment match {
            case RepaymentJourneyModel(nextUrl) =>
              Future.successful(Redirect(nextUrl))
            case RepaymentJourneyErrorResponse(status, message) =>
              Logger("application")
                .error(s"[CreditAndRefundController][handleRefundRequest] - failed RepaymentJourney: $status ")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
      case _ => Logger("application").error("[CreditAndRefundController][handleRefundRequest]")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def handleStatusRefundRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    repaymentService.view(user.nino).flatMap { view =>
      view match {
        case RepaymentJourneyModel(nextUrl) =>
          Future.successful(Redirect(nextUrl))
        case RepaymentJourneyErrorResponse(status, message) =>
          Logger("application")
            .error(s"[CreditAndRefundController][handleStatusRefundRequest] - failed RepaymentJourney: $status ")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }

  }

}