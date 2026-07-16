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

package hub.auth

import hub.auth.actions.TriggeredMigrationRetrievalAction
import common.auth.*
import common.auth.actions.RedirectIfNoIncomeSourcesAction
import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import hub.auth
import play.api.mvc.*

import javax.inject.{Inject, Singleton}

@Singleton
class AuthActionsWithTriggeredMigrationCheck @Inject()(
                                                        frontendAppConfig: FrontendAppConfig,
                                                        val authActions: AuthActions,
                                                        val triggeredMigrationRetrievalAction: auth.actions.TriggeredMigrationRetrievalAction,
                                                        val redirectIfNoIncomeSourcesAction: RedirectIfNoIncomeSourcesAction
                           ) extends FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  def asMTDIndividual(): ActionBuilder[MtdItUser, AnyContent] = {
    authActions.asMTDIndividual() andThen
      triggeredMigrationRetrievalAction()
  }
  
  def asMTDAgentWithConfirmedClient(): ActionBuilder[MtdItUser, AnyContent] = {
    authActions.asMTDAgentWithConfirmedClient() andThen
      triggeredMigrationRetrievalAction()
  }
  
  def asMTDIndividualOrAgentWithClient(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDAgentWithConfirmedClient()
    } else {
      asMTDIndividual()
    }
  }

  def asMTDIndividualWithIncomeSources(): ActionBuilder[MtdItUser, AnyContent] =
    asMTDIndividual() andThen redirectIfNoIncomeSourcesAction

  def asMTDAgentWithConfirmedClientWithIncomeSources(): ActionBuilder[MtdItUser, AnyContent] =
    asMTDAgentWithConfirmedClient() andThen redirectIfNoIncomeSourcesAction
}

