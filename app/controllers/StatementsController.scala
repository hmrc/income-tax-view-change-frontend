/*
 * Copyright 2020 HM Revenue & Customs
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

import auth.MtdItUserOptionNino
import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitching, Payment, Statements}
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.FinancialTransactionsService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatementsController @Inject()(implicit val appConfig: FrontendAppConfig,
                                     implicit val messagesApi: MessagesApi,
                                     implicit val ec: ExecutionContext,
                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val financialTransactionsService: FinancialTransactionsService
                                    ) extends BaseController with FeatureSwitching{

  val getStatements: Action[AnyContent] = (checkSessionTimeout andThen authenticate).async {
    implicit user => if(isEnabled(Statements)) renderView else fRedirectToHome
  }

  private[StatementsController] def renderView[A](implicit user: MtdItUserOptionNino[A]): Future[Result] = {

    val earliestYear = 2018

    financialTransactionsService.getFinancialTransactions(user.mtditid, earliestYear).map{
      case model: FinancialTransactionsModel =>
        Logger.debug("[StatementsController][getStatements] Success Response received from financialTransactionsService")
        Ok(views.html.statements(model.withYears().sortWith(_.taxYear > _.taxYear), isEnabled(Payment)))
      case _: FinancialTransactionsErrorModel =>
        Logger.error("[StatementsController][getStatements] Error Response received from financialTransactionsService")
        Ok(views.html.errorPages.statementsError())
    }
  }


}
