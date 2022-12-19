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

import audit.AuditingService
import audit.models.CreditsSummaryAuditing
import auth.MtdItUser
import config.featureswitch.{CutOverCredits, FeatureSwitching, MFACreditsAndDebits, R7cTxmEvents}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.creditDetailModel.{CreditDetailModel, CutOverCreditType, MfaCreditType}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreditHistoryService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.CreditsSummary

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditsSummaryController @Inject()(creditsView: CreditsSummary,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         creditHistoryService: CreditHistoryService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         checkSessionTimeout: SessionTimeoutPredicate,
                                         retrieveBtaNavBar: NavBarPredicate,
                                         authenticate: AuthenticationPredicate,
                                         retrieveNino: NinoPredicate,
                                         retrieveIncomeSources: IncomeSourceDetailsPredicate)
                                        (implicit val appConfig: FrontendAppConfig,
                                         mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext,
                                         val agentItvcErrorHandler: AgentItvcErrorHandler,
                                         val auditingService: AuditingService
                                        ) extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def creditsSummaryUrl(calendarYear: Int, origin: Option[String]): String =
    controllers.routes.CreditsSummaryController.showCreditsSummary(calendarYear, origin).url

  lazy val creditAndRefundUrl: String = controllers.routes.CreditAndRefundController.show().url

  private def paymentHistoryUrl(origin: Option[String]): String =
    controllers.routes.PaymentHistoryController.show(origin).url

  private def agentCreditsSummaryUrl(calendarYear: Int): String =
    controllers.routes.CreditsSummaryController.showAgentCreditsSummary(calendarYear).url

  lazy val agentCreditAndRefundUrl: String = controllers.routes.CreditAndRefundController.showAgent().url

  lazy val agentPaymentHistoryHomeUrl: String = controllers.routes.PaymentHistoryController.showAgent.url

  private def getBackURL(referer: Option[String], origin: Option[String], calendarYear: Int): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(url) if url.equals(paymentHistoryUrl(origin)) => paymentHistoryUrl(origin)
      case Some(url) if url.equals(creditAndRefundUrl) => creditAndRefundUrl
      case _ => creditsSummaryUrl(calendarYear, origin)
    }
  }

  private def getAgentBackURL(referer: Option[String], calendarYear: Int): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(`agentPaymentHistoryHomeUrl`) => agentPaymentHistoryHomeUrl
      case Some(`agentCreditAndRefundUrl`) => agentCreditAndRefundUrl
      case _ => agentCreditsSummaryUrl(calendarYear)
    }
  }

  def handleRequest(calendarYear: Int,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[AnyContent],
                    hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    if (isDisabled(MFACreditsAndDebits) && isDisabled(CutOverCredits)) {
      auditCreditSummary(None, Seq.empty)
      Future.successful(Ok(creditsView(
        calendarYear = calendarYear,
        backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER), calendarYear) else getBackURL(user.headers.get(REFERER), origin, calendarYear),
        isAgent = isAgent,
        utr = user.saUtr,
        btaNavPartial = user.btaNavPartial,
        charges = List.empty,
        maybeAvailableCredit = None,
        origin = origin))
      )
    } else {
      creditHistoryService.getCreditsHistory(calendarYear, user.nino).flatMap {
        case Right(credits) =>
          val charges: List[CreditDetailModel] = ((isEnabled(MFACreditsAndDebits), isEnabled(CutOverCredits)) match {
            case (true, false) => credits.filter(_.creditType == MfaCreditType)
            case (false, true) => credits.filter(_.creditType == CutOverCreditType)
            case _ => credits
          }).sortBy(_.date.toEpochDay)
          val maybeAvailableCredit: Option[BigDecimal] =
            credits.flatMap(_.balanceDetails.flatMap(_.availableCredit.filter(_ > 0.00))).headOption
          auditCreditSummary(maybeAvailableCredit, charges)
          Future.successful(Ok(creditsView(
            calendarYear = calendarYear,
            backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER), calendarYear) else getBackURL(user.headers.get(REFERER), origin, calendarYear),
            isAgent = isAgent,
            utr = user.saUtr,
            btaNavPartial = user.btaNavPartial,
            charges = charges,
            maybeAvailableCredit = maybeAvailableCredit,
            origin = origin)))
        case Left(_) => {
          if (isAgent) {
            Logger("application").error(s"[CreditsSummaryController][showAgentCreditsSummary] - Could not retrieve financial details for Calendar year: $calendarYear, NINO: ${user.nino}")
            Future.successful(agentItvcErrorHandler.showInternalServerError())
          }
          else {
            Logger("application").error(s"[CreditsSummaryController][showCreditsSummary] - Could not retrieve financial details for Calendar year: $calendarYear, NINO: ${user.nino}")
            Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
      }
    }
  }

  def showCreditsSummary(calendarYear: Int, origin: Option[String] = None): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          calendarYear = calendarYear,
          isAgent = false,
          origin = origin
        )
    }
  }

  def showAgentCreditsSummary(calendarYear: Int): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          handleRequest(
            calendarYear = calendarYear,
            isAgent = true
          )
        }
    }
  }

  private def auditCreditSummary(creditOnAccount: Option[BigDecimal], charges: Seq[CreditDetailModel])
                                (implicit hc: HeaderCarrier, user: MtdItUser[_], messages:Messages): Unit = {
    import CreditsSummaryAuditing._

    if (isEnabled(R7cTxmEvents)) {
      for {
        saUtr <- user.saUtr
        userType <- user.userType
        credId <- user.credId
      } yield
        auditingService.extendedAudit(
          CreditsSummaryModel(
            saUTR = saUtr,
            nino = user.nino,
            userType = userType,
            credId = credId,
            mtdRef = user.mtditid,
            creditOnAccount = creditOnAccount.getOrElse(BigDecimal(0.0)).toString(),
            creditDetails = toCreditSummaryDetailsSeq(charges)(messages)
          )
        )
    }
  }
}
