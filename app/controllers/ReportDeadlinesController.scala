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
import config.featureswitch.{FeatureSwitching, NextUpdates, ObligationsPage, ReportDeadlines}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import javax.inject.{Inject, Singleton}
import models.reportDeadlines.ObligationsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.ReportDeadlinesService
import uk.gov.hmrc.http.HeaderCarrier

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
      if (isEnabled(ReportDeadlines) && isEnabled(NextUpdates)) {
				renderViewNextUpdates
			} else if (isEnabled(ReportDeadlines)) {
        renderViewBothObligations
      } else
        fRedirectToHome
  }

  private def renderViewBothObligations[A](implicit user: MtdItUser[A]): Future[Result] = {
    if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
      auditReportDeadlines(user)
      for {
        currentObligations <- reportDeadlinesService.getReportDeadlines().map {
          case currentObligations: ObligationsModel => currentObligations
          case _ => ObligationsModel(List())
        }
        previousObligations <- reportDeadlinesService.getReportDeadlines(true).map {
          case previousObligations: ObligationsModel => previousObligations
          case _ => ObligationsModel(List())
        }
      } yield {
        (currentObligations, previousObligations) match {
          case (currentObligations, previousObligations) if currentObligations.obligations.nonEmpty =>
            Ok(views.html.obligations(currentObligations, previousObligations, dateFormatter))
          case _ =>
            itvcErrorHandler.showInternalServerError
        }
      }
    } else {
      Future.successful(Ok(views.html.noReportDeadlines()))
    }
  }

  private def renderViewNextUpdates[A](implicit user: MtdItUser[A]): Future[Result] = {
    if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
      auditReportDeadlines(user)
      for {
        nextUpdates <- reportDeadlinesService.getReportDeadlines().map {
          case nextUpdates: ObligationsModel => nextUpdates
          case _ => ObligationsModel(List())

      }
    } yield {
        if (nextUpdates.obligations.nonEmpty) {
					Ok(views.html.nextUpdates(nextUpdates, dateFormatter))
				} else {
					itvcErrorHandler.showInternalServerError
        }
      }
    }
    else {
      Future.successful(Ok(views.html.noReportDeadlines()))
    }
  }


  private def auditReportDeadlines[A](user: MtdItUser[A])(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(ReportDeadlinesAuditModel(user), Some(controllers.routes.ReportDeadlinesController.getReportDeadlines().url))

}
