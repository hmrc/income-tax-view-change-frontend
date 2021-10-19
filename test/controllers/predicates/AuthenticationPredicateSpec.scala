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

package controllers.predicates

import testConstants.BaseTestConstants.{testAuthSuccessResponse, testAuthSuccessResponseOrgNoNino, testMtditid, testNino}
import audit.AuditingService
import audit.mocks.MockAuditingService
import config.featureswitch.{FeatureSwitching, IvUplift}
import config.{FrontendAppConfig, ItvcErrorHandler}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

class AuthenticationPredicateSpec extends TestSupport with MockitoSugar with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockAuditingService with FeatureSwitching {

	val ivUpliftRedirectUrl: String = s"/mdtp/uplift?origin=ITVC&confidenceLevel=200&" +
		s"completionURL=${appConfig.itvcFrontendEnvironment + "/" + appConfig.baseUrl + controllers.routes.UpliftSuccessController.success().url}&" +
		s"failureURL=${appConfig.itvcFrontendEnvironment + "/" + appConfig.baseUrl + controllers.errors.routes.UpliftFailedController.show().url}"

	"The AuthenticationPredicate" when {

		def setupResult(): Action[AnyContent] =
			new AuthenticationPredicate()(
				ec,
				mockAuthService,
				app.injector.instanceOf[FrontendAppConfig],
				app.injector.instanceOf[Configuration],
				app.injector.instanceOf[Environment],
				app.injector.instanceOf[ItvcErrorHandler],
				app.injector.instanceOf[MessagesControllerComponents],
				mockAuditingService
			).async {
				implicit request =>
					Future.successful(Ok(testMtditid + " " + testNino))
			}

		"called with an authenticated user" when {

			def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

			"a HMRC-MTD-IT enrolment exists" when {

				"return Ok (200)" in {
					enable(IvUplift)
					status(result) shouldBe Status.OK
				}
			}

			"a HMRC-MTD-IT enrolment does NOT exist" should {

				"have a redirect (303 - SEE_OTHER) status" in {
					setupMockAuthorisationException(new InsufficientEnrolments)
					status(result) shouldBe Status.SEE_OTHER
				}

				"redirect to the Not Enrolled page" in {
					setupMockAuthorisationException(new InsufficientEnrolments)
					redirectLocation(result) shouldBe Some(controllers.errors.routes.NotEnrolledController.show().url)
				}
			}
			"there is a HMRC-MTD-IT enrolment but a user details error from auth" should {
				"return Ok (200)" in {
					status(result) shouldBe Status.OK
				}
			}
		}

		"called with a Bearer Token Expired response from Auth" should {

			lazy val result = setupResult()(fakeRequestWithActiveSession)

			"should be a redirect (303)" in {
				setupMockAuthorisationException(new BearerTokenExpired)
				status(result) shouldBe Status.SEE_OTHER
			}

			"should redirect to GG Sign In" in {
				setupMockAuthorisationException(new BearerTokenExpired)
				redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
			}
		}

		"called with a user with no session" should {

			lazy val result = setupResult()(fakeRequestNoSession)

			"be a redirect (303)" in {
				setupMockAuthorisationException()
				status(result) shouldBe Status.SEE_OTHER
			}

			"redirect to GG Sign In" in {
				redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
			}
		}

		"called with a confidence level below 200" should {

			"redirect to the IV Uplift Journey" when {

				"the feature switch is enabled for an individual" in {
					enable(IvUplift)
					setupMockAuthRetrievalSuccess(testAuthSuccessResponse(ConfidenceLevel.L50))

					def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

					redirectLocation(result) shouldBe Some(ivUpliftRedirectUrl)
				}

				"the feature switch is enabled for an organisation with a nino" in {
					enable(IvUplift)
					setupMockAuthRetrievalSuccess(testAuthSuccessResponse(ConfidenceLevel.L50, AffinityGroup.Organisation))

					def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

					redirectLocation(result) shouldBe Some(ivUpliftRedirectUrl)
				}

				"the feature switch is enabled for an organisation without a nino" in {
					enable(IvUplift)
					setupMockAuthRetrievalSuccess(testAuthSuccessResponseOrgNoNino(ConfidenceLevel.L50))

					def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

					redirectLocation(result) shouldBe Some(ivUpliftRedirectUrl)
				}
			}

			"return Ok (200)" when {

				"the feature switch is disabled" in {
					disable(IvUplift)
					setupMockAuthRetrievalSuccess(testAuthSuccessResponse(ConfidenceLevel.L50))

					def result: Future[Result] = setupResult()(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
				}
			}
		}
	}
}
