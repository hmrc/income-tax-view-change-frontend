/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.FrontendAuthorisedFunctions
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignOutController @Inject()(config: FrontendAppConfig,
                                  enrolmentsAuthService: FrontendAuthorisedFunctions,
                                  mcc: MessagesControllerComponents
                                   ) extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  val signOut:Action[AnyContent] = Action.async { implicit request =>
    enrolmentsAuthService.authorised.retrieve(Retrievals.affinityGroup) {
      case Some(AffinityGroup.Agent) => Future.successful(s"${config.contactFormServiceIdentifier}A")
      case _ => Future.successful(config.contactFormServiceIdentifier)
    }
      .map(contactFormIdentifier => Redirect(config.ggSignOutUrl(contactFormIdentifier)))
      .recover {
        case _: AuthorisationException => Redirect(config.signInUrl)
      }
  }
}
