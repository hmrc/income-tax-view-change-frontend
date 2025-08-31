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

import auth.MtdItUser
import auth.authV2.models._
import controllers.agent.AuthUtils._
import controllers.agent.sessionUtils.SessionKeys
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import forms.IncomeSourcesFormsSpec.fakeRequestWithClientDetails
import models.admin.FeatureSwitch
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import models.sessionData.SessionCookieData
import models.sessionData.SessionDataGetResponse.{SessionDataGetSuccess, SessionDataNotFound}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.Html
import testConstants.BaseTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Credentials

object AuthActionsTestData {

  val mtdEnrolment              = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "Activated", None)
  val agentEnrolment            = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", testArn)), "Activated", None)
  val ninoEnrolment             = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "Activated", None)
  val saEnrolment               = Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", testSaUtr)), "Activated", None)
  val testCredentials               = Credentials(testCredId, "bar")
  val defaultIncomeSourcesData  = IncomeSourceDetailsModel(testNino, testSaUtr, Some("2012"), Nil, Nil)
  val invalidIncomeSourceData  = IncomeSourceDetailsError(500, "mongo error")

  val acceptedConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L250
  val notAcceptedConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L50

  def primaryAgentPredicate(mtdItId: String = testMtditid): Predicate = Enrolment(mtdEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
    .withDelegatedAuthRule(primaryAgentAuthRule)
  def secondaryAgentPredicate(mtdItId: String = testMtditid): Predicate = Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
    .withDelegatedAuthRule(secondaryAgentAuthRule)

  def delegatedEnrolmentPredicate(mtdItId: String,
                                  isSupportingAgent: Boolean): Predicate = if(isSupportingAgent) {
    secondaryAgentPredicate(mtdItId)
  } else {
    primaryAgentPredicate(mtdItId)
  }

  def getAllEnrolmentsIndividual(hasNino: Boolean, hasSA: Boolean) = {
    var enrolmentList = List(mtdEnrolment)
    if(hasNino) enrolmentList :+= ninoEnrolment
    if(hasSA) enrolmentList :+= saEnrolment
    Enrolments(
      enrolmentList.toSet,
    )
  }

  def getAllEnrolmentsAgent(hasNino: Boolean,
                             hasSA: Boolean): Enrolments = {
    val optNinoEnrolment = if(hasNino) Some(ninoEnrolment) else None
    val optSaEnrolment = if(hasSA) Some(saEnrolment) else None
    Enrolments(
      List(Some(agentEnrolment), optNinoEnrolment, optSaEnrolment).flatten.toSet,
    )
  }

  def getAuthUserDetails(affinityGroup: Option[AffinityGroup],
                         enrolments: Enrolments,
                         hasUserName: Boolean,
                         hasCredentials: Boolean = true): AuthUserDetails = AuthUserDetails(
   enrolments,
    affinityGroup,
    if(hasCredentials) Some(testCredentials) else None,
    if(hasUserName) Some(testRetrievedUserName) else None,
    ConfidenceLevel.L250
  )

  def getAgentClientDetails(isConfirmed: Boolean, hasName: Boolean = true): AgentClientDetails = AgentClientDetails(
    testMtditid,
    if(hasName) Some(testFirstName) else None,
    if(hasName) Some(testSecondName) else None,
    testNino,
    testSaUtr,
    isConfirmed
  )

  lazy val defaultAuthUserDetails: MTDUserRole => AuthUserDetails = mtdUserRole => {
    val (affinityGroup, enrolments) = mtdUserRole match {
      case MTDIndividual => (Individual, getAllEnrolmentsIndividual(true, true))
      case _ => (Agent, getAllEnrolmentsAgent(true, true))
    }
    getAuthUserDetails(Some(affinityGroup), enrolments, true)
  }

  lazy val defaultAuthorisedRequest: (MTDUserRole, Request[_]) => AuthorisedUserRequest[_] = {
    (mtdUserRole, request) =>
      AuthorisedUserRequest(defaultAuthUserDetails(mtdUserRole))(request)
  }

  lazy val defaultAuthorisedAndEnrolledRequest: (MTDUserRole, Request[_]) => AuthorisedAndEnrolledRequest[_] = {
    (mtdUserRole, request) =>
      val optClientDetails = if(mtdUserRole == MTDIndividual) None else Some(getAgentClientDetails(true))
      AuthorisedAndEnrolledRequest(testMtditid, mtdUserRole, defaultAuthUserDetails(mtdUserRole), optClientDetails)(request)
  }

  lazy val defaultAuthorisedWithClientDetailsRequest: AuthorisedAgentWithClientDetailsRequest[_] = {
      AuthorisedAgentWithClientDetailsRequest(defaultAuthUserDetails(MTDPrimaryAgent), getAgentClientDetails(true))(fakeRequestWithClientDetails)
  }

  def getMtdItUser(affinityGroup: AffinityGroup,
                   featureSwitches: List[FeatureSwitch] = List.empty,
                   btaNavBar: Option[Html] = None,
                   isSupportingAgent: Boolean = false,
                   incomeSources: IncomeSourceDetailsModel = defaultIncomeSourcesData)
                  (implicit request: Request[_]) = {
    val (mtdUserRole, enrolments, optClientDetails) = affinityGroup match {
      case Agent =>
        (if(isSupportingAgent) MTDSupportingAgent else MTDPrimaryAgent, getAllEnrolmentsAgent(true, true), Some(getAgentClientDetails(true)))
      case _ => (MTDIndividual, getAllEnrolmentsIndividual(true, true), None)
    }
    MtdItUser(
      testMtditid,
      testNino,
      mtdUserRole,
      getAuthUserDetails(Some(affinityGroup), enrolments, true),
      optClientDetails,
      incomeSources,
      btaNavBar,
      featureSwitches = featureSwitches
    )(request)
  }

  def defaultMTDITUser(af: Option[AffinityGroup],
                       incomeSources: IncomeSourceDetailsModel,
                       request: Request[_] = FakeRequest(),
                       isSupportingAgent: Boolean = false): MtdItUser[_] = {
    getMtdItUser(af.getOrElse(Individual), isSupportingAgent = isSupportingAgent, incomeSources = incomeSources)(request)
  }

  def getMinimalMTDITUser(af: Option[AffinityGroup],
                          incomeSources: IncomeSourceDetailsModel,
                          isSupportingAgent: Boolean = false,
                          request: Request[_] = FakeRequest()): MtdItUser[_] = {
    val (mtdUserRole, enrolments, optClientDetails) = af match {
      case Some(Agent) =>
        (if(isSupportingAgent) MTDSupportingAgent else MTDPrimaryAgent, getAllEnrolmentsAgent(false, false), Some(getAgentClientDetails(true, false)))
      case _ => (MTDIndividual, getAllEnrolmentsIndividual(false, false), None)
    }
    MtdItUser(
      testMtditid,
      testNino,
      mtdUserRole,
      getAuthUserDetails(af, enrolments, true, false),
      optClientDetails,
      incomeSources
    )(request)
  }

  def getAuthorisedData(enrolments: Enrolments)(implicit request: Request[_]) = AuthorisedUserRequest(
    AuthUserDetails(
      enrolments,
      Some(AffinityGroup.Agent),
      Some(testCredentials),
      None,
      ConfidenceLevel.L250
    )
  )(request)

  def getSessionCookieData(isSupportingAgent: Boolean, confirmed: Boolean) = {
    val cookieData = SessionCookieData(
      testMtditid, testNino, testSaUtr, Some(testFirstName), Some(testSecondName)).toSessionCookieSeq
    if(confirmed) cookieData ++ Seq(SessionKeys.confirmedClient -> "true") else cookieData
  }

  val sessionGetSuccessResponse = SessionDataGetSuccess(
    testMtditid, testNino, testSaUtr, "sessionId"
  )
  val sessionGetNotFoundResponse = SessionDataNotFound("Not found")

  def getMTDRole(affinityGroup: Option[AffinityGroup], isSupportingAgent: Boolean): MTDUserRole = {
    affinityGroup match {
      case Some(Agent) if isSupportingAgent => MTDSupportingAgent
      case Some(Agent) => MTDPrimaryAgent
      case _ => MTDIndividual
    }
  }

}
