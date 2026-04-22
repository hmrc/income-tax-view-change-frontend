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

package config.featureswitch

import auth.MtdItUser
import authV2.AuthActionsTestData._
import config.FrontendAppConfig
import models.admin._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import testUtils.TestSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class FeatureSwitchingSpec extends TestSupport with FeatureSwitching with MockitoSugar {

  override val appConfig: FrontendAppConfig =
    new FrontendAppConfig(
      app.injector.instanceOf[ServicesConfig],
      app.injector.instanceOf[Configuration]
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    FeatureSwitchName.allFeatureSwitches.foreach(feature => sys.props.remove(feature.name))
  }

  val mtdItUser: MtdItUser[_] = defaultMTDITUser(None, IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil))

  private def setFeatureSwitch(fs: FeatureSwitchName, enabled: Boolean): MtdItUser[_] = {
    sys.props += fs.name -> enabled.toString
    mtdItUser.copy(featureSwitches = List(FeatureSwitch(fs, isEnabled = enabled)))
  }

  val allFeatureSwitches: Set[FeatureSwitchName] = Set(
    ITSASubmissionIntegration,
    ChargeHistory,
    NavBarFs,
    CreditsRefundsRepay,
    PaymentHistoryRefunds,
    OptOutFs,
    SignUpFs,
    FilterCodedOutPoas,
    DisplayBusinessStartDate,
    PenaltiesAndAppeals,
    PenaltiesBackendEnabled,
    OptInOptOutContentUpdateR17,
    SelfServeTimeToPayR17,
    SubmitClaimToAdjustToNrs,
    TriggeredMigration,
    PostFinalisationAmendmentsR18,
    `CY+1YouMustWaitToSignUpPageEnabled`,
    NewHomePage,
    OverseasBusinessAddress,
    RecentActivity,
    MortgageEvidence
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

      s"enable and disable feature switches by setting system properties for FS: ${featureSwitchName.name}" in {

        setFeatureSwitch(featureSwitchName, enabled = true)
        sys.props(featureSwitchName.name) shouldBe "true"

        setFeatureSwitch(featureSwitchName, enabled = false)
        sys.props(featureSwitchName.name) shouldBe "false"
      }

      s"return true if a feature switch is enabled in system properties for FS: ${featureSwitchName.name}" in {

        setFeatureSwitch(featureSwitchName, enabled = true)

        isEnabledFromConfig(featureSwitchName) shouldBe true
      }

      s"return false if a feature switch is disabled in system properties for FS: ${featureSwitchName.name}" in {

        setFeatureSwitch(featureSwitchName, enabled = true)
        setFeatureSwitch(featureSwitchName, enabled = false)

        isEnabledFromConfig(featureSwitchName) shouldBe false
      }

      s"provide a fold function that branches based on feature state for FS: ${featureSwitchName.name}" in {

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

      val featureSwitchName = FilterCodedOutPoas

      when(MockFeatureSwitching.appConfig.readFeatureSwitchesFromMongo).thenReturn(true)

      val userFeatureSwitches = List(FeatureSwitch(featureSwitchName, isEnabled = true))

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser.copy(featureSwitches = userFeatureSwitches)) shouldBe true
    }

    "fall back to system properties if MongoDB is disabled in config" in {

      val featureSwitchName = FilterCodedOutPoas

      when(MockFeatureSwitching.appConfig.readFeatureSwitchesFromMongo).thenReturn(false)

      setFeatureSwitch(featureSwitchName, enabled = true)

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser) shouldBe true

      setFeatureSwitch(featureSwitchName, enabled = false)

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser) shouldBe false
    }
  }
}