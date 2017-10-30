$(document).ready(function() {
// =====================================================
// Handle the ITVC UR panel dismiss link functionality
// =====================================================

    var cookieData=GOVUK.getCookie("bta_ur_panel");
    var URbanner = $("#ur-panel");

    if (cookieData == null) {
        URbanner.addClass("banner-panel--show").removeAttr('style');
    }

    $(".banner-panel__close").on("click", function(e) {
        e.preventDefault();
         GOVUK.setCookie("bta_ur_panel", 1, 99999999999);
         URbanner.removeClass("banner-panel--show").addClass('hidden');
    });
  // end of on doc ready
});