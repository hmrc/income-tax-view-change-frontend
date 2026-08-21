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

package financials.auth


import javax.inject.{Inject, Singleton}
import play.api.mvc.*
import common.auth.MtdItUser
import common.auth.actions.*
import common.auth.AuthActions as CommonAuthActions

@Singleton
class AuthActions @Inject()(        
    val commonAuthActions: CommonAuthActions,
    val checkSessionTimeout: SessionTimeoutAction,
    val authoriseAndRetrieveAgentForNrs: AuthoriseAndRetrieveAgentForNrs,
    val retrieveClientData: RetrieveClientData,
    val authoriseAndRetrieveMtdAgent: AuthoriseAndRetrieveMtdAgent,
    val agentIsPrimaryAction: AgentIsPrimaryAction,
    val incomeSourceRetrievalAction: IncomeSourceRetrievalAction,
    val retrieveFeatureSwitches: FeatureSwitchRetrievalAction,
    val authoriseAndRetrieveIndividualForNrs: AuthoriseAndRetrieveIndividualForNrs,
    val retrieveNavBar: NavBarRetrievalAction,
  ) 
  {

  def asMTDIndividualOrPrimaryAgentWithClient(isAgent: Boolean, triggeredMigrationPage: Boolean = false): ActionBuilder[MtdItUser, AnyContent] = {
    if (isAgent) {
      commonAuthActions.asMTDPrimaryAgent(triggeredMigrationPage)
    } else {
      commonAuthActions.asMTDIndividual(triggeredMigrationPage)
    }
  }
  
  def asMTDIndividualOrPrimaryAgentWithClientForNrs(isAgent: Boolean): ActionBuilder[MtdItUser, AnyContent] = {
    checkSessionTimeout andThen {
    if isAgent then 
      authoriseAndRetrieveAgentForNrs.authorise() andThen
      retrieveClientData.authorise() andThen
      authoriseAndRetrieveMtdAgent andThen
      agentIsPrimaryAction 
    else 
      authoriseAndRetrieveIndividualForNrs 
    } andThen 
    incomeSourceRetrievalAction andThen {
    if isAgent then 
      retrieveFeatureSwitches 
    else 
      retrieveFeatureSwitches andThen
      retrieveNavBar
    }
  }

}
