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

package controllers.agent

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.Lang
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

@Singleton
class AgentLanguageController @Inject()(implicit mcc: MessagesControllerComponents,
                                        appConfig: FrontendAppConfig,
                                        languageUtils: LanguageUtils) extends LanguageController(languageUtils, mcc) {

  override def fallbackURL: String = controllers.agent.routes.EnterClientsUTRController.show().url //will fall back to Agent home when implemented

  override protected def languageMap: Map[String, Lang] = appConfig.languageMap

}
