document.addEventListener("DOMContentLoaded", () => {
    const bookElement = document.getElementById("flip-book");

    if (!bookElement || typeof St === "undefined") {
        return;
    }

    const pageFlip = new St.PageFlip(bookElement, {
        width: 400,
        height: 560,
        size: "stretch",
        minWidth: 280,
        maxWidth: 800,
        minHeight: 390,
        maxHeight: 1120,
        showCover: true,
        usePortrait: true,
        drawShadow: true,
        flippingTime: 700,
        maxShadowOpacity: 0.35,
        mobileScrollSupport: true,
        autoSize: true
    });

    const pages = Array.from(
        bookElement.querySelectorAll(".flip-page")
    );

    if (pages.length > 0) {
        pageFlip.loadFromHTML(pages);
    }

    document.getElementById("flip-next")?.addEventListener(
        "click",
        () => pageFlip.flipNext()
    );

    document.getElementById("flip-prev")?.addEventListener(
        "click",
        () => pageFlip.flipPrev()
    );
});