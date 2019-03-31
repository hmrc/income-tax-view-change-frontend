/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.financialTransactions.{FinancialTransactionsModel, FinancialTransactionsResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{CalculationService, FinancialTransactionsService, ReportDeadlinesService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

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
                               implicit val config: FrontendAppConfig,
                               val messagesApi: MessagesApi) extends FrontendController with I18nSupport {

  val home: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      calculationService.getAllLatestCalculations(user.nino, user.incomeSources.orderedTaxYears) flatMap {
        case lastTaxCalcs if lastTaxCalcs.nonEmpty => {
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
                Ok(views.html.home(date, date.get))
              case _ => Ok(views.html.home(None, LocalDate.parse("2019-01-05")))
            }
          }

        }
        case _ => Future.successful(Ok(views.html.home(None, LocalDate.parse("2019-01-01"))))
          }
      }

  }


