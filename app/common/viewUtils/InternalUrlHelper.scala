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

import common.auth.{AuthorisedAgentWithClientDetailsRequest, MtdItUser}
import common.config.FrontendAppConfig
import common.controllers.routes as appRoutes
import common.controllers.agent.routes as agentRoutes
import common.controllers.errors.routes as errorRoutes
import common.controllers.timeout.routes as timeoutRoutes
import common.controllers.feedback.routes as feedbackRoutes
import common.controllers.agent.errors.routes as agentErrorRoutes
import hub.v2.controllers.routes as hubAppRoutes
import hub.v2.controllers.agent.routes as hubAgentRoutes
import hub.v2.controllers.errors.routes as hubErrorRoutes
import hub.v2.controllers.timeout.routes as hubTimeoutRoutes
import hub.v2.controllers.feedback.routes as hubFeedbackRoutes
import hub.v2.controllers.agent.errors.routes as hubAgentErrorRoutes
object InternalUrlHelper {

  val signinCall = hubAppRoutes.SignInController.signIn()

  val signinUrl = signinCall.url
  
  val signoutUrl = hubAppRoutes.SignOutController.signOut().url

  val switchItvcLangToEnglishCall= hubAppRoutes.ItvcLanguageController.switchToEnglish(None)
  
  val switchItvcLangToWelshCall= hubAppRoutes.ItvcLanguageController.switchToWelsh(None)
  
  val switchLocaleToWelshUrl = hubAppRoutes.LocalLanguageController.switchToLanguage("cymraeg").url

  val switchLocaleToEnglishUrl = hubAppRoutes.LocalLanguageController.switchToLanguage("english").url

  val keepAliveUrl = hubTimeoutRoutes.SessionTimeoutController.keepAlive().url

  val timeoutCall = hubTimeoutRoutes.SessionTimeoutController.timeout()

  val timeoutUrl = timeoutCall.url
  
  val feedbackUrl = hubFeedbackRoutes.FeedbackController.show().url

  val agentFeedbackUrl = hubFeedbackRoutes.FeedbackController.showAgent().url

  val agentErrorCall = hubAgentErrorRoutes.AgentErrorController.show()
  
  def noIncomeSourceUrl(isAgent: Boolean) = {
      hubAppRoutes.NoIncomeSourcesController.show(isAgent)
  }

  val upliftSuccessUrl = hubAppRoutes.UpliftSuccessController.success().url

  val upliftFailureUrl = hubErrorRoutes.UpliftFailedController.show().url

  val clientRelationshipFailureCall = hubAgentRoutes.ClientRelationshipFailureController.show()

  val noAssignmentCall = hubAgentRoutes.NoAssignmentController.show().url

  val notEnrolledCall = hubErrorRoutes.NotEnrolledController.show().url

}
