(function (document, window) {
    document.querySelectorAll('a[href="#print-dialogue"]')
        .forEach(function (link) {
            link.addEventListener('click', function (event) {
                event.preventDefault();
                window.print();
            })
        })
})(document, window);