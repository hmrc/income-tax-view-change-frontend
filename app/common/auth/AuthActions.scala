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

package common.auth

import common.auth.actions.*
import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
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
                             val agentIsPrimaryAction: AgentIsPrimaryAction,
                             val retrieveNavBar: NavBarRetrievalAction,
                             val incomeSourceRetrievalAction: IncomeSourceRetrievalAction,
                             val retrieveClientData: RetrieveClientData,
                             val retrieveFeatureSwitches: FeatureSwitchRetrievalAction,
                             val authoriseAndRetrieveIndividualForNrs: AuthoriseAndRetrieveIndividualForNrs,
                             val authoriseAndRetrieveAgentForNrs: AuthoriseAndRetrieveAgentForNrs,
                             val redirectIfNoIncomeSourcesAction: RedirectIfNoIncomeSourcesAction,
                             val triggeredMigrationRetrievalAction: TriggeredMigrationRetrievalAction
                           ) extends FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  def asMTDIndividual(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveIndividual andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen
      retrieveNavBar andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage)
  }

  def asMTDIndividualForNrs: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveIndividualForNrs andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen
      retrieveNavBar
  }

  def asAgent(arnRequired: Boolean = true): ActionBuilder[AuthorisedUserRequest, AnyContent] =
    checkSessionTimeout andThen authoriseAndRetrieveAgent.authorise(arnRequired)

  def asMTDAgentWithConfirmedClient(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentHasConfirmedClientAction andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage)
  }

  def asMTDAgentWithUnconfirmedClient: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise() andThen
      retrieveClientData.authorise(useCookies = true) andThen
      authoriseAndRetrieveMtdAgent andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches
  }

  def asMTDPrimaryAgent(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveAgent.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentIsPrimaryAction andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage)
  }
  
  def asMTDPrimaryAgentForNrs: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveAgentForNrs.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentIsPrimaryAction andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches
  }

  def asMTDIndividualWithIncomeSources(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] =
    asMTDIndividual(isTriggeredMigrationPage) andThen redirectIfNoIncomeSourcesAction

  def asMTDAgentWithConfirmedClientWithIncomeSources(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] =
    asMTDAgentWithConfirmedClient(isTriggeredMigrationPage) andThen redirectIfNoIncomeSourcesAction

  def asMTDPrimaryAgentWithIncomeSources(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] =
    asMTDPrimaryAgent(isTriggeredMigrationPage) andThen redirectIfNoIncomeSourcesAction

  def asMTDIndividualForNoIncomeSourcesPage: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveIndividual andThen
      incomeSourceRetrievalAction andThen
      retrieveNavBar
  }

  def asMTDAgentWithConfirmedClientForNoIncomeSourcesPage: ActionBuilder[MtdItUser, AnyContent] = {
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

  def asMTDIndividualOrPrimaryAgentWithClient(isAgent: Boolean, triggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDPrimaryAgent(triggeredMigrationPage)
    } else {
      asMTDIndividual(triggeredMigrationPage)
    }
  }

  def asMTDIndividualOrPrimaryAgentWithClientForNrs(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDPrimaryAgentForNrs
    } else {
      asMTDIndividualForNrs
    }
  }

  def asAuthorisedUser: ActionBuilder[AuthorisedUserRequest, AnyContent] = {
    checkSessionTimeout andThen authoriseAndRetrieve
  }
}

