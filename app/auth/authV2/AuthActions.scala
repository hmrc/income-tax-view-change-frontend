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

import _root_.models.admin.YouMustWaitToSignUpPageEnabled
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
                             val retrieveFeatureSwitches: FeatureSwitchRetrievalAction
                           ) extends FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  // Note: Auth feature switching will be config driven and feature switch page only, not synced with mongo

  def asMTDIndividual: ActionBuilder[MtdItUser, AnyContent] = {
    if (isEnabledFromConfig(YouMustWaitToSignUpPageEnabled)) {
      checkSessionTimeout andThen
        authoriseAndRetrieveIndividual andThen
        incomeSourceRetrievalAction andThen
        itsaStatusRetrievalAction andThen
        retrieveFeatureSwitches andThen
        retrieveNavBar
    } else {
      checkSessionTimeout andThen
        authoriseAndRetrieveIndividual andThen
        incomeSourceRetrievalAction andThen
        retrieveFeatureSwitches andThen
        retrieveNavBar
    }
  }

  def asAgent(arnRequired: Boolean = true): ActionBuilder[AuthorisedUserRequest, AnyContent] =
    checkSessionTimeout andThen authoriseAndRetrieveAgent.authorise(arnRequired)

  def asMTDAgentWithConfirmedClient: ActionBuilder[MtdItUser, AnyContent] = {
    if (isEnabledFromConfig(YouMustWaitToSignUpPageEnabled)) {
      checkSessionTimeout andThen
        authoriseAndRetrieveAgent.authorise() andThen
        retrieveClientData.authorise() andThen
        authoriseAndRetrieveMtdAgent andThen
        agentHasConfirmedClientAction andThen
        incomeSourceRetrievalAction andThen
        itsaStatusRetrievalAction andThen
        retrieveFeatureSwitches
    } else {
      checkSessionTimeout andThen
        authoriseAndRetrieveAgent.authorise() andThen
        retrieveClientData.authorise() andThen
        authoriseAndRetrieveMtdAgent andThen
        agentHasConfirmedClientAction andThen
        incomeSourceRetrievalAction andThen
        retrieveFeatureSwitches
    }
  }

  def asMTDAgentWithUnconfirmedClient: ActionBuilder[MtdItUser, AnyContent] = {
    if (isEnabledFromConfig(YouMustWaitToSignUpPageEnabled)) {
      checkSessionTimeout andThen
        authoriseAndRetrieveAgent.authorise() andThen
        retrieveClientData.authorise(useCookies = true) andThen
        authoriseAndRetrieveMtdAgent andThen
        incomeSourceRetrievalAction andThen
        itsaStatusRetrievalAction andThen
        retrieveFeatureSwitches
    } else {
      checkSessionTimeout andThen
        authoriseAndRetrieveAgent.authorise() andThen
        retrieveClientData.authorise(useCookies = true) andThen
        authoriseAndRetrieveMtdAgent andThen
        incomeSourceRetrievalAction andThen
        retrieveFeatureSwitches
    }
  }

  def asMTDPrimaryAgent: ActionBuilder[MtdItUser, AnyContent] = {
    if (isEnabledFromConfig(YouMustWaitToSignUpPageEnabled)) {
      checkSessionTimeout andThen
        authoriseAndRetrieveAgent.authorise() andThen
        retrieveClientData.authorise() andThen
        authoriseAndRetrieveMtdAgent andThen
        agentIsPrimaryAction andThen
        incomeSourceRetrievalAction andThen
        itsaStatusRetrievalAction andThen
        retrieveFeatureSwitches
    } else {
      checkSessionTimeout andThen
        authoriseAndRetrieveAgent.authorise() andThen
        retrieveClientData.authorise() andThen
        authoriseAndRetrieveMtdAgent andThen
        agentIsPrimaryAction andThen
        incomeSourceRetrievalAction andThen
        retrieveFeatureSwitches
    }
  }

  def asMTDIndividualOrAgentWithClient(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDAgentWithConfirmedClient
    } else {
      asMTDIndividual
    }
  }

  def asMTDIndividualOrPrimaryAgentWithClient(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      asMTDPrimaryAgent
    } else {
      asMTDIndividual
    }
  }

  def asAuthorisedUser: ActionBuilder[AuthorisedUserRequest, AnyContent] = {
    checkSessionTimeout andThen authoriseAndRetrieve
  }
}

