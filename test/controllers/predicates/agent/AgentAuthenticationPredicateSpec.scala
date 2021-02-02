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
import controllers.predicates.AuthPredicate.AuthPredicateSuccess
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate._
import org.scalatest.EitherValues
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http.SessionKeys.{authToken, lastRequestTimestamp}


class AgentAuthenticationPredicateSpec extends TestSupport with MockitoSugar with ScalaFutures with EitherValues {

  private def testUser(affinityGroup: Option[AffinityGroup],
                       confidenceLevel: ConfidenceLevel,
                       enrolments: Enrolment*): IncomeTaxAgentUser = IncomeTaxAgentUser(
    enrolments = Enrolments(enrolments.toSet),
    affinityGroup = affinityGroup,
    confidenceLevel: ConfidenceLevel,
  )

  private def testUser(affinityGroup: Option[AffinityGroup], enrolments: Enrolment*): IncomeTaxAgentUser =
    testUser(affinityGroup, testConfidenceLevel, enrolments: _*)

  val blankUser: IncomeTaxAgentUser = testUser(None, confidenceLevel = ConfidenceLevel.L0)

  val userWithArnIdEnrolment: IncomeTaxAgentUser = testUser(None, arnEnrolment)

  lazy val authorisedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    authToken -> "",
    lastRequestTimestamp -> ""
  )

  "arnPredicate" should {
    "return an AuthPredicateSuccess where an arn enrolment already exists" in {
      arnPredicate(FakeRequest())(userWithArnIdEnrolment).right.value mustBe AuthPredicateSuccess
    }

    "return a NotFoundException where an arn enrolment does not already exist" in {
      intercept[NotFoundException](await(arnPredicate(FakeRequest())(blankUser).left.value))
    }
  }

  "timeoutPredicate" should {
    "return an AuthPredicateSuccess where the lastRequestTimestamp is not set" in {
      timeoutPredicate(FakeRequest())(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return an AuthPredicateSuccess where the authToken is set and hte lastRequestTimestamp is set" in {
      timeoutPredicate(authorisedRequest)(blankUser).right.value mustBe AuthPredicateSuccess
    }

    "return the timeout page where the lastRequestTimestamp is set but the auth token is not" in {
      lazy val request = FakeRequest().withSession(lastRequestTimestamp -> "")
      await(timeoutPredicate(request)(blankUser).left.value) mustBe timeoutRoute
    }
  }
}
