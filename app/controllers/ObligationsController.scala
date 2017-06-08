/*
 * Copyright 2017 HM Revenue & Customs
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

import com.google.inject.Inject
import config.AppConfig
import controllers.predicates.AuthenticationPredicate
import models.{ObligationModel, ObligationsModel}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import utils.ImplicitDateFormatter._

import scala.concurrent.Future

class ObligationsController @Inject()(implicit val config: AppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val authentication: AuthenticationPredicate
                                     ) extends BaseController {

  def getObligations(): Action[AnyContent] = authentication.async { implicit request => implicit mtditid =>

    val dummyObligations =
      ObligationsModel(
        List(
        ObligationModel(
          start = localDate("2017-04-06"),
          end = localDate("2017-07-05"),
          due = localDate("2017-08-05"),
          met = true
        ), ObligationModel(
          start = localDate("2017-07-06"),
          end = localDate("2017-10-05"),
          due = localDate("2017-11-05"),
          met = true
        ), ObligationModel(
          start = localDate("2017-10-06"),
          end = localDate("2018-01-05"),
          due = localDate("2018-02-05"),
          met = false
        ), ObligationModel(
          start = localDate("2018-01-06"),
          end = localDate("2018-04-05"),
          due = localDate("2018-05-06"),
          met = false
        )
      )
    )
    Future.successful(Ok(views.html.obligations(dummyObligations)))
  }

}
