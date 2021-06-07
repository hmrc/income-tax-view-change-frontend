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
import audit.models.ReportDeadlinesAuditing.ReportDeadlinesAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, NextUpdates}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import models.reportDeadlines.ObligationsModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.ReportDeadlinesService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportDeadlinesController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val auditingService: AuditingService,
                                          val reportDeadlinesService: ReportDeadlinesService,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val appConfig: FrontendAppConfig,
                                          implicit val mcc: MessagesControllerComponents,
                                          implicit val executionContext: ExecutionContext,
                                          implicit val dateFormatter: ImplicitDateFormatterImpl)
                                          extends BaseController with FeatureSwitching with I18nSupport {

  val getReportDeadlines: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
        NextUpdates.fold(
          ifEnabled = renderViewNextUpdates,
          ifDisabled = renderViewBothObligations)
      } else {
        Future.successful(Ok(views.html.noReportDeadlines(backUrl = backUrl)))
      }
  }

  private def renderViewBothObligations[A](implicit user: MtdItUser[A]): Future[Result] = {
      auditReportDeadlines(user)
      for {
        currentObligations <- getObligations()
        previousObligations <- getObligations(previous = true)
      } yield {
        (currentObligations, previousObligations) match {
          case (currentObligations, previousObligations) if currentObligations.obligations.nonEmpty =>
            Ok(views.html.obligations(currentObligations, previousObligations, dateFormatter, backUrl = backUrl))
          case _ =>
            itvcErrorHandler.showInternalServerError
        }
      }
  }

  private def renderViewNextUpdates[A](implicit user: MtdItUser[A]): Future[Result] = {
      auditReportDeadlines(user)
      for {
        nextUpdates <- getObligations()
    } yield {
        if (nextUpdates.obligations.nonEmpty) {
					Ok(views.html.nextUpdates(nextUpdates, dateFormatter, backUrl = backUrl))
				} else {
					itvcErrorHandler.showInternalServerError
        }
      }
  }

  private def getObligations[A](previous: Boolean = false)
                               (implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsModel] =
    reportDeadlinesService.getReportDeadlines(previous).map {
      case obligations: ObligationsModel => obligations
      case _ => ObligationsModel(Nil)
    }

  private def auditReportDeadlines[A](user: MtdItUser[A])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    auditingService.audit(ReportDeadlinesAuditModel(user), Some(controllers.routes.ReportDeadlinesController.getReportDeadlines().url))

  lazy val backUrl: String = controllers.routes.HomeController.home().url


}
