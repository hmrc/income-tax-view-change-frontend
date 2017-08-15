$(document).ready($(function () {

    var $form = $("form");
    var $submissionButton = $("form button[type=submit]");

    $submissionButton.on('click', function (e) {
        var satisfactionSelection = $('input[name=satisfaction]:checked').val();
        if (typeof ga === "function" && satisfactionSelection != undefined) {
            e.preventDefault();
            ga('send', 'event', 'itvc-exit-survey', 'satisfaction', satisfactionSelection, {
                hitCallback: function () {
                    $form.submit();
                }
            });
        }
    });

}));