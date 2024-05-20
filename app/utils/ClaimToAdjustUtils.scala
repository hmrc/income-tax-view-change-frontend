package utils

import auth.MtdItUser
import config.featureswitch.{AdjustPaymentsOnAccount, FeatureSwitching}
import controllers.routes
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

trait ClaimToAdjustUtils extends FeatureSwitching{

  def ifAdjustPoaIsEnabled(isAgent: Boolean)
                                  (block: Future[Result])
                                  (implicit user: MtdItUser[_]): Future[Result] = {
    if(isEnabled(AdjustPaymentsOnAccount)) {
      block
    } else {
      Future.successful(
        Redirect(
          if (isAgent) routes.HomeController.showAgent
          else         routes.HomeController.show()
        )
      )
    }
  }

}
