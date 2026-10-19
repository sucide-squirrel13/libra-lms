"use strict";

(function () {
    // Expose robust global handlers for the profile menu
    window.toggleLibraProfile = function (event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        var menu = document.getElementById("profile-menu");
        var trigger = document.getElementById("profile-trigger");
        if (!menu) return;

        var isOpen = menu.classList.toggle("open");
        if (trigger) {
            trigger.setAttribute("aria-expanded", String(isOpen));
        }
    };

    window.closeLibraProfile = function () {
        var menu = document.getElementById("profile-menu");
        var trigger = document.getElementById("profile-trigger");
        if (menu) {
            menu.classList.remove("open");
        }
        if (trigger) {
            trigger.setAttribute("aria-expanded", "false");
        }
    };

    function initNavbar() {
        var trigger = document.getElementById("profile-trigger");
        var menu = document.getElementById("profile-menu");
        var mobileButton = document.getElementById("mobile-menu-button");
        var nav = document.getElementById("nav-right");
        var photoInput = document.getElementById("profile-photo");
        var photoForm = document.getElementById("profile-photo-form");
        var changePhotoButton = document.getElementById("change-photo-button");

        if (trigger) {
            trigger.onclick = window.toggleLibraProfile;
        }

        if (menu) {
            menu.onclick = function (e) {
                e.stopPropagation();
            };
        }

        // Close on outside click
        document.addEventListener("click", function (event) {
            if (!event.target.closest(".profile-area")) {
                window.closeLibraProfile();
            }
        });

        // Close on Escape key
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                window.closeLibraProfile();
            }
        });

        // Mobile drawer toggle
        if (mobileButton && nav) {
            mobileButton.onclick = function (event) {
                event.stopPropagation();
                var isOpen = nav.classList.toggle("mobile-open");
                mobileButton.setAttribute("aria-expanded", String(isOpen));
                mobileButton.textContent = isOpen ? "✕" : "☰";
                if (!isOpen) {
                    window.closeLibraProfile();
                }
            };

            nav.querySelectorAll(".nav-link").forEach(function (link) {
                link.onclick = function () {
                    nav.classList.remove("mobile-open");
                    mobileButton.setAttribute("aria-expanded", "false");
                    mobileButton.textContent = "☰";
                    window.closeLibraProfile();
                };
            });
        }

        // Profile photo upload trigger
        if (changePhotoButton && photoInput && photoForm) {
            changePhotoButton.onclick = function (event) {
                event.stopPropagation();
                photoInput.click();
            };

            photoInput.onchange = function () {
                if (!photoInput.files || photoInput.files.length === 0) {
                    return;
                }

                var file = photoInput.files[0];
                var allowedTypes = ["image/png", "image/jpeg"];

                if (!allowedTypes.includes(file.type)) {
                    alert("Please select a PNG or JPEG image.");
                    photoInput.value = "";
                    return;
                }

                if (file.size > 3 * 1024 * 1024) {
                    alert("Please select an image smaller than 3 MB.");
                    photoInput.value = "";
                    return;
                }

                photoForm.submit();
            };
        }

        // Handle avatar photo load/error
        document.querySelectorAll(".navbar .avatar img").forEach(function (img) {
            var avatar = img.closest(".avatar");
            if (!avatar) return;

            function updateAvatarVisibility() {
                if (img.naturalWidth > 0) {
                    avatar.classList.add("has-photo");
                } else {
                    avatar.classList.remove("has-photo");
                }
            }

            img.onload = updateAvatarVisibility;
            img.onerror = function () {
                avatar.classList.remove("has-photo");
            };

            if (img.complete) {
                updateAvatarVisibility();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initNavbar);
    } else {
        initNavbar();
    }
})();