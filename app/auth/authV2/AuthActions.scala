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

package auth.authV2

import auth.MtdItUser
import auth.authV2.actions._
import auth.authV2.models.AuthorisedUserRequest
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import play.api.mvc._

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
                             val itsaStatusRetrievalAction: ItsaStatusRetrievalAction,
                             val retrieveClientData: RetrieveClientData,
                             val retrieveFeatureSwitches: FeatureSwitchRetrievalAction,
                             val authoriseAndRetrieveIndividualForNrs: AuthoriseAndRetrieveIndividualForNrs,
                             val authoriseAndRetrieveAgentForNrs: AuthoriseAndRetrieveAgentForNrs,
                             val triggeredMigrationRetrievalAction: TriggeredMigrationRetrievalAction
                           ) extends FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  def asMTDIndividual(isTriggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveIndividual andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen  // order of feature switch action prior to enable feature switching in itsaStatusRetrievalAction
      itsaStatusRetrievalAction andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage) andThen
      retrieveNavBar
  }

  def asMTDIndividualForNrs: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveIndividualForNrs andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen  // order of feature switch action prior to enable feature switching in itsaStatusRetrievalAction
      itsaStatusRetrievalAction andThen
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
      itsaStatusRetrievalAction andThen
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
      itsaStatusRetrievalAction andThen
      triggeredMigrationRetrievalAction(isTriggeredMigrationPage)
  }
  
  def asMTDPrimaryAgentForNrs: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveAgentForNrs.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentIsPrimaryAction andThen
      incomeSourceRetrievalAction andThen
      retrieveFeatureSwitches andThen
      itsaStatusRetrievalAction
  }

  def asMTDIndividualOrAgentWithClient(isAgent: Boolean, triggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDAgentWithConfirmedClient(triggeredMigrationPage)
    } else {
      asMTDIndividual(triggeredMigrationPage)
    }
  }

  def asMTDIndividualOrPrimaryAgentWithClient(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDPrimaryAgent()
    } else {
      asMTDIndividual()
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

