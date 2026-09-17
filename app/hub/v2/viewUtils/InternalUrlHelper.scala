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

package hub.v2.viewUtils

import hub.v2.controllers.routes as appRoutes
import hub.v2.controllers.errors.routes as errorRoutes
import hub.v2.controllers.agent.routes as agentRoutes
import hub.v2.controllers.timeout.routes as timeoutRoutes
import hub.v2.controllers.feedback.routes as feedbackRoutes
import hub.v2.controllers.agent.errors.routes as agentErrorRoutes

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
  val clientRelationshipFailure = agentRoutes.ClientRelationshipFailureController.show()
  val upliftSuccessUrl = appRoutes.UpliftSuccessController.success().url
  val upliftFailedUrl = errorRoutes.UpliftFailedController.show().url
  val noAssignmentUrl = agentRoutes.NoAssignmentController.show()
  val notEnrolledUrl = errorRoutes.NotEnrolledController.show()
}
