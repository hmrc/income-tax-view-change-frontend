package views.manageBusinesses

import testUtils.TestSupport
import views.html.manageBusinesses.ManageYourBusinesses

class ManageYourBusinessesViewSpec extends TestSupport {

  val view: ManageYourBusinesses = app.injector.instanceOf[ManageYourBusinesses]

  "ManageYourBusinessesView" when {
    "the user is an Agent" should {
      "return the correct content when the user has a sole trader business" in {

      }
      "return the correct content when the user has a uk property business" in {

      }
      "return the correct content when the user has a foreign property business" in {

      }
      "return the correct content when the user has all business types" in {

      }
      "return no business start dates when the displayStartDate flag is false" in {

      }
    }

    "the user is an individual" should {
      "return the correct content when the user has a sole trader business" in {

      }
      "return the correct content when the user has a uk property business" in {

      }
      "return the correct content when the user has a foreign property business" in {

      }
      "return the correct content when the user has all business types" in {

      }
      "return no business start dates when the displayStartDate flag is false" in {

      }
    }
  }

}
