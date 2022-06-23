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

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, MFACreditsAndDebits}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.CreditsSummary

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditsSummaryController @Inject()(creditsView: CreditsSummary,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         financialDetailsService: FinancialDetailsService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         checkSessionTimeout: SessionTimeoutPredicate,
                                         retrieveBtaNavBar: NavBarPredicate,
                                         authenticate: AuthenticationPredicate,
                                         retrieveNino: NinoPredicate,
                                         retrieveIncomeSources: IncomeSourceDetailsPredicate)
                                        (implicit val appConfig: FrontendAppConfig,
                                         mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext,
                                         val agentItvcErrorHandler: AgentItvcErrorHandler
                                        ) extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  // TODO needs to be implemented
  private def getFinancialsByTaxYear(calendarYear: Int, isAgent: Boolean)(f: List[DocumentDetail] => Future[Result])
                                    (implicit user: MtdItUser[AnyContent]): Future[Result] = {
    financialDetailsService.getFinancialDetails(calendarYear, user.nino).flatMap {
      case FinancialDetailsModel(_, _, documentDetails, _) =>
        val docDetailsMFACredits = documentDetails.filter(_.validMFACreditDescription())

        val financialDetailsWithCredit: List[DocumentDetail] = docDetailsMFACredits.filter { dd =>
          dd.documentDate.getYear == calendarYear
        }

        println(s"££££££££££££££££££££ $financialDetailsWithCredit £££££££££££££££££££££££££ ")
        f(financialDetailsWithCredit)

      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)

      case _ if isAgent =>
        Logger("application").error(s"[TaxYearSummaryController][withTaxYearFinancials] - Could not retrieve financial details for year: $calendarYear")
        Future.successful(agentItvcErrorHandler.showInternalServerError())
      case _ =>
        Logger("application").error(s"[Agent][TaxYearSummaryController][withTaxYearFinancials] - Could not retrieve financial details for year: $calendarYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def creditsSummaryUrl(calendarYear: Int, origin: Option[String]): String = controllers.routes.CreditsSummaryController.showCreditsSummary(calendarYear, origin).url

  lazy val creditAndRefundUrl: String = controllers.routes.CreditAndRefundController.show().url

  private def paymentHistoryUrl(origin: Option[String]): String = controllers.routes.PaymentHistoryController.show(origin).url

  private def agentCreditsSummaryUrl(calendarYear: Int): String = controllers.routes.CreditsSummaryController.showAgentCreditsSummary(calendarYear).url

  lazy val agentCreditAndRefundUrl: String = controllers.routes.CreditAndRefundController.showAgent().url

  lazy val agentPaymentHistoryHomeUrl: String = controllers.routes.PaymentHistoryController.showAgent().url

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
                   (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    if (isEnabled(MFACreditsAndDebits)) {
      getFinancialsByTaxYear(calendarYear, isAgent) { charges =>
        Future.successful(Ok(creditsView(
          calendarYear = calendarYear,
          backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER), calendarYear) else getBackURL(user.headers.get(REFERER), origin, calendarYear),
          isAgent = isAgent,
          utr = user.saUtr,
          btaNavPartial = user.btaNavPartial,
          enableMfaCreditsAndDebits = isEnabled(MFACreditsAndDebits),
          charges = charges,
          origin = origin)))
      }
    } else {
      Future.successful(Redirect(controllers.routes.HomeController.show().url))
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
}
