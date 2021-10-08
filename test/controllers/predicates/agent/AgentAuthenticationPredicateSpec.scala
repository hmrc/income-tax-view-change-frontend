/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.predicates.agent

import assets.BaseTestConstants._
import controllers.agent.utils.SessionKeys.{clientFirstName, clientLastName, clientUTR, confirmedClient}
import controllers.predicates.AuthPredicate.AuthPredicateSuccess
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate._
import org.scalatest.EitherValues
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.SessionKeys.{authToken, lastRequestTimestamp}

import scala.concurrent.Future

class AgentAuthenticationPredicateSpec extends TestSupport with MockitoSugar with ScalaFutures with EitherValues {

  private def testUser(affinityGroup: Option[AffinityGroup],
                       confidenceLevel: ConfidenceLevel,
                       credentials: Option[Credentials],
                       enrolments: Enrolment*): IncomeTaxAgentUser = IncomeTaxAgentUser(
    enrolments = Enrolments(enrolments.toSet),
    affinityGroup = affinityGroup,
    confidenceLevel: ConfidenceLevel,
    credentials = credentials
  )

  private def testUser(affinityGroup: Option[AffinityGroup], enrolments: Enrolment*): IncomeTaxAgentUser =
    testUser(affinityGroup, testConfidenceLevel, testCredentials, enrolments: _*)

  val blankUser: IncomeTaxAgentUser = testUser(None, confidenceLevel = ConfidenceLevel.L50, credentials = None)

  val userWithArnIdEnrolment: IncomeTaxAgentUser = testUser(None, arnEnrolment)

  lazy val authorisedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    authToken -> "",
    lastRequestTimestamp -> ""
  )

  lazy val clientDetailsPopulated: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    clientFirstName -> "",
    clientLastName -> "",
    clientUTR -> ""
  )

  lazy val confirmedClientPopulated: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    confirmedClient -> ""
  )

  "arnPredicate" should {
    "return an AuthPredicateSuccess where an arn enrolment already exists" in {
      arnPredicate().apply(FakeRequest())(userWithArnIdEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "return a MissingAgentReferenceNumber where a user does not have it in their enrolments" in {
      val result = arnPredicate().apply(FakeRequest())(blankUser).left.value.failed.futureValue
      result shouldBe an[MissingAgentReferenceNumber]
    }

    "return a custom result when a user does not have AgentReferenceNumber in their enrolments" in {
      val customResult = mock[Future[Result]]
      arnPredicate(customResult).apply(FakeRequest())(blankUser).left.value mustBe customResult
    }
  }

  "timeoutPredicate" should {
    "return an AuthPredicateSuccess where the lastRequestTimestamp is not set" in {
      timeoutPredicate(FakeRequest())(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return an AuthPredicateSuccess where the authToken is set and the lastRequestTimestamp is set" in {
      timeoutPredicate(authorisedRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the timeout page where the lastRequestTimestamp is set but the auth token is not" in {
      lazy val request = FakeRequest().withSession(lastRequestTimestamp -> "")
      timeoutPredicate(request)(blankUser).left.value.futureValue mustBe timeoutRoute
    }
  }

  "detailsPredicate" should {
    "return an AuthPredicateSuccess where the client's details are in session" in {
      detailsPredicate(clientDetailsPopulated)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the Enter Client UTR page where the client's details are not in session" in {
      detailsPredicate(FakeRequest())(blankUser).left.value.futureValue mustBe noClientDetailsRoute
    }
  }

  "selectedClientPredicate" should {
    "return an AuthPredicateSuccess where the confirmedClient key is in session" in {
      selectedClientPredicate(confirmedClientPopulated)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return an Enter Client UTR page where the confirmedClient key is not in session" in {
      selectedClientPredicate(FakeRequest())(blankUser).left.value.futureValue mustBe noClientDetailsRoute
    }
  }
}
