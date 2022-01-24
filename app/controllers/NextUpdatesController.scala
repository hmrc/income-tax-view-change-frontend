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
import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, BtaNavBarPredicate, IncomeSourceDetailsPredicateNoCache, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.nextUpdates.ObligationsModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.NextUpdatesService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{NextUpdates, NoNextUpdates}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(NoNextUpdatesView: NoNextUpdates,
                                      nextUpdatesView: NextUpdates,
                                      checkSessionTimeout: SessionTimeoutPredicate,
                                      authenticate: AuthenticationPredicate,
                                      retrieveNino: NinoPredicate,
                                      retrieveIncomeSourcesNoCache: IncomeSourceDetailsPredicateNoCache,
                                      auditingService: AuditingService,
                                      nextUpdatesService: NextUpdatesService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      val retrieveBtaNavBar: BtaNavBarPredicate,
                                      val appConfig: FrontendAppConfig)
                                     (implicit mcc: MessagesControllerComponents,
                                          val executionContext: ExecutionContext)
  extends BaseController with I18nSupport {

  val getNextUpdates: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSourcesNoCache andThen retrieveBtaNavBar).async {
    implicit user =>
      if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
        renderViewNextUpdates
      } else {
        Future.successful(Ok(NoNextUpdatesView(backUrl = backUrl)))
      }
  }

  private def renderViewNextUpdates[A](implicit user: MtdItUser[A]): Future[Result] = {
      auditNextUpdates(user)
      for {
        nextUpdates <- getObligations()
    } yield {
        if (nextUpdates.obligations.nonEmpty) {
          Ok(nextUpdatesView(nextUpdates, backUrl = backUrl))
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

  lazy val backUrl: String = controllers.routes.HomeController.home().url


}
