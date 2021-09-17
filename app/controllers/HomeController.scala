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
import audit.models.HomeAudit
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.financialDetails.FinancialDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, NextUpdatesService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.CurrentDateProvider

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val homeView: views.html.Home,
                               val checkSessionTimeout: SessionTimeoutPredicate,
                               val authenticate: AuthenticationPredicate,
                               val retrieveNino: NinoPredicate,
                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                               val nextUpdatesService: NextUpdatesService,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val financialDetailsService: FinancialDetailsService,
                               override val appConfig: FrontendAppConfig,
                               mcc: MessagesControllerComponents,
                               implicit val ec: ExecutionContext,
                               val currentDateProvider: CurrentDateProvider,
                               auditingService: AuditingService) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private def view(nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate, overDuePayments: Option[Int], overDueUpdates: Option[Int])
                  (implicit user: MtdItUser[_]): Html = {
    homeView(
      nextPaymentDueDate = nextPaymentDueDate,
      nextUpdate = nextUpdate,
      overDuePayments = overDuePayments,
      overDueUpdates = overDueUpdates,
      user.saUtr,
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      paymentHistoryEnabled = isEnabled(PaymentHistory)
    )
  }

  val home: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>

      nextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(user.incomeSources).flatMap { latestDeadlineDate =>
        val allCharges: Future[Seq[LocalDate]] = Future.sequence(user.incomeSources.orderedTaxYears.map { taxYear =>
            financialDetailsService.getFinancialDetails(taxYear, user.nino)
        }) map {
          _.filter(_ match {
            case fdm: FinancialDetailsModel if fdm.financialDetails.nonEmpty =>
              fdm.documentDetails.exists(!_.isPaid)
            case _ =>
              false
          }) flatMap {
            case fdm: FinancialDetailsModel =>
              fdm.getAllDueDates
          }
        }

        allCharges.map(_.sortBy(_.toEpochDay())).map { paymentsDue =>
          val overDuePayments = paymentsDue.count(_.isBefore(currentDateProvider.getCurrentDate()))
          val overDueUpdates = latestDeadlineDate._2.size

          if (isEnabled(TxmEventsApproved)) {
            auditingService.extendedAudit(HomeAudit(
              mtdItUser = user,
              paymentsDue.headOption,
              latestDeadlineDate._1,
              overDuePayments,
              overDueUpdates
            ))
          }

          Ok(view(paymentsDue.headOption, latestDeadlineDate._1, Some(overDuePayments), Some(overDueUpdates)))
        }

      }.recover {
        case ex =>
          Logger.error(s"[HomeController][Home] Downstream error, ${ex.getMessage}")
          itvcErrorHandler.showInternalServerError()
      }
  }
}
