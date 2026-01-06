/*
 * Copyright 2023 HM Revenue & Customs
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
import audit.models.CreditSummaryAuditing
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.creditDetailModel.CreditDetailModel
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.CreditHistoryService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.CreditsSummaryView

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditsSummaryController @Inject()(creditsView: CreditsSummaryView,
                                         val authActions: AuthActions,
                                         creditHistoryService: CreditHistoryService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         agentItvcErrorHandler: AgentItvcErrorHandler
                                        )(implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          msgApi: MessagesApi,
                                          val auditingService: AuditingService,
                                          ec: ExecutionContext
                                        ) extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  private def creditsSummaryUrl(calendarYear: Int, origin: Option[String]): String =
    controllers.routes.CreditsSummaryController.showCreditsSummary(calendarYear, origin).url

  lazy val creditAndRefundUrl: String = controllers.routes.CreditAndRefundController.show().url

  private def paymentHistoryUrl(origin: Option[String]): String =
    controllers.routes.PaymentHistoryController.show(origin).url

  private def agentCreditsSummaryUrl(calendarYear: Int): String =
    controllers.routes.CreditsSummaryController.showAgentCreditsSummary(calendarYear).url

  private lazy val agentCreditAndRefundUrl: String = controllers.routes.CreditAndRefundController.showAgent().url

  private lazy val agentPaymentHistoryHomeUrl: String = controllers.routes.PaymentHistoryController.showAgent().url

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
                   (implicit user: MtdItUser[_],
                    hc: HeaderCarrier): Future[Result] = {
    creditHistoryService.getCreditsHistory(calendarYear, user.nino).flatMap {
      case Right(credits) =>
        val charges: List[CreditDetailModel] = credits.sortBy(_.date.toEpochDay)
        val maybeAvailableCredit: Option[BigDecimal] =
          credits.flatMap(_.availableCredit.filter(_ > 0.00)).headOption
        auditCreditSummary(maybeAvailableCredit, charges)
        val backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER), calendarYear) else getBackURL(user.headers.get(REFERER), origin, calendarYear)
        Future.successful(Ok(creditsView(
          calendarYear = calendarYear,
          backUrl = backUrl,
          isAgent = isAgent,
          utr = user.saUtr,
          btaNavPartial = user.btaNavPartial,
          charges = charges,
          maybeAvailableCredit = maybeAvailableCredit,
          origin = origin)))
      case Left(_) =>
        if (isAgent) {
          Logger("application").error(s"- Could not retrieve financial details for Calendar year: $calendarYear, NINO: ${user.nino}")
          Future.successful(agentItvcErrorHandler.showInternalServerError())
        }
        else {
          Logger("application").error(s"- Could not retrieve financial details for Calendar year: $calendarYear, NINO: ${user.nino}")
          Future.successful(itvcErrorHandler.showInternalServerError())
        }
    }
  }

  def showCreditsSummary(calendarYear: Int, origin: Option[String] = None): Action[AnyContent] = {
    authActions.asMTDIndividual.async {
      implicit user =>
        handleRequest(
          calendarYear = calendarYear,
          isAgent = false,
          origin = origin
        )
    }
  }

  def showAgentCreditsSummary(calendarYear: Int): Action[AnyContent] = {
    authActions.asMTDPrimaryAgent.async { implicit mtdItUser =>
      handleRequest(
        calendarYear = calendarYear,
        isAgent = true
      )
    }
  }

  private def auditCreditSummary(creditOnAccount: Option[BigDecimal], charges: Seq[CreditDetailModel])
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {
    import CreditSummaryAuditing._
    auditingService.extendedAudit(
      CreditsSummaryModel(
        saUTR = user.saUtr.getOrElse(""),
        nino = user.nino,
        userType = user.userType.fold("")(_.toString),
        credId = user.credId.getOrElse(""),
        mtdRef = user.mtditid,
        creditOnAccount = creditOnAccount.getOrElse(BigDecimal(0.0)).toString(),
        creditDetails = toCreditSummaryDetailsSeq(charges)(msgApi)
      )
    )
  }
}
