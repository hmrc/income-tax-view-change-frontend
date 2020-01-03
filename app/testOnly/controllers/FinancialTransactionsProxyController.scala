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

package testOnly.controllers

import javax.inject.{Inject, Singleton}

import config.FrontendAppConfig
import controllers.BaseController
import play.api.i18n.MessagesApi
import play.api.mvc._
import testOnly.connectors.FinancialTransactionsProxyConnector

@Singleton
class FinancialTransactionsProxyController @Inject()(val financialTransactionsProxyConnector: FinancialTransactionsProxyConnector,
                                                     implicit val appConfig: FrontendAppConfig,
                                                     implicit val messagesApi: MessagesApi) extends BaseController {

  def getFinancialData(regime: String,
                       mtditid: String,
                       onlyOpenItems: Option[String],
                       dateFrom: Option[String],
                       dateTo: Option[String],
                       includeLocks: Option[String],
                       calculateAccruedInterest: Option[String],
                       customerPaymentInfo: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      financialTransactionsProxyConnector.getFinancialData(
        regime,
        mtditid,
        onlyOpenItems,
        dateFrom,
        dateTo,
        includeLocks,
        calculateAccruedInterest,
        customerPaymentInfo
      ).map(
        response =>
          Status(response.status)(response.json)
      )
  }
}
