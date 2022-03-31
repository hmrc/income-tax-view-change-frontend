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

package models.navBar

import auth.MtdItUser
import play.api.libs.json.Json
import play.twirl.api.Html

case class BtaNavBarRequest[A](request: MtdItUser[A], serviceInfoContent: Option[Html])

case class ListLinks(message: String, url: String, alerts: Option[String] = None, showBoolean: Option[Boolean] = Some(true))

case class NavLinks(en: String, cy: String, url: String, alerts: Option[Int] = None)

object NavLinks {
  implicit val format = Json.format[NavLinks]
}

case class NavContent(home: NavLinks,
                      account: NavLinks,
                      messages: NavLinks,
                      help: NavLinks,
                      forms: NavLinks
                     )

object NavContent {
  implicit val format = Json.format[NavContent]
}
