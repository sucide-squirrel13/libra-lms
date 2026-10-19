"use strict";

document.addEventListener("DOMContentLoaded", function () {
    const trigger = document.getElementById("profile-trigger");
    const menu = document.getElementById("profile-menu");
    const mobileButton = document.getElementById("mobile-menu-button");
    const nav = document.getElementById("nav-right");
    const photoInput = document.getElementById("profile-photo");
    const photoForm = document.getElementById("profile-photo-form");
    const changePhotoButton = document.getElementById("change-photo-button");

    // The script is shared, so exit if this page has no navbar.
    if (!trigger || !menu || !mobileButton || !nav) {
        return;
    }

    function closeProfileMenu() {
        menu.classList.remove("open");
        menu.hidden = true;
        trigger.setAttribute("aria-expanded", "false");
    }

    function openProfileMenu() {
        menu.hidden = false;

        // Allow the browser to apply the visible state before animating.
        requestAnimationFrame(function () {
            menu.classList.add("open");
        });

        trigger.setAttribute("aria-expanded", "true");
    }

    function toggleProfileMenu(event) {
        event.stopPropagation();

        if (trigger.getAttribute("aria-expanded") === "true") {
            closeProfileMenu();
        } else {
            openProfileMenu();
        }
    }

    // Profile dropdown
    trigger.addEventListener("click", toggleProfileMenu);

    // Close when clicking outside the profile area.
    document.addEventListener("click", function (event) {
        if (!event.target.closest(".profile-area")) {
            closeProfileMenu();
        }
    });

    // Keep clicks inside the dropdown from closing it prematurely.
    menu.addEventListener("click", function (event) {
        event.stopPropagation();
    });

    // Mobile navigation
    mobileButton.addEventListener("click", function () {
        const isOpen = nav.classList.toggle("mobile-open");

        mobileButton.setAttribute("aria-expanded", String(isOpen));
        mobileButton.textContent = isOpen ? "✕" : "☰";

        if (!isOpen) {
            closeProfileMenu();
        }
    });

    // Close mobile navigation after following a navigation link.
    nav.querySelectorAll(".nav-link").forEach(function (link) {
        link.addEventListener("click", function () {
            nav.classList.remove("mobile-open");
            mobileButton.setAttribute("aria-expanded", "false");
            mobileButton.textContent = "☰";
            closeProfileMenu();
        });
    });

    // Only open the file picker when the user clicks the button.
    if (changePhotoButton && photoInput && photoForm) {
        changePhotoButton.addEventListener("click", function () {
            photoInput.click();
        });

        photoInput.addEventListener("change", function () {
            if (!photoInput.files || photoInput.files.length === 0) {
                return;
            }

            const file = photoInput.files[0];
            const allowedTypes = ["image/png", "image/jpeg"];

            if (!allowedTypes.includes(file.type)) {
                alert("Please select a PNG or JPEG image.");
                photoInput.value = "";
                return;
            }

            if (file.size > 3 * 1024 * 1024) {
                alert("The profile picture must be smaller than 3 MB.");
                photoInput.value = "";
                return;
            }

            photoForm.requestSubmit();
        });
    }

    // Populate avatar initials from the displayed user's name.
    const nameElement = document.querySelector(".profile-heading strong");
    const fullName = nameElement ? nameElement.textContent.trim() : "";
    const initial = fullName ? fullName.charAt(0).toUpperCase() : "U";

    ["navbar-initial", "menu-initial"].forEach(function (id) {
        const element = document.getElementById(id);

        if (element) {
            element.textContent = initial;
        }
    });

    // Display the profile photo when it loads successfully.
    document.querySelectorAll(".navbar .avatar img").forEach(function (img) {
        const avatar = img.closest(".avatar");

        function showPhoto() {
            if (img.naturalWidth > 0) {
                avatar.classList.add("has-photo");
            } else {
                avatar.classList.remove("has-photo");
            }
        }

        img.addEventListener("load", showPhoto);

        img.addEventListener("error", function () {
            avatar.classList.remove("has-photo");
        });

        if (img.complete) {
            showPhoto();
        }
    });

    // Close menus with Escape.
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeProfileMenu();

            nav.classList.remove("mobile-open");
            mobileButton.setAttribute("aria-expanded", "false");
            mobileButton.textContent = "☰";

            trigger.focus();
        }
    });
});