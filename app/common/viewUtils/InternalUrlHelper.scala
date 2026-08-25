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

import common.config.FrontendAppConfig
import common.controllers.routes as appRoutes
import common.controllers.timeout.routes as timeoutRoutes
import common.controllers.feedback.routes as feedbackRoutes
import common.controllers.agent.errors.routes as agentErrorRoutes
import hub.v2.controllers.routes as hubAppRoutes
import hub.v2.controllers.timeout.routes as hubTimeoutRoutes
import hub.v2.controllers.feedback.routes as hubFeedbackRoutes
import hub.v2.controllers.agent.errors.routes as hubAgentErrorRoutes
object InternalUrlHelper {

  def signinCall(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAppRoutes.SignInController.signIn()
    else
      appRoutes.SignInController.signIn()
  }
  def signinUrl(implicit appConfig: FrontendAppConfig) = signinCall.url
  
  def signoutUrl(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAppRoutes.SignOutController.signOut().url
    else
      appRoutes.SignOutController.signOut().url
  }

  def switchItvcLangToEnglishCall(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAppRoutes.ItvcLanguageController.switchToEnglish(None)
    else
      appRoutes.ItvcLanguageController.switchToEnglish(None)
  }
  
  def switchItvcLangToWelshCall(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAppRoutes.ItvcLanguageController.switchToWelsh(None)
    else
      appRoutes.ItvcLanguageController.switchToWelsh(None)
  }
  
  def switchLocaleToWelshUrl(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAppRoutes.LocalLanguageController.switchToLanguage("cymraeg").url
    else
      appRoutes.LocalLanguageController.switchToLanguage("cymraeg").url
  }
  def switchLocaleToEnglishUrl(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAppRoutes.LocalLanguageController.switchToLanguage("english").url
    else
      appRoutes.LocalLanguageController.switchToLanguage("english").url
  }
  def keepAliveUrl(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubTimeoutRoutes.SessionTimeoutController.keepAlive().url
    else
      timeoutRoutes.SessionTimeoutController.keepAlive().url
  }

  def timeoutCall(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubTimeoutRoutes.SessionTimeoutController.timeout()
    else
      timeoutRoutes.SessionTimeoutController.timeout()
  }

  def timeoutUrl(implicit appConfig: FrontendAppConfig) = timeoutCall.url
  
  def feedbackUrl(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubFeedbackRoutes.FeedbackController.show().url
    else
      feedbackRoutes.FeedbackController.show().url
  }
  def agentFeedbackUrl(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubFeedbackRoutes.FeedbackController.showAgent().url
    else
      feedbackRoutes.FeedbackController.showAgent().url
  }
  def agentErrorCall(implicit appConfig: FrontendAppConfig) = {
    if (appConfig.hubContextRootEnabledConfig)
      hubAgentErrorRoutes.AgentErrorController.show()
    else
      agentErrorRoutes.AgentErrorController.show()
  }

}
