/*
 * Copyright 2026 HM Revenue & Customs
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

package testConstants

import play.api.libs.json.Json

object AddressLookupTestConstants {

  val ukRequestBodyIndividual = Json.parse(
    """{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/session-timeout",
      |      "timeoutKeepAliveUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/keep-alive"
      |    },
      |    "signOutHref": "http://localhost:9081/report-quarterly/income-and-expenses/view/sign-out",
      |    "accessibilityFooterUrl": "http://localhost:9081/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": true,
      |      "showConfirmChangeText": true
      |    },
      |    "phaseFeedbackLink": "http://localhost:9081/report-quarterly/income-and-expenses/view/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": true
      |  },
      |  "labels": {
      |    "en": {
      |      "selectPageLabels": {
      |        "heading": "Select business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Manage your Self Assessment"
      |      }
      |    },
      |    "cy": {
      |      "selectPageLabels": {
      |        "heading": "Dewiswch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Rheoli’ch Hunanasesiad"
      |      }
      |    }
      |  }
      |}""".stripMargin)

  val ukRequestBodyAgent = Json.parse(
    """
      |{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/session-timeout",
      |      "timeoutKeepAliveUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/keep-alive"
      |    },
      |    "signOutHref": "http://localhost:9081/report-quarterly/income-and-expenses/view/sign-out",
      |    "accessibilityFooterUrl": "http://localhost:9081/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": true,
      |      "showConfirmChangeText": true
      |    },
      |    "phaseFeedbackLink": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": true
      |  },
      |  "labels": {
      |    "en": {
      |      "selectPageLabels": {
      |        "heading": "Select business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Manage your Self Assessment"
      |      }
      |    },
      |    "cy": {
      |      "selectPageLabels": {
      |        "heading": "Dewiswch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Rheoli’ch Hunanasesiad"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
  
  val internationalRequestBodyInvididual = Json.parse(
    """
      |{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/session-timeout",
      |      "timeoutKeepAliveUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/keep-alive"
      |    },
      |    "signOutHref": "http://localhost:9081/report-quarterly/income-and-expenses/view/sign-out",
      |    "accessibilityFooterUrl": "http://localhost:9081/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": true,
      |      "showConfirmChangeText": true
      |    },
      |    "manualAddressEntryConfig": {
      |      "mandatoryFields": {
      |       "addressLine1": true,
      |       "addressLine2": true
      |      }
      |    },
      |    "phaseFeedbackLink": "http://localhost:9081/report-quarterly/income-and-expenses/view/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": false
      |  },
      |  "labels": {
      |    "en": {
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Select the country or territory for your business address",
      |        "title": "Select the country or territory for your business address"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Enter your international business address",
      |          "title": "Enter your international business address",
      |          "postcodeLabel": "Postcode or zipcode"
      |        }
      |      }
      |    },
      |    "cy": {
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Dewiswch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes",
      |        "title": "Dewiswch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "title": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "postcodeLabel": "Cod post neu god ‘zip’"
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)

  val internationalRequestBodyAgent = Json.parse(
    """
      |{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/session-timeout",
      |      "timeoutKeepAliveUrl": "http://localhost:9081/report-quarterly/income-and-expenses/view/keep-alive"
      |    },
      |    "signOutHref": "http://localhost:9081/report-quarterly/income-and-expenses/view/sign-out",
      |    "accessibilityFooterUrl": "http://localhost:9081/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": true,
      |      "showConfirmChangeText": true
      |    },
      |    "manualAddressEntryConfig": {
      |      "mandatoryFields": {
      |       "addressLine1": true,
      |       "addressLine2": true
      |      }
      |    },
      |    "phaseFeedbackLink": "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": false
      |  },
      |  "labels": {
      |    "en": {
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Select the country or territory for your business address",
      |        "title": "Select the country or territory for your business address"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Enter your international business address",
      |          "title": "Enter your international business address",
      |          "postcodeLabel": "Postcode or zipcode"
      |        }
      |      }
      |    },
      |    "cy": {
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Dewiswch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes",
      |        "title": "Dewiswch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "title": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "postcodeLabel": "Cod post neu god ‘zip’"
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
