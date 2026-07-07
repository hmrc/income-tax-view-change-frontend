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

package common.config.featureswitch

import common.auth.actions.AuthActionsTestData.*
import common.auth.MtdItUser
import common.config.FrontendAppConfig
import common.models.admin.*
import common.models.incomeSourceDetails.IncomeSourceDetailsModel
import common.testUtils.TestSupport
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

class FeatureSwitchingSpec extends TestSupport with MockitoSugar {

  override val appConfig: FrontendAppConfig = mock[FrontendAppConfig]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(appConfig)
    when(appConfig.readFeatureSwitchesFromMongo).thenReturn(false)
  }

  val mtdItUser: MtdItUser[_] = defaultMTDITUser(None, IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil))

  private def setFeatureSwitch(fs: FeatureSwitchName, enabled: Boolean): MtdItUser[_] = {
    mtdItUser.copy(featureSwitches = List(FeatureSwitch(fs, isEnabled = enabled)))
  }

  val allFeatureSwitches: Set[FeatureSwitchName] = Set(
    ITSASubmissionIntegration,
    ChargeHistory,
    CreditsRefundsRepay,
    PaymentHistoryRefunds,
    OptOutFs,
    SignUpFs,
    DisplayBusinessStartDate,
    PenaltiesAndAppeals,
    PenaltiesBackendEnabled,
    SelfServeTimeToPayR17,
    SubmitClaimToAdjustToNrs,
    TriggeredMigration,
    PostFinalisationAmendmentsR18,
    `CY+1YouMustWaitToSignUpPageEnabled`,
    NewHomePage,
    OverseasBusinessAddress,
    RecentActivity,
    MortgageEvidence,
    IdempotencyKeyForCreateIncomeSource,
    NoIncomeSourcesRedirect,
    BusinessDetailsFrontend, 
    ObligationsFrontend
  )

  "FeatureSwitchName" when {

    ".allFeatureSwitches" should {

      "return a list of all feature switches" in {

        val featureSwitchList = FeatureSwitchName.allFeatureSwitches

        featureSwitchList shouldBe allFeatureSwitches

      }
    }
  }

  allFeatureSwitches.foreach { featureSwitchName =>
    "FeatureSwitching" when {

      s"enable and disable feature switches via MongoDB for FS: ${featureSwitchName.name}" in {

        when(appConfig.readFeatureSwitchesFromMongo).thenReturn(true)

        val enabledUser = setFeatureSwitch(featureSwitchName, enabled = true)
        isEnabled(featureSwitchName)(enabledUser) shouldBe true

        val disabledUser = setFeatureSwitch(featureSwitchName, enabled = false)
        isEnabled(featureSwitchName)(disabledUser) shouldBe false
      }


      s"provide a fold function that branches based on feature state for FS: ${featureSwitchName.name}" in {

        when(appConfig.readFeatureSwitchesFromMongo).thenReturn(true)

        {
          implicit val user: MtdItUser[_] = setFeatureSwitch(featureSwitchName, enabled = true)
          val resultEnabled = featureSwitchName.fold(ifEnabled = "enabled", ifDisabled = "disabled")
          resultEnabled shouldBe "enabled"
        }

        {
          implicit val user: MtdItUser[_] = setFeatureSwitch(featureSwitchName, enabled = false)
          val resultDisabled = featureSwitchName.fold(ifEnabled = "enabled", ifDisabled = "disabled")
          resultDisabled shouldBe "disabled"
        }
      }
    }
  }


  "Mock FeatureSwitching" when {

    object MockFeatureSwitching extends FeatureSwitching {
      override val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
    }

    "use MongoDB feature switch status if MongoDB is enabled in config" in {

      val featureSwitchName = OverseasBusinessAddress

      when(MockFeatureSwitching.appConfig.readFeatureSwitchesFromMongo).thenReturn(true)

      val userFeatureSwitches = List(FeatureSwitch(featureSwitchName, isEnabled = true))

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser.copy(featureSwitches = userFeatureSwitches)) shouldBe true
    }

    "use MongoDB feature switch status when disabled for featureSwitches list" in {

      val featureSwitchName = OverseasBusinessAddress

      when(MockFeatureSwitching.appConfig.readFeatureSwitchesFromMongo).thenReturn(true)

      val userFeatureSwitches = List(FeatureSwitch(featureSwitchName, isEnabled = false))

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser.copy(featureSwitches = userFeatureSwitches)) shouldBe false
    }
  }
}