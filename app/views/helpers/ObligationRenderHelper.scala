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

package views.helpers

import models._
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import utils.ImplicitDateFormatter.localDateOrdering
import utils.ImplicitListMethods
import views.html.templates.obligations.{obligations_error_template, obligations_template}

object ObligationRenderHelper extends ImplicitListMethods {

  def renderObligations(obs: ObligationsResponseModel, id: String)(implicit messages: Messages): HtmlFormat.Appendable =
    obs match {
      case obligations: ObligationsModel => obligations_template(subsetObligations(obligations), id)
      case _: ObligationsErrorModel => obligations_error_template(id)
    }

  def subsetObligations(obs: ObligationsModel): ObligationsModel =
    ObligationsModel(getLatestReceived(obs.obligations) ++ getAllOverdue(obs.obligations) ++ getNextDue(obs.obligations))

  private def getLatestReceived(obligations: List[ObligationModel]): List[ObligationModel] =
    obligations.filter(_.getObligationStatus == Received).maxItemBy(_.due)

  private def getAllOverdue(obligations: List[ObligationModel]): List[ObligationModel] =
    obligations.filter(_.getObligationStatus == Overdue)

  private def getNextDue(obligations: List[ObligationModel]): List[ObligationModel] =
    obligations.filter(_.getObligationStatus.isInstanceOf[Open]).minItemBy(_.due)

}
