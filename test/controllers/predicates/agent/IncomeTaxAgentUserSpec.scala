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

import controllers.predicates.IncomeTaxAgentUser
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IncomeTaxAgentUserSpec extends UnitSpec with WithFakeApplication {

  val testArn = "123456"

  val testCredId: Option[String] = Some("credId")

  "IncomeTaxSAUser" should {
    val confidenceLevel = ConfidenceLevel.L50

    lazy val user = IncomeTaxAgentUser(
      Enrolments(Set(
        Enrolment(Constants.agentServiceEnrolmentName,
          Seq(EnrolmentIdentifier(Constants.agentServiceIdentifierKey, testArn)),
          "Activated"
        )
      )),
      None,
      confidenceLevel,
      credId = testCredId
    )

    s"have the expected ARN '${testArn}'" in {
      user.agentReferenceNumber shouldBe Some(testArn)
    }

    s"have the expected credId '$testCredId" in {
      user.credId shouldBe testCredId
    }

  }
}
