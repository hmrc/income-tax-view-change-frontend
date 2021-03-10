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

import java.time.LocalDate

import auth.{FrontendAuthorisedFunctions, MtdItUser, MtdItUserWithNino}
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.IncomeTaxAgentUser
import implicits.ImplicitDateFormatterImpl
import javax.inject.{Inject, Singleton}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, _}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import views.html.agent.Home

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(home: Home,
                               reportDeadlinesService: ReportDeadlinesService,
                               financialDetailsService: FinancialDetailsService,
                               incomeSourceDetailsService: IncomeSourceDetailsService,
                               val authorisedFunctions: FrontendAuthorisedFunctions)
                              (implicit mcc: MessagesControllerComponents,
                               implicit val appConfig: FrontendAppConfig,
                               val itvcErrorHandler: ItvcErrorHandler,
                               implicit val ec: ExecutionContext,
                               dateFormatter: ImplicitDateFormatterImpl) extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  private def view(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]],
                   nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int])
                  (implicit request: Request[_], user: MtdItUser[_]): Html = {
    home(
      nextPaymentOrOverdue = nextPaymentOrOverdue,
      nextUpdateOrOverdue = nextUpdateOrOverdue,
      paymentEnabled = isEnabled(Payment),
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      implicitDateFormatter = dateFormatter
    )
  }

  def getMtdItUserWithIncomeSources()(implicit user: IncomeTaxAgentUser, request: Request[AnyContent], hc: HeaderCarrier): Future[MtdItUser[_]] = {
    val userWithNino: MtdItUserWithNino[_] = MtdItUserWithNino(
      getClientMtditid, getClientNino, getClientName, getClientUtr, None, Some("Agent")
    )

    incomeSourceDetailsService.getIncomeSourceDetails()(hc = hc, mtdUser = userWithNino) map {
      case model@IncomeSourceDetailsModel(_, _, _) => MtdItUser(
        userWithNino.mtditid, userWithNino.nino, userWithNino.userName, model, userWithNino.saUtr, userWithNino.credId, userWithNino.userType)
      case _ => throw new InternalServerException("[HomeController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created")
    }
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(AgentViewer)) {
        for {
          mtdItUser <- getMtdItUserWithIncomeSources()
          dueObligationDetails <- reportDeadlinesService.getObligationDueDates()(implicitly, implicitly, mtdItUser)
          dueChargesDetails <- financialDetailsService.getChargeDueDates(implicitly, mtdItUser)
        } yield {
          Ok(view(dueChargesDetails, dueObligationDetails)(implicitly, mtdItUser))
        }
      } else {
        Future.failed(new NotFoundException("[HomeController][home] - Agent viewer is disabled"))
      }
  }

}
