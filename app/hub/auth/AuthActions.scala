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

import common.auth.actions.{AgentHasConfirmedClientAction, FeatureSwitchRetrievalAction, IncomeSourceRetrievalAction, NavBarRetrievalAction}
import common.auth.actions.{RedirectIfNoIncomeSourcesAction, RetrieveClientData, TriggeredMigrationRetrievalAction}
import common.auth.{AuthorisedUserRequest, MtdItUser, RequestWithFeatureSwitches}
import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import hub.auth.actions.*
import play.api.mvc.*

import javax.inject.{Inject, Singleton}

@Singleton
class AuthActions @Inject()(
                             frontendAppConfig: FrontendAppConfig,
                             val checkSessionTimeout: SessionTimeoutAction,
                             val authoriseAndRetrieve: AuthoriseAndRetrieve,
                             val authoriseAndRetrieveIndividual: AuthoriseAndRetrieveIndividual,
                             val authoriseAndRetrieveAgent: AuthoriseAndRetrieveAgent,
                             val authoriseAndRetrieveMtdAgent: AuthoriseAndRetrieveMtdAgent,
                             val agentHasConfirmedClientAction: AgentHasConfirmedClientAction,
                             val retrieveNavBar: NavBarRetrievalAction,
                             val incomeSourceRetrievalAction: IncomeSourceRetrievalAction,
                             val retrieveClientData: RetrieveClientData,
                             val retrieveFeatureSwitches: FeatureSwitchRetrievalAction,
                             val redirectIfNoIncomeSourcesAction: RedirectIfNoIncomeSourcesAction,
                             val triggeredMigrationRetrievalAction: TriggeredMigrationRetrievalAction,
                             val correctHubContextRootAction: CorrectHubContextRootAction
                           ) extends FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  def asMTDIndividual(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    retrieveFeatureSwitches andThen
      correctHubContextRootAction andThen
      checkSessionTimeout andThen
      authoriseAndRetrieveIndividual andThen
      incomeSourceRetrievalAction andThen
      retrieveNavBar andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage)
  }

  def asAgent(arnRequired: Boolean = true, checkContextRoot: Boolean = true): ActionBuilder[AuthorisedUserRequest, AnyContent] =
    retrieveFeatureSwitchesAndCheckContextRootIfReq(checkContextRoot) andThen
      checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise(arnRequired)

  def asMTDAgentWithConfirmedClient(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    retrieveFeatureSwitches andThen
      correctHubContextRootAction andThen
      checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentHasConfirmedClientAction andThen
      incomeSourceRetrievalAction andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage)
  }

  def asMTDAgentWithUnconfirmedClient(checkContextRoot: Boolean = true): ActionBuilder[MtdItUser, AnyContent] = {
    retrieveFeatureSwitchesAndCheckContextRootIfReq(checkContextRoot) andThen
      checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise() andThen
      retrieveClientData.authorise(useCookies = true) andThen
      authoriseAndRetrieveMtdAgent andThen
      incomeSourceRetrievalAction
  }

  def asMTDIndividualWithIncomeSources(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] =
    asMTDIndividual(isTriggeredMigrationPage) andThen redirectIfNoIncomeSourcesAction

  def asMTDAgentWithConfirmedClientWithIncomeSources(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] =
    asMTDAgentWithConfirmedClient(isTriggeredMigrationPage) andThen redirectIfNoIncomeSourcesAction

  def asMTDIndividualForNoIncomeSourcesPage: ActionBuilder[MtdItUser, AnyContent] = {
    retrieveFeatureSwitches andThen
      checkSessionTimeout andThen
      authoriseAndRetrieveIndividual andThen
      incomeSourceRetrievalAction andThen
      retrieveNavBar
  }

  def asMTDAgentWithConfirmedClientForNoIncomeSourcesPage: ActionBuilder[MtdItUser, AnyContent] = {
    retrieveFeatureSwitches andThen
      checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentHasConfirmedClientAction andThen
      incomeSourceRetrievalAction
  }

  def asMTDIndividualOrAgentWithClient(isAgent: Boolean, triggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDAgentWithConfirmedClient(triggeredMigrationPage)
    } else {
      asMTDIndividual(triggeredMigrationPage)
    }
  }

  def asAuthorisedUser(checkContextRoot: Boolean = true): ActionBuilder[AuthorisedUserRequest, AnyContent] = {
    retrieveFeatureSwitchesAndCheckContextRootIfReq(checkContextRoot) andThen
    checkSessionTimeout andThen authoriseAndRetrieve
  }
  
  def retrieveFeatureSwitchesAndCheckContextRootIfReq(checkContextRoot: Boolean = true): ActionBuilder[RequestWithFeatureSwitches, AnyContent] = {
    if (checkContextRoot) {
      retrieveFeatureSwitches andThen correctHubContextRootAction
    } else {
      retrieveFeatureSwitches
    }
  }
}

