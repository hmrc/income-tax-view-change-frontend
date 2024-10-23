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

package auth.authV2

import audit.AuditingService
import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import auth.authV2.actions._
import config.{FrontendAppConfig, FrontendAuthConnector}
import controllers.predicates.IncomeSourceDetailsPredicate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.Mockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{Application, Play}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import services.IncomeSourceDetailsService
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}

trait AuthActionsSpecHelper extends TestSupport with ScalaFutures {

  def bindingMocks: List[GuiceableModule]


  lazy val mockAuthConnector = mock[FrontendAuthConnector]
  lazy val mockIncomeSourceDetailsService = mock[IncomeSourceDetailsService]
  lazy val mockAppConfig = mock[FrontendAppConfig]
  lazy val mockAuditingService = mock[AuditingService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockIncomeSourceDetailsService)
  }

  override def afterEach(): Unit = {
    Play.stop(fakeApplication())
    super.afterEach()
  }

  val frontendAuthFunctions = new FrontendAuthorisedFunctions(mockAuthConnector)

  override def fakeApplication(): Application = {

    new GuiceApplicationBuilder()
      .overrides(
        bindingMocks:_*
      )
      .build()
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  type AuthRetrievals =
    Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup]  ~ ConfidenceLevel

  case class RetrievalData(enrolments: Enrolments,
                           name: Option[Name],
                           credentials: Option[Credentials],
                           affinityGroup: Option[AffinityGroup],
                           confidenceLevel: ConfidenceLevel)

  val authActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutPredicateV2],
    app.injector.instanceOf[AuthoriseAndRetrieve],
    app.injector.instanceOf[AuthoriseAndRetrieveIndividual],
    app.injector.instanceOf[AuthoriseAndRetrieveAgent],
    app.injector.instanceOf[AgentHasClientDetails],
    app.injector.instanceOf[AsMtdUser],
    app.injector.instanceOf[NavBarPredicateV2],
    app.injector.instanceOf[IncomeSourceDetailsPredicate],
    app.injector.instanceOf[FeatureSwitchPredicateV2]
  )(appConfig, ec)



}
