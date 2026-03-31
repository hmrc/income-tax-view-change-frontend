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

package auth.authV2.actions

import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.bta.BtaNavBarController
import forms.utils.SessionKeys
import models.OriginEnum
import models.OriginEnum.{BTA, PTA}
import models.admin.NavBarFs
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.*
import uk.gov.hmrc.govukfrontend.views.Aliases.{ServiceNavigationItem, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.servicenavigation.ServiceNavigation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.navBar.{BtaPartial, PtaPartial}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NavBarRetrievalAction @Inject()(val btaNavBarController: BtaNavBarController,
                                      val ptaPartial: PtaPartial,
                                      val btaPartial: BtaPartial,
                                      val itvcErrorHandler: ItvcErrorHandler)
                                     (implicit val appConfig: FrontendAppConfig,
                                      val executionContext: ExecutionContext,
                                      val messagesApi: MessagesApi
                                     ) extends ActionRefiner[MtdItUser, MtdItUser] with SaveOriginAndRedirect {

  override def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
    val header: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val messages: Messages = messagesApi.preferred(request)
    implicit val hc: HeaderCarrier = header.copy(extraHeaders = header.headers(Seq(play.api.http.HeaderNames.COOKIE)))
    lazy val navigationBarDisabled = !isEnabled(NavBarFs)(request)
    request.getQueryString(SessionKeys.origin) match {
      case Some(_) => saveOriginAndReturnToHomeWithoutQueryParams(request, navigationBarDisabled).map(Left(_))
      case None if navigationBarDisabled => Future.successful(Right(request))
      case None => retrieveCacheAndHandleNavBar(request)
    }
  }

  def retrieveCacheAndHandleNavBar[A](request: MtdItUser[A])(implicit hc: HeaderCarrier, messages: Messages): Future[Either[Result, MtdItUser[A]]] = {
    (request.session.get(SessionKeys.origin), appConfig.itvcRebrand) match {
      case (Some(origin), true) if OriginEnum(origin).contains(PTA)  => Future.successful(Right(request.addServiceNavigation(createPtaServiceNavigation())))
      case (Some(origin), true) if OriginEnum(origin).contains(BTA)  => Future.successful(Right(request.addServiceNavigation(createBtaServiceNavigation())))
      case (Some(origin), false) if OriginEnum(origin).contains(PTA) => Future.successful(Right(request.addNavBar(ptaPartial()(request, request.messages, appConfig))))
      case (Some(origin), false) if OriginEnum(origin).contains(BTA) => handleBtaNavBar(request)
      case _                                                         => Future.successful(Right(request))
    }
  }

  private def handleBtaNavBar[A](request: MtdItUser[A])(implicit hc: HeaderCarrier): Future[Either[Result, MtdItUser[A]]] = {
    btaNavBarController.btaNavBarPartial(request) map { partial =>
      Right(request.addNavBar(partial))
    }
  }

  private def createBtaServiceNavigation()(implicit messages: Messages): ServiceNavigation = {
    ServiceNavigation(
      navigation = Seq(
        ServiceNavigationItem(
          content = Text(messages("bta.navigation.manageAccount")),
          href = appConfig.businessTaxAccountManageAccountUrl
        ),
        ServiceNavigationItem(
          content = Text(messages("bta.navigation.messages")),
          href = appConfig.businessTaxAccountMessagesUrl
        ),
        ServiceNavigationItem(
          content = Text(messages("bta.navigation.helpAndContact")),
          href = appConfig.businessTaxAccountHelpUrl
        )
      ),
      navigationId = "bta-service-navigation"
    )
  }

  private def createPtaServiceNavigation()(implicit messages: Messages): ServiceNavigation = {
    ServiceNavigation(
      navigation = Seq(
        ServiceNavigationItem(
          content = Text(messages("pta.navigation.messages")),
          href = appConfig.personalTaxAccountMessagesUrl
        ),
        ServiceNavigationItem(
          content = Text(messages("pta.navigation.checkProgress")),
          href = appConfig.personalTaxAccountCheckProgressUrl
        ),
        ServiceNavigationItem(
          content = Text(messages("pta.navigation.profileAndSettings")),
          href = appConfig.personalTaxAccountProfileUrl
        ),
        ServiceNavigationItem(
          content = Text(messages("pta.navigation.businessTaxAccount")),
          href = appConfig.personalTaxAccountBtaUrl
        )
      ),
      navigationId = "pta-service-navigation"
    )
  }
}
