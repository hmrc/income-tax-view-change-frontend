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

package controllers

import audit.AuditingService
import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, NextUpdates}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.nextUpdates.ObligationsModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.NextUpdatesService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{NoNextUpdates, Obligations, NextUpdates}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(NoNextUpdatesView: NoNextUpdates,
                                      obligationsView: Obligations,
                                      nextUpdatesView: NextUpdates,
                                      checkSessionTimeout: SessionTimeoutPredicate,
                                      authenticate: AuthenticationPredicate,
                                      retrieveNino: NinoPredicate,
                                      retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      auditingService: AuditingService,
                                      NextUpdatesService: NextUpdatesService,
                                      itvcErrorHandler: ItvcErrorHandler,
                                      val appConfig: FrontendAppConfig)
                                     (implicit mcc: MessagesControllerComponents,
                                          val executionContext: ExecutionContext)
  extends BaseController with FeatureSwitching with I18nSupport {

  val getNextUpdates: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
        NextUpdates.fold(
          ifEnabled = renderViewNextUpdates,
          ifDisabled = renderViewBothObligations)
      } else {
        Future.successful(Ok(NoNextUpdatesView(backUrl = backUrl)))
      }
  }

  private def renderViewBothObligations[A](implicit user: MtdItUser[A]): Future[Result] = {
      auditNextUpdates(user)
      for {
        currentObligations <- getObligations()
        previousObligations <- getObligations(previous = true)
      } yield {
        (currentObligations, previousObligations) match {
          case (currentObligations, previousObligations) if currentObligations.obligations.nonEmpty =>
            Ok(obligationsView(currentObligations, previousObligations, backUrl = backUrl))
          case _ =>
            itvcErrorHandler.showInternalServerError
        }
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
    NextUpdatesService.getNextUpdates(previous).map {
      case obligations: ObligationsModel => obligations
      case _ => ObligationsModel(Nil)
    }

  private def auditNextUpdates[A](user: MtdItUser[A])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    auditingService.audit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.getNextUpdates().url))

  lazy val backUrl: String = controllers.routes.HomeController.home().url


}
