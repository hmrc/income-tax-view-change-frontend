function optOutCustomData(answer, customPrefixes) {

    const ninoPrefix = answer.value.substring(0, 2);

    if (customPrefixes.includes(ninoPrefix)) {
        document.getElementById("OptOutCustomDataDropdowns").classList.remove("display-none");
    } else {
        document.getElementById("OptOutCustomDataDropdowns").classList.add("display-none");
    }
}
