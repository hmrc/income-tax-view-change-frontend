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
import enums.MTDUserRole
import play.api.mvc._

import javax.inject.{Inject, Singleton}

@Singleton
class AuthActions @Inject()(val checkSessionTimeout: SessionTimeoutAction,
                            val authoriseAndRetrieve: AuthoriseAndRetrieve,
                            val authoriseAndRetrieveIndividual: AuthoriseAndRetrieveIndividual,
                            val authoriseAndRetrieveAgent: AuthoriseAndRetrieveAgent,
                            val authoriseAndRetrieveMtdAgent: AuthoriseAndRetrieveMtdAgent,
                            val agentHasClientDetails: AgentHasClientDetails,
                            val agentHasConfirmedClientAction: AgentHasConfirmedClientAction,
                            val agentIsPrimaryAction: AgentIsPrimaryAction,
                            val asMtdUser: AsMtdUser,
                            val retrieveBtaNavBar: NavBarRetrievalAction,
                            val retrieveNinoWithIncomeSources: IncomeSourceRetrievalAction,
                            val retrieveClientData: RetrieveClientData,
                            val retrieveFeatureSwitches: FeatureSwitchRetrievalAction) {

  def asMTDIndividual: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      authoriseAndRetrieveIndividual andThen
      retrieveNinoWithIncomeSources andThen
      retrieveFeatureSwitches andThen
      retrieveBtaNavBar
  }

  def asAgent(arnRequired: Boolean = true): ActionBuilder[AgentUser, AnyContent] = checkSessionTimeout andThen authoriseAndRetrieveAgent.authorise(arnRequired)

  def asIndividualOrAgent(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) asMTDAgentWithConfirmedClient
    else asMTDIndividual
  }

  def asMTDAgentWithConfirmedClient: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      retrieveClientData andThen
      authoriseAndRetrieveMtdAgent andThen
      agentHasClientDetails andThen
      agentHasConfirmedClientAction andThen
      retrieveNinoWithIncomeSources andThen
      retrieveFeatureSwitches
  }

  def asMTDAgentWithUnconfirmedClient: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      retrieveClientData andThen
      authoriseAndRetrieveMtdAgent andThen
      retrieveNinoWithIncomeSources andThen
      retrieveFeatureSwitches
  }

  def asMTDPrimaryAgent: ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen
      retrieveClientData andThen
      authoriseAndRetrieveMtdAgent andThen
      agentHasConfirmedClientAction andThen
      agentIsPrimaryAction andThen
      retrieveNinoWithIncomeSources andThen
      retrieveFeatureSwitches
  }

  def asMDTIndividualOrAgentWithClient(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if(isAgent) {
      asMTDAgentWithConfirmedClient
    } else {
      asMTDIndividual
    }
  }

  def asMDTIndividualOrPrimaryAgentWithClient(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    if(isAgent) {
      asMTDPrimaryAgent
    } else {
      asMTDIndividual
    }
  }
}

