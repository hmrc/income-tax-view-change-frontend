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

package views.helpers

import javax.inject.{Inject, Singleton}

import config.FrontendAppConfig
import connectors.ServiceInfoPartialConnector
import models.{Breadcrumb, BreadcrumbItem}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.twirl.api.{Html, HtmlFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object BreadcrumbHelper {
  def buildBreadcrumb(breadcrumb: Breadcrumb)(implicit messages: Messages, config: FrontendAppConfig): Future[Html] = {
    Future(
      HtmlFormat.fill(scala.collection.immutable.Seq(
        Html(
        s"""
           |<div>
           |  <nav id="breadcrumbs" class="breadcrumb-nav breadcrumb-nav--slim service-info__left">
           |    <ul class="breadcrumb-nav__list">
           |      <li class="breadcrumb-nav__item">
           |        <a id=s"breadcrumb-bta" href=${config.businessTaxAccount}>${messages("breadcrumb-bta")}</a>
           """.stripMargin),
        allCrumbs(breadcrumb),
        Html(
        s"""
           |      </li>
           |    </ul>
           |  </nav>
           |</div>
           """.stripMargin)
      ))
    )
  }

  def allCrumbs(breadcrumb: Breadcrumb)(implicit messages: Messages): Html = {
      HtmlFormat.fill(
        breadcrumb.items.map{
          item => Html(" => <a id=\"" + item.id + "\" href=\"" + item.url + "\">" + messages(item.id) + "</a>")
        }
      )
  }
}
