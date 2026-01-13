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
import auth.authV2.actions.ItsaStatusRetrievalAction
import authV2.AuthActionsTestData._
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import connectors.ITSAStatusConnector
import models.admin.`CY+1YouMustWaitToSignUpPageEnabled`
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Voluntary
import models.itsaStatus.StatusReason.MtdItsaOptOut
import models.itsaStatus.{ITSAStatusResponseError, ITSAStatusResponseModel, StatusDetail}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, anyBoolean}
import org.mockito.Mockito.{never, reset, verify, when}
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.DateServiceInterface
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}

import scala.concurrent.Future

class ItsaStatusRetrievalActionSpec extends TestSupport with ScalaFutures {

  lazy val mockItsaStatusConnector = mock[ITSAStatusConnector]
  lazy val mockDateServiceInterface = mock[DateServiceInterface]

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .build()

  lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]

  lazy val itvcErrorHandler =
    app.injector.instanceOf[ItvcErrorHandler]

  lazy val agentErrorHandler =
    app.injector.instanceOf[AgentItvcErrorHandler]

  val action =
    new ItsaStatusRetrievalAction(
      appConfig,
      mockItsaStatusConnector,
      mockDateServiceInterface
    )(
      ec,
      itvcErrorHandler,
      agentErrorHandler,
      mcc
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockItsaStatusConnector)
  }

  ".refine()" when {

    "CY+1YouMustWaitToSignUpPageEnabled feature switch is Enabled" should {

      "redirect to YouMustWaitToSignUp page when only next-year ITSA status exists - Agent" in {

        enable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val itsaStatusResponse =
          ITSAStatusResponseModel(
            taxYear = "2026-27",
            itsaStatusDetails = Some(List(
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
            ))
          )

        val mtdUser = getMtdItUser(Agent)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Right(List(itsaStatusResponse))))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result.left.get.header.status shouldBe SEE_OTHER
        result.left.get.header.headers("LOCATION") shouldBe controllers.optIn.routes.YouMustWaitToSignUpController.show(true).url
      }

      "redirect to YouMustWaitToSignUp page when only next-year ITSA status exists - Individual" in {

        enable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val itsaStatusResponse =
          ITSAStatusResponseModel(
            taxYear = "2026-27",
            itsaStatusDetails = Some(List(
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
            ))
          )

        val mtdUser = getMtdItUser(Individual, isSupportingAgent = true)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Right(List(itsaStatusResponse))))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result.left.get.header.status shouldBe SEE_OTHER
        result.left.get.header.headers("LOCATION") shouldBe controllers.optIn.routes.YouMustWaitToSignUpController.show(false).url
      }

      "redirect to YouMustWaitToSignUp page when only next-year ITSA status exists - SupportingAgent" in {

        enable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val itsaStatusResponse =
          ITSAStatusResponseModel(
            taxYear = "2026-27",
            itsaStatusDetails = Some(List(
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
            ))
          )

        val mtdUser = getMtdItUser(Agent, isSupportingAgent = true)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Right(List(itsaStatusResponse))))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result.left.get.header.status shouldBe SEE_OTHER
        result.left.get.header.headers("LOCATION") shouldBe controllers.optIn.routes.YouMustWaitToSignUpController.show(true).url
      }

      "return an MtdItUser when valid ITSA status exists for current and future years" in {

        val itsaStatusResponses = List(
          ITSAStatusResponseModel(
            taxYear = "2024-25",
            itsaStatusDetails = Some(List(
              StatusDetail("ts", Voluntary, MtdItsaOptOut, None)
            ))
          ),
          ITSAStatusResponseModel(
            taxYear = "2025-26",
            itsaStatusDetails = Some(List(
              StatusDetail("ts", Voluntary, MtdItsaOptOut, None)
            ))
          )
        )

        val mtdUser = getMtdItUser(Individual)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Right(itsaStatusResponses)))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result.map(_.mtditid) shouldBe Right(mtdUser.mtditid)
        result.map(_.nino) shouldBe Right(defaultIncomeSourcesData.nino)
        result.map(_.usersRole) shouldBe Right(mtdUser.usersRole)
        result.map(_.authUserDetails) shouldBe Right(mtdUser.authUserDetails)
        result.map(_.clientDetails) shouldBe Right(mtdUser.clientDetails)
        result.map(_.incomeSources) shouldBe Right(defaultIncomeSourcesData)
      }

      "return Individual error page" in {

        enable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val itsaStatusResponseError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "some random failure")

        val mtdUser = getMtdItUser(Individual)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Left(itsaStatusResponseError)))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result match {
          case Left(res) =>
            val body = contentAsString(Future.successful(res))
            val document = Jsoup.parse(body)
            val title = document.title()
            val feedbackUrl = document.getElementById("individuals-feedback-url").select("a").attr("href")

            res.header.status shouldBe INTERNAL_SERVER_ERROR
            title shouldBe "Sorry, there is a problem with the service - GOV.UK"
            feedbackUrl shouldBe "/report-quarterly/income-and-expenses/view/feedback" // to help distinguish error pages checking for feedback url since page titles are the same
          case Right(_) =>
            fail("Expected Individual error page but fell into a Right branch")
        }
      }

      "return Agent error page" in {

        enable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val itsaStatusResponseError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "some random failure")

        val mtdUser = getMtdItUser(Agent)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Left(itsaStatusResponseError)))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result match {
          case Left(res) =>
            val body = contentAsString(Future.successful(res))
            val document = Jsoup.parse(body)
            val title = document.title()
            val agentFeedbackUrl = document.getElementById("agent-feedback-url").select("a").attr("href")

            res.header.status shouldBe INTERNAL_SERVER_ERROR
            title shouldBe "Sorry, there is a problem with the service - GOV.UK"
            agentFeedbackUrl shouldBe "/report-quarterly/income-and-expenses/view/agents/feedback" // to help distinguish error pages checking for feedback url since page titles are the same
          case Right(_) =>
            fail("Expected Agent error page but fell into a Right branch")
        }
      }

      "redirect to error page when user's affinity group is 'Organisation'" in {

        enable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val itsaStatusResponse =
          ITSAStatusResponseModel(
            taxYear = "2026-27",
            itsaStatusDetails = Some(List(
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
              StatusDetail("some fake timestamp", Voluntary, MtdItsaOptOut, None),
            ))
          )

        val mtdUser = getMtdItUser(Organisation)

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Right(List(itsaStatusResponse))))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action.refine(mtdUser).futureValue

        result match {
          case Left(res) =>
            val body = contentAsString(Future.successful(res))
            val document = Jsoup.parse(body)
            val title = document.title()
            res.header.status shouldBe INTERNAL_SERVER_ERROR
            title shouldBe "Sorry, there is a problem with the service - GOV.UK"
          case Right(_) =>
            fail("Expected Organisation error page but fell into a Right branch")
        }
      }
    }

    "CY+1YouMustWaitToSignUpPageEnabled feature switch is Disabled" should {

      "return the request unmodified" in {

        disable(`CY+1YouMustWaitToSignUpPageEnabled`)

        val mtdAgentUser = getMtdItUser(Agent)

        val result: Either[Result, MtdItUser[Any]] = action.refine(mtdAgentUser).futureValue

        result.foreach { user =>
          user shouldBe mtdAgentUser
        }

        verify(mockItsaStatusConnector, never())
          .getITSAStatusDetail(any(), any(), anyBoolean(), anyBoolean())(any())
      }
    }

  }
}
