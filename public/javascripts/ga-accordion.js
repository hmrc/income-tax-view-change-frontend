var open = false;

function markAccordionOpen(category, label) {
    if (!open) {
        open = true;
        ga('send', 'event', category, 'ExpandProgressiveDisclosure', label);
    }
}