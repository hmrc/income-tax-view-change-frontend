/*
 * Copyright 2024 HM Revenue & Customs
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

package authV2

import auth.authV2.AgentUser
import auth.authV2.actions.ClientDataRequest
import auth.{MtdItUser, MtdItUserOptionNino}
import controllers.agent.sessionUtils.SessionKeys
import models.admin.FeatureSwitch
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import models.sessionData.SessionCookieData
import models.sessionData.SessionDataGetResponse.{SessionDataGetSuccess, SessionDataNotFound}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}

object AuthActionsTestData {

  val nino              = "AA111111A"
  val saUtr             = "123456789"
  val mtdId             = "abcde"
  val arn               = "1"
  val firstName = "Issac"
  val lastName = "Newton"
  val userName = Name(Some("Issac"), Some("Newton"))
  val clientName = Name(Some("Test"), Some("Client"))

  val mtdEnrolment              = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", mtdId)), "Activated", None)
  val agentEnrolment            = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "1")), "Activated", None)
  val ninoEnrolment             = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", nino)), "Activated", None)
  val saEnrolment               = Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", saUtr)), "Activated", None)
  val credentials               = Credentials("foo", "bar")
  val defaultIncomeSourcesData  = IncomeSourceDetailsModel(nino, saUtr, Some("2012"), Nil, Nil)
  val invalidIncomeSourceData  = IncomeSourceDetailsError(500, "mongo error")

  val acceptedConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L250
  val notAcceptedConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L50

  def mtdIdAgentPredicate(mtdId: String): Enrolment = Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId).withDelegatedAuthRule("mtd-it-auth")
  def mtdIdSupportingAgentPredicate(mtdId: String) = Enrolment("HMRC-MTD-IT-SUPP").withIdentifier("MTDITID", mtdId).withDelegatedAuthRule("mtd-it-auth-supp")
  def mtdIdIndividualPredicate(mtdId: String) = Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId)


  def getAllEnrolmentsIndividual(hasNino: Boolean, hasSA: Boolean) = {
    var enrolmentList = List(mtdEnrolment)
    if(hasNino) enrolmentList :+= ninoEnrolment
    if(hasSA) enrolmentList :+= saEnrolment
    Enrolments(
      enrolmentList.toSet,
    )
  }

  def getAllEnrolmentsAgent(
                             hasNino: Boolean,
                             hasSA: Boolean,
                             hasDelegatedEnrolment: Boolean = false,
                             delegatedPredicate: => Enrolment = mtdIdAgentPredicate(mtdId)): Enrolments = {
    var enrolmentList = if(!hasDelegatedEnrolment) List(agentEnrolment) else List(agentEnrolment, delegatedPredicate)
    if(hasNino) enrolmentList :+= ninoEnrolment
    if(hasSA) enrolmentList :+= saEnrolment
    Enrolments(
      enrolmentList.toSet,
    )
  }

  def getMtdItUserOptionNinoForAuthorise(affinityGroup: Option[AffinityGroup],
                                         hasNino: Boolean = false,
                                         hasSA: Boolean = false,
                                         hasUserName: Boolean = false,
                                         clientConfirmed: Boolean = false,
                                         isSupportingAgent: Boolean = false)
                                        (implicit request: Request[_]): MtdItUserOptionNino[_] = MtdItUserOptionNino(
    mtdId,
    if(hasNino) Some(nino) else None,
    if(hasUserName) Some(userName) else None,
    None,
    if(hasSA) Some(saUtr) else None,
    Some(credentials.providerId),
    affinityGroup,
    Some(arn),
    None,
    isSupportingAgent,
    clientConfirmed = clientConfirmed
  )

  def getMtdItUserOptionNinoForAuthoriseMtdAgent(affinityGroup: Option[AffinityGroup],
                                                 cdr: ClientDataRequest[_],
                                                 isSupportingAgent: Boolean = false)
                                                (implicit request: Request[_]): MtdItUserOptionNino[_] = MtdItUserOptionNino(
    mtdId,
    Some(nino),
    Some(userName),
    None,
    Some(saUtr),
    Some(credentials.providerId),
    affinityGroup,
    Some(arn),
    Some(clientName),
    isSupportingAgent
  )

  def getMtdItUser(affinityGroup: AffinityGroup,
                   featureSwitches: List[FeatureSwitch] = List.empty,
                   isSupportingAgent: Boolean = false,
                   btaNavBar: Option[Html] = None)
                  (implicit request: Request[_]) = MtdItUser(
    mtdId,
    nino,
    Some(userName),
    defaultIncomeSourcesData,
    btaNavBar,
    Some(saUtr),
    Some(credentials.providerId),
    Some(affinityGroup),
    None,
    featureSwitches = featureSwitches,
    isSupportingAgent = isSupportingAgent
  )(request)

  def getAgentData(enrolments: Enrolments)(implicit request: Request[_]) = AgentUser(
    enrolments,
    Some(AffinityGroup.Agent),
    acceptedConfidenceLevel,
    Some(credentials)
  )(request)

  def getSessionCookieData(isSupportingAgent: Boolean, confirmed: Boolean) = {
    val cookieData = SessionCookieData(
      mtdId, nino, saUtr, Some(firstName), Some(lastName), isSupportingAgent
    ).toSessionCookieSeq
    if(confirmed) cookieData ++ Seq(SessionKeys.confirmedClient -> "true") else cookieData
  }

  val sessionGetSuccessResponse = SessionDataGetSuccess(
    mtdId, nino, saUtr, "sessionId"
  )
  val sessionGetNotFoundResponse = SessionDataNotFound("Not found")


}
