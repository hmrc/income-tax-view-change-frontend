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
import auth.authV2.actions._
import config.{FrontendAppConfig, FrontendAuthConnector, ItvcErrorHandler}
import controllers.bta.BtaNavBarController
import controllers.predicates.IncomeSourceDetailsPredicate
import org.mockito.Mockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import services.{IncomeSourceDetailsService, SessionDataService}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import views.html.navBar.PtaPartial

trait AuthActionsSpecHelper extends TestSupport with ScalaFutures {

  lazy val mockAuthConnector = mock[FrontendAuthConnector]
  lazy val mockIncomeSourceDetailsService = mock[IncomeSourceDetailsService]
  lazy val mockAppConfig = mock[FrontendAppConfig]
  lazy val mockAuditingService = mock[AuditingService]
  lazy val mockSessionDataService = mock[SessionDataService]
  lazy val mockItvcErrorHandler = mock[ItvcErrorHandler]
  lazy val mockBtaNavBarController = mock[BtaNavBarController]
  lazy val mockPtaPartial = mock[PtaPartial]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockIncomeSourceDetailsService)
    Mockito.reset(mockAuditingService)
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  type AgentAuthRetrievals =
    Enrolments ~ Option[Credentials] ~ Option[AffinityGroup]  ~ ConfidenceLevel

  type AuthRetrievals =
    Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup]  ~ ConfidenceLevel


  case class RetrievalData(enrolments: Enrolments,
                           name: Option[Name],
                           credentials: Option[Credentials],
                           affinityGroup: Option[AffinityGroup],
                           confidenceLevel: ConfidenceLevel)

  val authActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutAction],
    app.injector.instanceOf[AuthoriseAndRetrieve],
    app.injector.instanceOf[AuthoriseAndRetrieveIndividual],
    app.injector.instanceOf[AuthoriseAndRetrieveAgent],
    app.injector.instanceOf[AuthoriseAndRetrieveMtdAgent],
    app.injector.instanceOf[AgentHasClientDetails],
    app.injector.instanceOf[AsMtdUser],
    app.injector.instanceOf[NavBarRetrievalAction],
    app.injector.instanceOf[IncomeSourceRetrievalAction],
    app.injector.instanceOf[RetrieveClientData],
    app.injector.instanceOf[FeatureSwitchRetrievalAction]
  )(appConfig, ec)



}
