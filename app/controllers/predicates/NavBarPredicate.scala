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

package controllers.predicates

import auth.MtdItUser
import config.featureswitch.NavBarFs
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.bta.BtaNavBarController
import forms.utils.SessionKeys
import models.NavBarEnum
import models.NavBarEnum.{BTA, PTA}
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.navBar.PtaPartial

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NavBarPredicate @Inject()(val btaNavBarController: BtaNavBarController,
                                val ptaPartial: PtaPartial,
                                val itvcErrorHandler: ItvcErrorHandler)
                               (implicit val appConfig: FrontendAppConfig,
                                val executionContext: ExecutionContext,
                                val messagesApi: MessagesApi
                               ) extends ActionRefiner[MtdItUser, MtdItUser] with NavBar {

  override def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {

    val header: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val hc: HeaderCarrier = header.copy(extraHeaders = header.headers(Seq(play.api.http.HeaderNames.COOKIE)))

    if (isDisabled(NavBarFs)) {
      Future.successful(Right(request))
    } else {
      request.getQueryString(origin).fold[Future[Either[Result, MtdItUser[A]]]](ifEmpty = retrieveCacheAndHandleNavBar(request))(_ => {
        saveOriginAndReturnToHomeWithoutQueryParams(request, isDisabled(NavBarFs)).map(Left(_))
      })
    }
  }

  def retrieveCacheAndHandleNavBar[A](request: MtdItUser[A])(implicit hc: HeaderCarrier): Future[Either[Result, MtdItUser[A]]] = {
    request.session.get(SessionKeys.origin) match {
      case Some(origin) if NavBarEnum(origin) == Some(PTA) =>
        Future.successful(Right(returnMtdItUserWithNavbar(request, ptaPartial()(request, request.messages, appConfig))))
      case Some(origin) if NavBarEnum(origin) == Some(BTA) =>
        handleBtaNavBar(request)
      case _ =>
        Future.successful(Left(Redirect(appConfig.taxAccountRouterUrl)))
    }
  }

  def returnMtdItUserWithNavbar[A](request: MtdItUser[A], partial: Html): MtdItUser[A] = {
    MtdItUser[A](mtditid = request.mtditid, nino = request.nino, userName = request.userName,
      incomeSources = request.incomeSources, btaNavPartial = Some(partial), saUtr = request.saUtr, credId = request.credId,
      userType = request.userType, arn = request.arn)(request)
  }

  def handleBtaNavBar[A](request: MtdItUser[A])(implicit hc: HeaderCarrier): Future[Either[Result, MtdItUser[A]]] = {
    btaNavBarController.btaNavBarPartial(request) map {
      case Some(partial) => Right(returnMtdItUserWithNavbar(request, partial))
      case _ => Left(itvcErrorHandler.showInternalServerError()(request))
    }
  }
}
