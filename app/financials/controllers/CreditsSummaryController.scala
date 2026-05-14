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

package financials.controllers

import audit.models.CreditSummaryAuditing
import common.auth.{AuthActions, MtdItUser}
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.config.featureswitch.FeatureSwitching
import common.services.AuditingService
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
import financials.controllers.routes as financialsRoutes

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
    financialsRoutes.CreditsSummaryController.showCreditsSummary(calendarYear, origin).url

  lazy val moneyInYourAccountUrl: String = financialsRoutes.MoneyInYourAccountController.show().url

  private def paymentHistoryUrl(origin: Option[String]): String =
    financialsRoutes.PaymentHistoryController.show(origin).url

  private def agentCreditsSummaryUrl(calendarYear: Int): String =
    financialsRoutes.CreditsSummaryController.showAgentCreditsSummary(calendarYear).url

  private lazy val agentMoneyInYourAccountUrl: String = financialsRoutes.MoneyInYourAccountController.showAgent().url

  private lazy val agentPaymentHistoryHomeUrl: String = financialsRoutes.PaymentHistoryController.showAgent().url

  private def getBackURL(referer: Option[String], origin: Option[String], calendarYear: Int): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(url) if url.equals(paymentHistoryUrl(origin)) => paymentHistoryUrl(origin)
      case Some(url) if url.equals(moneyInYourAccountUrl) => moneyInYourAccountUrl
      case _ => creditsSummaryUrl(calendarYear, origin)
    }
  }

  private def getAgentBackURL(referer: Option[String], calendarYear: Int): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(`agentPaymentHistoryHomeUrl`) => agentPaymentHistoryHomeUrl
      case Some(`agentMoneyInYourAccountUrl`) => agentMoneyInYourAccountUrl
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
          serviceNavigationPartial = user.serviceNavigationPartial,
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
    authActions.asMTDIndividual().async {
      implicit user =>
        handleRequest(
          calendarYear = calendarYear,
          isAgent = false,
          origin = origin
        )
    }
  }

  def showAgentCreditsSummary(calendarYear: Int): Action[AnyContent] = {
    authActions.asMTDPrimaryAgent().async { implicit mtdItUser =>
      handleRequest(
        calendarYear = calendarYear,
        isAgent = true
      )
    }
  }

  private def auditCreditSummary(creditOnAccount: Option[BigDecimal], charges: Seq[CreditDetailModel])
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Unit = {
    import CreditSummaryAuditing.*
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
