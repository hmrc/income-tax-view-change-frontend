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

package common.viewUtils

import common.controllers.routes as appRoutes
import common.controllers.timeout.routes as timeoutRoutes
import common.controllers.feedback.routes as feedbackRoutes
import common.controllers.agent.errors.routes as agentErrorRoutes

import play.api.i18n.Messages

object InternalUrlHelper {
  
  val signinUrl = appRoutes.SignInController.signIn().url
  val signinCall = appRoutes.SignInController.signIn()
  val signoutUrl = appRoutes.SignOutController.signOut().url
  val switchLocaleToWelshUrl = appRoutes.LocalLanguageController.switchToLanguage("cymraeg").url
  val switchLocaleToEnglishUrl = appRoutes.LocalLanguageController.switchToLanguage("english").url
  val switchItvcLangToEnglishCall = appRoutes.ItvcLanguageController.switchToEnglish(None)
  val switchItvcLangToWelshCall = appRoutes.ItvcLanguageController.switchToWelsh(None)
  val keepAliveUrl = timeoutRoutes.SessionTimeoutController.keepAlive().url
  val timeoutUrl = timeoutRoutes.SessionTimeoutController.timeout().url
  val timeoutCall = timeoutRoutes.SessionTimeoutController.timeout()
  val feedbackUrl = feedbackRoutes.FeedbackController.show().url
  val agentFeedbackUrl = feedbackRoutes.FeedbackController.showAgent().url
  val agentErrorCall = agentErrorRoutes.AgentErrorController.show()

}
