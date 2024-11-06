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
import config.FrontendAppConfig
import models.admin._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

class FeatureSwitchingSpec extends TestSupport with FeatureSwitching with MockitoSugar {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  object MockFeatureSwitching extends FeatureSwitching {
    override val appConfig: FrontendAppConfig = mockAppConfig
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    FeatureSwitchName.allFeatureSwitches.foreach(feature => sys.props.remove(feature.name))
  }

  val mtdItUser: MtdItUser[_] =
    MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil),
      btaNavPartial = None,
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = None,
      arn = None
    )

  "FeatureSwitchName" when {
    ".allFeatureSwitches" should {
      "return a list of all feature switches" in {

        enable(ReportingFrequencyPage)
        disable(ReviewAndReconcilePoa)

        val featureSwitchList = FeatureSwitchName.allFeatureSwitches

        featureSwitchList shouldBe
          Set(
            ITSASubmissionIntegration,
            ChargeHistory,
            NavBarFs,
            CreditsRefundsRepay,
            PaymentHistoryRefunds,
            IncomeSourcesNewJourney,
            IncomeSources,
            OptOut,
            AdjustPaymentsOnAccount,
            FilterCodedOutPoas,
            ReviewAndReconcilePoa,
            ReportingFrequencyPage
          )
      }
    }
  }

  "FeatureSwitching" should {

    "enable and disable feature switches by setting system properties" in {

      val featureSwitchName = ReportingFrequencyPage

      enable(ReportingFrequencyPage)
      sys.props(featureSwitchName.name) shouldBe "true"

      disable(featureSwitchName)
      sys.props(featureSwitchName.name) shouldBe "false"
    }

    "return true if a feature switch is enabled in system properties" in {

      val featureSwitchName = ReportingFrequencyPage
      enable(featureSwitchName)

      isEnabledFromConfig(featureSwitchName) shouldBe true
    }

    "return false if a feature switch is not set or explicitly disabled in system properties" in {

      val featureSwitchName = ReportingFrequencyPage

      isEnabledFromConfig(featureSwitchName) shouldBe false

      enable(featureSwitchName)
      disable(featureSwitchName)

      isEnabledFromConfig(featureSwitchName) shouldBe false
    }

    "provide a fold function that branches based on feature state" in {
      val featureSwitchName = ReportingFrequencyPage
      enable(featureSwitchName)

      val resultEnabled = featureSwitchName.fold(ifEnabled = "enabled", ifDisabled = "disabled")
      resultEnabled shouldBe "enabled"

      disable(featureSwitchName)

      val resultDisabled = featureSwitchName.fold(ifEnabled = "enabled", ifDisabled = "disabled")
      resultDisabled shouldBe "disabled"
    }
  }

  "Mock FeatureSwitching" when {

    "use MongoDB feature switch status if MongoDB is enabled in config" in {

      val featureSwitchName = ReviewAndReconcilePoa

      when(mockAppConfig.readFeatureSwitchesFromMongo).thenReturn(true)

      val userFeatureSwitches = List(FeatureSwitch(featureSwitchName, isEnabled = true))

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser.copy(featureSwitches = userFeatureSwitches)) shouldBe true
    }

    "fall back to system properties if MongoDB is disabled in config" in {

      val featureSwitchName = ReviewAndReconcilePoa

      when(appConfig.readFeatureSwitchesFromMongo).thenReturn(false)

      enable(featureSwitchName)

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser) shouldBe true

      disable(featureSwitchName)

      MockFeatureSwitching.isEnabled(featureSwitchName)(mtdItUser) shouldBe false
    }
  }
}