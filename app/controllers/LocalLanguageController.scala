/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

class LocalLanguageController @Inject()(
                                         languageUtils: LanguageUtils,
                                         controllerComponent: ControllerComponents,
                                         override val messagesApi: MessagesApi
                                       ) extends LanguageController(languageUtils, controllerComponent) {

  override def languageMap: Map[String, Lang] =
    Map(
      "english" -> Lang("en"),
      "cymraeg" -> Lang("cy")
    )

  override def fallbackURL: String = controllers.routes.HomeController.show().url
}
