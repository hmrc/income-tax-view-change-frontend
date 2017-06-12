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

package views

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import models.ObligationModel
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Messages, MessagesApi}
import play.twirl.api.Html
import utils.ImplicitLongDate._

@Singleton
class Helpers @Inject()(implicit val messagesApi: MessagesApi) {

  def currentTime(): LocalDate = LocalDate.now()

  def getObligationStatus(obligation: ObligationModel): Html = {
    val now = currentTime()
          (obligation.met, obligation.due) match {
            case (true, _)                                => Html(Messages("status.received"))
            case (false, date) if now.isBefore(date)      => Html(Messages("status.open", obligation.due.toLongDate))
            case (false, _)                               => Html(Messages("status.overdue"))
          }

  }
}
