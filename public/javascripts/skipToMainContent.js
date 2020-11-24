$(document).ready($(function () {
    document.getElementById('skip-link').addEventListener('click', function(e) {
        e.preventDefault();
        var target = document.querySelectorAll('.content__body, .report-error')[0];
        target.setAttribute('tabindex', '-1');
        target.focus();
    });
}));
