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

package auth

import auth.authV2.models.{AgentClientDetails, AuthUserDetails}
import enums.MTDUserRole
import enums.MTDUserRole.{MTDIndividual, MTDSupportingAgent}
import models.admin.FeatureSwitch
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.mvc.{Request, WrappedRequest}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Name

case class MtdItUser[A](mtditid: String,
                        nino: String,
                        usersRole: MTDUserRole,
                        authUserDetails: AuthUserDetails,
                        clientDetails: Option[AgentClientDetails],
                        incomeSources: IncomeSourceDetailsModel,
                        btaNavPartial: Option[Html] = None,
                        featureSwitches: List[FeatureSwitch] = List.empty // TODO: remove default
                       )(implicit request: Request[A]) extends WrappedRequest[A](request){

  val saUtr: Option[String] = if(clientDetails.isDefined) clientDetails.map(_.utr)
  else authUserDetails.saUtr
  val credId: Option[String] = authUserDetails.credId
  val userType: Option[AffinityGroup] = authUserDetails.affinityGroup
  val arn: Option[String] = authUserDetails.agentReferenceNumber

  def isAgent(): Boolean = usersRole != MTDIndividual
  def userName: Option[Name] = authUserDetails.name
  def optClientNameAsString: Option[String] = {
    val optClientName = clientDetails.fold[Option[Name]](None)(_.clientName)
    val firstName = optClientName.fold[Option[String]](None)(_.name)
    val lastName  = optClientName.fold[Option[String]](None)(_.lastName)
    (firstName, lastName) match {
      case (Some(fn), Some(ln)) => Some(s"$fn $ln")
      case _ => None
    }
  }

  val isSupportingAgent: Boolean = usersRole == MTDSupportingAgent

  def addFeatureSwitches(newFeatureSwitches: List[FeatureSwitch]) = copy(featureSwitches = newFeatureSwitches)

  def addNavBar(partial: Html) = copy(btaNavPartial = Some(partial))

}