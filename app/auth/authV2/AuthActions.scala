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
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.predicates._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuthActions @Inject()(val checkSessionTimeout: SessionTimeoutPredicateV2,
                            val authoriseAndRetrieve: AuthoriseAndRetrieve,
                            val agentHasClientDetails: AgentHasClientDetails,
                            val asMtdUser: AsMtdUser,
                            val retrieveBtaNavBar: NavBarPredicate,
                            val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                            val featureSwitchPredicate: FeatureSwitchPredicateV2)
                           (implicit val appConfig: FrontendAppConfig,
                                     val ec: ExecutionContext)  extends  FeatureSwitching {

  def authAction[A](): ActionBuilder[MtdItUser, AnyContent] = {

      (   checkSessionTimeout andThen

          authoriseAndRetrieve andThen

          agentHasClientDetails andThen

          // get MtdItUser from EnroledUser by requiring mtdId

          asMtdUser andThen

          // are income sources required on most pages? could this step
          // be removed from here and added where required?

          retrieveNinoWithIncomeSources andThen

          featureSwitchPredicate andThen

          retrieveBtaNavBar )
  }

}

