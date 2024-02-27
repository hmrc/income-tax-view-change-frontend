package testOnly.services

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import testOnly.utils.OptOutCustomDataUploadHelper

import javax.inject.Inject

class OptOutCustomDataService @Inject()(implicit val appConfig: FrontendAppConfig) extends OptOutCustomDataUploadHelper with FeatureSwitching {

}
