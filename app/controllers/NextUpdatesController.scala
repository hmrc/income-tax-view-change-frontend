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
import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.incomeSourceDetails.{QuarterTypeCalendar, QuarterTypeStandard}
import models.nextUpdates._
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.{NextUpdates, NoNextUpdates}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(NoNextUpdatesView: NoNextUpdates,
                                      nextUpdatesView: NextUpdates,
                                      incomeSourceDetailsService: IncomeSourceDetailsService,
                                      auditingService: AuditingService,
                                      nextUpdatesService: NextUpdatesService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      val appConfig: FrontendAppConfig,
                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                      val auth: AuthenticatorPredicate)
                                     (implicit mcc: MessagesControllerComponents,
                                      implicit val agentItvcErrorHandler: AgentItvcErrorHandler,
                                      val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def getNextUpdates(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
        for {
          nextUpdates <- nextUpdatesService.getNextUpdates().map {
            case obligations: ObligationsModel => obligations
            case _ => ObligationsModel(Nil)
          }
        } yield {
          if (nextUpdates.obligations.nonEmpty) {
            Ok(view(nextUpdates, backUrl = controllers.routes.HomeController.show(origin).url, isAgent = false, origin = origin)(user))
          } else {
            itvcErrorHandler.showInternalServerError()
          }
        }
      } else {
        Future.successful(Ok(NoNextUpdatesView(backUrl = controllers.routes.HomeController.show(origin).url)))
      }
  }

  val getNextUpdatesAgent: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
        mtdItUser =>
          nextUpdatesService.getNextUpdates()(implicitly, mtdItUser).map {
            case nextUpdates: ObligationsModel if nextUpdates.obligations.nonEmpty =>
              Ok(view(nextUpdates, controllers.routes.HomeController.showAgent.url, isAgent = true)(mtdItUser))
            case _ => agentItvcErrorHandler.showInternalServerError()
          }
      }
  }

  private def getViewModel(obligationsModel: ObligationsModel)(implicit user: MtdItUser[_]): NextUpdatesViewModel = NextUpdatesViewModel{
    obligationsModel.obligationsByDate map { case (date: LocalDate, obligations: Seq[NextUpdateModelWithIncomeType]) =>
      if (obligations.headOption.map(_.obligation.obligationType).contains("Quarterly")) {
        val obligationsByType = obligationsModel.groupByQuarterPeriod(obligations)
        DeadlineViewModel(QuarterlyObligation, standardAndCalendar = true, date, obligationsByType.getOrElse(Some(QuarterTypeStandard), Seq.empty), obligationsByType.getOrElse(Some(QuarterTypeCalendar), Seq.empty))
      }
      else DeadlineViewModel(EopsObligation, standardAndCalendar = false, date, obligations, Seq.empty)
    }
  }

  private def view(obligationsModel: ObligationsModel, backUrl: String, isAgent: Boolean, origin: Option[String] = None)
                  (implicit user: MtdItUser[_]): Html = {
    auditNextUpdates(user, isAgent, origin)
    val viewModel = getViewModel(obligationsModel)
    nextUpdatesView(currentObligations = viewModel, backUrl = backUrl, isAgent = isAgent, origin = origin)
  }

  private def auditNextUpdates[A](user: MtdItUser[A], isAgent: Boolean, origin: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    if (isAgent) {
      auditingService.audit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.getNextUpdatesAgent.url))
    } else {
      auditingService.audit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.getNextUpdates(origin).url))
    }
}