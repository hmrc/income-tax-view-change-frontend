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

import javax.inject.{Inject, Singleton}

import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}

import scala.concurrent.Future

@Singleton
class BusinessDetailsController @Inject()(implicit val config: FrontendAppConfig,
                                          implicit val messagesApi: MessagesApi,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler
                                        ) extends BaseController {

  val getBusinessDetails: Int => Action[AnyContent] = id =>
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
      implicit user =>
        if (config.features.accountDetailsEnabled()) renderView(id) else fRedirectToHome
    }

  private def renderView[A](id: Int)(implicit user: MtdItUser[A]): Future[Result] = {
    user.incomeSources.findBusinessById(id) match {
      case Some(business) =>
        Future.successful(Ok(views.html.businessDetailsView(business)))
      case _ =>
        Logger.error(s"[BusinessDetailsController][getBusinessDetails] No Business Details found with ID: $id")
        Future.successful(itvcErrorHandler.showInternalServerError)
    }
  }
}
