/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import play.api.mvc.Call

object UrlHelper {

  def getUrl(name: String, isAgent: Boolean, origin: Option[String] = None): String = {
    name match {
      case "adjustPoa" => controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = isAgent).url
      case "whatYouOwe" => if (isAgent) controllers.routes.WhatYouOweController.showAgent.url else controllers.routes.WhatYouOweController.show(origin).url
    }
  }

  def getCall(name: String, isAgent: Boolean, showWarning: Option[Boolean] = None): Call = {
    name match {
      case "optOutConfirmation" =>
        showWarning match {
          case Some(true) => controllers.optOut.routes.SingleYearOptOutWarningController.show(isAgent)
          case _ => controllers.optOut.routes.ConfirmOptOutController.show(isAgent)
        }
      case "optOut" =>
        controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent)
      case "viewAllBusiness" =>
        controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent)
      case "viewUpcomingUpdates" =>
        if (isAgent) controllers.routes.NextUpdatesController.showAgent
        else controllers.routes.NextUpdatesController.show()
    }
  }
}
