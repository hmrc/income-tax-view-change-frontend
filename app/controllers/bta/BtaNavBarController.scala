/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.bta

import connectors.BtaNavBarPartialConnector
import javax.inject.Inject
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{MessagesControllerComponents, Request}
import play.api.routing.Router.RequestImplicits.WithHandlerDef
import play.twirl.api.Html
import services.BtaNavBarService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.navBar.BtaNavBar

import scala.concurrent.{ExecutionContext, Future}

class BtaNavBarController @Inject()(BtaNavBarPartialConnector: BtaNavBarPartialConnector,
                                    navBar: BtaNavBar,
                                    mcc: MessagesControllerComponents,
                                    navBarService: BtaNavBarService) extends FrontendController(mcc) with Logging {

  def btaNavBarPartial[A](request: Request[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Html]] = {
    val maybeNavLinks = BtaNavBarPartialConnector.getNavLinks()
    implicit val messages: Messages = mcc.messagesApi.preferred(request.request)
    for {
      navLinks <- maybeNavLinks
    } yield {
      logger.info("[BtaNavBarController][btaNavBarPartial] successful")
      Some(navBar(navBarService.partialList(navLinks)))
    }
  }
}
