$(document).ready(function() {
   // Details/summary polyfill from frontend toolkit
   GOVUK.details.init();
    let tabListItems = document.querySelectorAll(".govuk-tabs__list-item");
    let tabPanels = document.querySelectorAll(".govuk-tabs__panel");
    tabListItems.forEach(addClickListener);
    function enableTab(e) {
        if (screen.width > "700"){
            e.preventDefault();
        }
        tabListItems.forEach(clearSelectedTabs);
        tabPanels.forEach(clearSelectedPanels);
        this.className = "govuk-tabs__list-item govuk-tabs__list-item--selected";
        this.getElementsByTagName("a")[0].setAttribute("aria-selected", "true");
        let panelIdToEnable = this.getElementsByTagName("a")[0].getAttribute("href").replace("#", "");
        document.getElementById(panelIdToEnable).className = "govuk-tabs__panel"
    }
    function clearSelectedPanels(item, index) {
        item.className = "govuk-tabs__panel govuk-tabs__panel--hidden"
    }
    function clearSelectedTabs(item, index) {
        item.getElementsByTagName("a")[0].setAttribute("aria-selected", "false");
        item.className = "govuk-tabs__list-item";
    }
    function addClickListener(item, index) {
        item.addEventListener("click", enableTab);
    }

    if(document.getElementById("error-summary-display")) {
        document.getElementById("error-summary-display").focus()
    }

});


