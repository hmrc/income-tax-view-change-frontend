/*
 * Copyright 2026 HM Revenue & Customs
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

package common.config

import common.controllers.feedback.routes as feedbackRoutes

trait InternalUrlHelper {

  val basePath: String
  val isNewHubUrl: Boolean
  lazy val basePathAgent: String = s"$basePath/agents"
  lazy val signinUrl = s"$basePath/sign-in"
  lazy val signoutUrl = s"$basePath/sign-out"
  lazy val keepAliveUrl = s"$basePath/keep-alive"
  lazy val timeoutUrl = s"$basePath/session-timeout"
  lazy val agentFeedbackUrl = s"$basePathAgent/feedback"
  lazy val agentErrorUrl = s"$basePathAgent/agent-error"
  lazy val clientRelationshipFailureUrl = s"$basePathAgent/not-authorised-to-view-client"
  lazy val notEnrolledUrl = s"$basePath/cannot-access-service"
  lazy val noIncomeSourcesUrl: Boolean => String = isAgent =>
    if(isAgent)
      s"$basePathAgent/no-income-sources"
    else
      s"$basePath/no-income-sources"

  lazy val noAssignmentUrl = s"$basePathAgent/no-assignment"

  //feedback
  lazy val feedbackUrl = s"$basePath/feedback"
  lazy val feedbackPostCall = if(isNewHubUrl) {
    feedbackRoutes.FeedbackController.submitNewUrl()
  } else {
    feedbackRoutes.FeedbackController.submit()
  }
  lazy val feedbackAgentPostCall = if (isNewHubUrl) {
    feedbackRoutes.FeedbackController.submitAgentNewUrl()
  } else {
    feedbackRoutes.FeedbackController.submitAgent()
  }

  lazy val feedBackThankYouUrl: Boolean => String = isAgent =>
    if(isAgent)
      s"$basePathAgent/thankyou"
    else
      s"$basePath/thankyou"

  //Uplift
  lazy val upliftSuccessUrl = s"$basePath/uplift-success"
  lazy val upliftFailureUrl = s"$basePath/cannot-view-page"
}
