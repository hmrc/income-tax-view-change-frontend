/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.HomeAudit
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.ImplicitDateFormatterImpl
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, _}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.NotFoundException
import views.html.agent.Home

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(home: Home,
                               reportDeadlinesService: ReportDeadlinesService,
                               financialDetailsService: FinancialDetailsService,
                               incomeSourceDetailsService: IncomeSourceDetailsService,
                               auditingService: AuditingService,
                               val authorisedFunctions: FrontendAuthorisedFunctions)
                              (implicit mcc: MessagesControllerComponents,
                               implicit val appConfig: FrontendAppConfig,
                               val itvcErrorHandler: ItvcErrorHandler,
                               implicit val ec: ExecutionContext,
                               dateFormatter: ImplicitDateFormatterImpl) extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  private def view(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]],
                   nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int],
                   overduePaymentExists: Boolean)
                  (implicit request: Request[_], user: MtdItUser[_]): Html = {
    home(
      nextPaymentOrOverdue = nextPaymentOrOverdue,
      nextUpdateOrOverdue = nextUpdateOrOverdue,
      overduePaymentExists = overduePaymentExists,
      paymentEnabled = isEnabled(Payment),
      paymentHistoryEnabled = isEnabled(PaymentHistory),
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      implicitDateFormatter = dateFormatter
    )
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(AgentViewer)) {
        for {
          mtdItUser <- getMtdItUserWithIncomeSources(incomeSourceDetailsService)
          dueObligationDetails <- reportDeadlinesService.getObligationDueDates()(implicitly, implicitly, mtdItUser)
          dueChargesDetails <- financialDetailsService.getChargeDueDates(implicitly, mtdItUser)
        } yield {
          auditingService.extendedAudit(HomeAudit(
            mtdItUser, dueChargesDetails, dueObligationDetails, user.agentReferenceNumber
          ))
          Ok(view(dueChargesDetails, dueObligationDetails, overduePaymentExists(dueChargesDetails))(implicitly, mtdItUser))
        }
      } else {
        Future.failed(new NotFoundException("[HomeController][home] - Agent viewer is disabled"))
      }
  }

  private def overduePaymentExists(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]]): Boolean = {
    nextPaymentOrOverdue match {
      case Some(_@Left((_, true))) => true
      case Some(_@Right(overdueCount)) if overdueCount > 0 => true
      case _ => false
    }
  }

}
