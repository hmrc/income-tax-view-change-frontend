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

import java.util.Calendar

import models.ObligationModel
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html
import utils.ImplicitLongDate._

object Helpers {

  def getObligationStatus(obligation: ObligationModel): Html = {
    val currentDate = Calendar.getInstance()
          (obligation.met, obligation.due) match {
            case (true, _)                                => Html(Messages("obligations.received"))
            case (false, date) if currentDate.after(date) => Html(Messages("obligations.open", obligation.due.toLongDate))
            case (false, _)                               => Html(currentDate+" "+obligation.due + "" + Messages("obligations.overdue"))
          }

  }

}
