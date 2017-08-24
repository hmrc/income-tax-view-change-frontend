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

import javax.inject.{Inject, Singleton}

import config.AppConfig
import controllers.predicates.AsyncActionPredicate
import models.ObligationsModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ObligationsService

@Singleton
class ObligationsController @Inject()(implicit val config: AppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val actionPredicate: AsyncActionPredicate,
                                      val obligationsService: ObligationsService
                                     ) extends BaseController {

  val getObligations: Action[AnyContent] = actionPredicate.async {
    implicit request =>
      implicit user =>
        implicit sources =>
          for {
            business <- obligationsService.getBusinessObligations(user.nino, sources.businessDetails)
            property <- obligationsService.getPropertyObligations(user.nino, sources.propertyDetails)
          } yield Ok(views.html.obligations(business, property))
  }
}

