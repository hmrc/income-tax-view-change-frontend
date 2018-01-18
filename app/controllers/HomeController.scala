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

import javax.inject.{Inject, Singleton}

import auth.MtdItUserWithNino
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.ServiceInfoPartialService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                               val authenticate: AuthenticationPredicate,
                               val retrieveNino: NinoPredicate,
                               val serviceInfoPartialService: ServiceInfoPartialService,
                               val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                               implicit val config: FrontendAppConfig,
                               val messagesApi: MessagesApi) extends FrontendController with I18nSupport {

  import itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest

  val home: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino).async { implicit user =>
    if (config.features.homePageEnabled()) renderView else redirectToBTA
  }

  private[HomeController] def redirectToBTA: Future[Result] = Future.successful(Redirect(config.businessTaxAccount))

  private[HomeController] def renderView[A](implicit user: MtdItUserWithNino[A]): Future[Result] = Future.successful(Ok(views.html.home()))
}
