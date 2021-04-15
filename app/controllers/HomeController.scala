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
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import models.financialDetails.FinancialDetailsModel
import models.financialTransactions.FinancialTransactionsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, FinancialTransactionsService, ReportDeadlinesService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.CurrentDateProvider

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                               val authenticate: AuthenticationPredicate,
                               val retrieveNino: NinoPredicate,
                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                               val reportDeadlinesService: ReportDeadlinesService,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val financialTransactionsService: FinancialTransactionsService,
                               val financialDetailsService: FinancialDetailsService,
                               val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                               implicit val appConfig: FrontendAppConfig,
                               mcc: MessagesControllerComponents,
                               implicit val ec: ExecutionContext,
                               val currentDateProvider: CurrentDateProvider,
                               dateFormatter: ImplicitDateFormatterImpl,
                               auditingService: AuditingService) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private def view(nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate, overDuePayments: Option[Int], overDueUpdates: Option[Int])(implicit request: Request[_], user: MtdItUser[_]): Html = {
    views.html.home(
      nextPaymentDueDate = nextPaymentDueDate,
      nextUpdate = nextUpdate,
      overDuePayments = overDuePayments,
      overDueUpdates = overDueUpdates,
      paymentEnabled = isEnabled(Payment),
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      paymentHistoryEnabled = isEnabled(PaymentHistory),
      implicitDateFormatter = dateFormatter
    )
  }

  val home: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>

      reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(user.incomeSources).flatMap { latestDeadlineDate =>
        val allCharges: Future[Seq[LocalDate]] = Future.sequence(user.incomeSources.orderedTaxYears(isEnabled(API5)).map { taxYear =>
          if (isEnabled(NewFinancialDetailsApi)) {
            financialDetailsService.getFinancialDetails(taxYear, user.nino)
          }
          else {
            financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear)
          }
        }) map {
          _.filter(_ match {
            case ftm: FinancialTransactionsModel if ftm.financialTransactions.nonEmpty => {
              ftm.financialTransactions.get.exists(!_.isPaid)
            }
            case fdm: FinancialDetailsModel if fdm.financialDetails.nonEmpty => {
              fdm.financialDetails.exists(!_.isPaid)
            }
            case _ =>
              false
          }) flatMap {
            case ftm: FinancialTransactionsModel =>
              ftm.financialTransactions.get.flatMap(_.charges().map(_.dueDate.get))
            case fdm: FinancialDetailsModel =>
              fdm.financialDetails.flatMap(_.charges().map(b => LocalDate.parse(b.dueDate.get)))
          }
        }

        allCharges.map(_.sortBy(_.toEpochDay())).map { paymentsDue =>
          val overDuePayments = paymentsDue.count(_.isBefore(currentDateProvider.getCurrentDate()))
          val overDueUpdates = latestDeadlineDate._2.size

          auditingService.extendedAudit(HomeAudit(
            mtdItUser = user,
            paymentsDue.headOption,
            latestDeadlineDate._1,
            overDuePayments,
            overDueUpdates
          ))

          Ok(view(paymentsDue.headOption, latestDeadlineDate._1, Some(overDuePayments), Some(overDueUpdates)))
        }

      }.recover {
        case ex => {
          Logger.error(s"[HomeController][home] Downstream error, ${ex.getMessage}")
          itvcErrorHandler.showInternalServerError()
        }
      }
  }
}
