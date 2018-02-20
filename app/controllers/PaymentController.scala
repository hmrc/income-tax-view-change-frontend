/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import models.PaymentDataModel
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import config.FrontendAppConfig
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import scala.concurrent.Future



@Singleton
class PaymentController @Inject()(implicit val config: FrontendAppConfig,
                                  implicit val messagesApi: MessagesApi,
                                  val checkSessionTimeout: SessionTimeoutPredicate,
                                  val authenticate: AuthenticationPredicate) extends BaseController {

  val action = checkSessionTimeout andThen authenticate

  val paymentHandoff:Int => Action[AnyContent] = paymentAmount => action.async {
    implicit user =>
      Future.successful(Redirect(config.paymentsUrl).addingToSession(
        "payment-data" -> payment(user.mtditid, paymentAmount).toString())
      )
  }

  private def payment(mtditid: String, paymentAmount: Int) = Json.toJson[PaymentDataModel](
    PaymentDataModel("mtdfb-itsa",mtditid,paymentAmount,config.paymentRedirectUrl))

}
