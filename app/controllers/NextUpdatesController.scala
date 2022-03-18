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

import scala.concurrent.{ExecutionContext, Future}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import audit.AuditingService
import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, BtaNavBarPredicate, IncomeSourceDetailsPredicateNoCache, IncomeTaxAgentUser, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.nextUpdates.ObligationsModel
import services.{IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.{NextUpdates, NoNextUpdates}

@Singleton
class NextUpdatesController @Inject()(NoNextUpdatesView: NoNextUpdates,
                                      nextUpdatesView: NextUpdates,
                                      checkSessionTimeout: SessionTimeoutPredicate,
                                      authenticate: AuthenticationPredicate,
                                      retrieveNino: NinoPredicate,
                                      retrieveIncomeSourcesNoCache: IncomeSourceDetailsPredicateNoCache,
                                      incomeSourceDetailsService: IncomeSourceDetailsService,
                                      auditingService: AuditingService,
                                      nextUpdatesService: NextUpdatesService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      val retrieveBtaNavBar: BtaNavBarPredicate,
                                      val appConfig: FrontendAppConfig,
                                      val authorisedFunctions: FrontendAuthorisedFunctions)
                                     (implicit mcc: MessagesControllerComponents,
                                      implicit val agentItvcErrorHandler: config.AgentItvcErrorHandler,
                                      val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  val getNextUpdates: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSourcesNoCache andThen retrieveBtaNavBar).async {
    implicit user =>
      if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
        renderViewNextUpdates
      } else {
        Future.successful(Ok(NoNextUpdatesView(backUrl = controllers.routes.HomeController.home().url)))
      }
  }

  val getNextUpdatesAgent: Action[AnyContent] = Authenticated.async { implicit request =>

    implicit user =>
      println(s"${implicitly[HeaderCarrier]}")
//      println(s"${implicitly[MtdItUser[AnyContentAsText]}")
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = false).flatMap {
        mtdItUser =>
          nextUpdatesService.getNextUpdates()(implicitly, mtdItUser).map {
            case nextUpdates: ObligationsModel if nextUpdates.obligations.nonEmpty => Ok(view(nextUpdates,
              controllers.agent.routes.HomeController.show().url, isAgent = true)(mtdItUser))
            case _ => agentItvcErrorHandler.showInternalServerError()
          }

      }

  }

  private def view(obligationsModel: ObligationsModel, backUrl: String, isAgent: Boolean)
                  (implicit user: MtdItUser[_]): Html = {
    nextUpdatesView(
      currentObligations = obligationsModel,
      backUrl = backUrl,
      isAgent = isAgent
    )
  }


  private def renderViewNextUpdates[A](implicit user: MtdItUser[A]): Future[Result] = {
    auditNextUpdates(user)
    for {
      nextUpdates <- getObligations()
    } yield {
      if (nextUpdates.obligations.nonEmpty) {
        Ok(nextUpdatesView(nextUpdates, backUrl = controllers.routes.HomeController.home().url))
      } else {
        itvcErrorHandler.showInternalServerError
      }
    }
  }

  private def getObligations[A](previous: Boolean = false)
                               (implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsModel] =
    nextUpdatesService.getNextUpdates(previous).map {
      case obligations: ObligationsModel => obligations
      case _ => ObligationsModel(Nil)
    }

  private def auditNextUpdates[A](user: MtdItUser[A])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    auditingService.audit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.getNextUpdates().url))

}
