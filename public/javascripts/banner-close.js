$(document).ready(function() {
// =====================================================
// Handle the ITVC UR panel dismiss link functionality
// =====================================================

    var cookieData=GOVUK.getCookie("mdtpurr");
    var URbanner = $("#ur-panel");

    if (cookieData == null) {
        URbanner.addClass("banner-panel--show").removeAttr('style');
    }

    $(".banner-panel__close").on("click", function(e) {
        e.preventDefault();
         GOVUK.setCookie("mdtpurr", 1, 2419200);
         URbanner.removeClass("banner-panel--show").addClass('hidden');
    });
  // end of on doc ready
});