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

package services

import config.FrontendAppConfig
import javax.inject.Inject
import models.btaNavBar.{ListLinks, NavContent, NavLinks}
import play.api.i18n.{Lang, Messages}

class BtaNavBarService @Inject()()(implicit val appConfig: FrontendAppConfig) {

  def notificationBadgeCount(messageCount: Int): String = {
    messageCount match {
      case 0 => "0"
      case count if count > 99 => "+99"
      case _ => s"${messageCount}"
    }
  }

  def formsNav(form: NavLinks)(implicit lang: Lang): ListLinks = {
    form.alerts match {
      case Some(alert) if alert.equals(0) => ListLinks("", "", showBoolean = Some(false))
      case Some(alert) if lang.code == "cy" => ListLinks(form.cy, form.url, Some(notificationBadgeCount(alert)))
      case Some(alert) => ListLinks(form.en, form.url, Some(notificationBadgeCount(alert)))
      case _ => ListLinks("", "", showBoolean = Some(false))
    }
  }

  def partialList(navLinks: Option[NavContent])(implicit messages: Messages): Seq[ListLinks] = {
    implicit val lang: Lang = messages.lang
    navLinks match {
      case Some(NavContent(home, account, message, help, forms)) =>
        if (messages.lang == Lang("cy")) {
          Seq(
            ListLinks(home.cy, appConfig.homePageUrl),
            ListLinks(account.cy, account.url),
            ListLinks(message.cy, message.url, Some(notificationBadgeCount(message.alerts.getOrElse(0)))),
            formsNav(forms),
            ListLinks(help.cy, help.url)
          )
        } else {
          Seq(
            ListLinks(home.en, appConfig.homePageUrl),
            ListLinks(account.en, account.url),
            ListLinks(message.en, message.url, Some(notificationBadgeCount(message.alerts.getOrElse(0)))),
            formsNav(forms),
            ListLinks(help.en, help.url)
          )
        }
      case None => Seq()
    }
  }

}
