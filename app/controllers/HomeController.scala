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

import java.time.LocalDate

import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import javax.inject.{Inject, Singleton}
import models.financialTransactions.FinancialTransactionsModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.Html
import services.{CalculationService, FinancialTransactionsService, ReportDeadlinesService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                               val authenticate: AuthenticationPredicate,
                               val retrieveNino: NinoPredicate,
                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                               val reportDeadlinesService: ReportDeadlinesService,
                               val calculationService: CalculationService,
                               val itvcErrorHandler: ItvcErrorHandler,
                               val financialTransactionsService: FinancialTransactionsService,
                               val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                               implicit val appConfig: FrontendAppConfig,
                               mcc: MessagesControllerComponents,
                               implicit val ec: ExecutionContext,
                               dateFormatter: ImplicitDateFormatterImpl) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private def view(nextPaymentDueDate: Option[LocalDate], nextUpdate: LocalDate)(implicit request: Request[_], user: MtdItUser[_]): Html = {
    views.html.home(
      nextPaymentDueDate = nextPaymentDueDate,
      nextUpdate = nextUpdate,
      paymentEnabled = isEnabled(Payment),
      implicitDateFormatter = dateFormatter
    )
  }

  val home: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>

      reportDeadlinesService.getNextDeadlineDueDate(user.incomeSources).flatMap { latestDeadlineDate =>

        calculationService.getAllLatestCalculations(user.nino, user.incomeSources.orderedTaxYears(isEnabled(API5))) flatMap {
          case lastTaxCalcs if lastTaxCalcs.exists(_.isError) => Future.successful(itvcErrorHandler.showInternalServerError())
          case lastTaxCalcs if lastTaxCalcs.nonEmpty =>
            Future.sequence(lastTaxCalcs.filter(_.isCrystallised).map { crystallisedTaxCalc =>
              financialTransactionsService.getFinancialTransactions(user.mtditid, crystallisedTaxCalc.year) map { transactions =>
                (crystallisedTaxCalc, transactions)
              }
            }).map { billDetails =>
              billDetails.filter(_._2 match {
                case ftm: FinancialTransactionsModel if ftm.financialTransactions.nonEmpty => {
                  ftm.financialTransactions.get.exists(!_.isPaid)
                }
                case _ => false
              }).sortBy(_._1.year).headOption match {
                case Some((bill, transaction: FinancialTransactionsModel)) =>
                  val date = transaction.findChargeForTaxYear(bill.year).get.items.get.head.dueDate
                  Ok(view(date, latestDeadlineDate))
                case _ => Ok(view(None, latestDeadlineDate))
              }
            }
          case _ => Future.successful(Ok(view(None, latestDeadlineDate)))
        }
      }
  }
}


