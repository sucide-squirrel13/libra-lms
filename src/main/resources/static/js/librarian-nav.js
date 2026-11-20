/**
 * LIBRA LMS — Librarian Navigation Component
 * Handles profile menu, mobile drawer, theme toggling, and local view routing on /librarian.
 */
(function () {
    "use strict";

    function applyLibrarianTheme(theme) {
        const isLight = theme === "light";
        document.body.classList.toggle("light", isLight);
        document.querySelectorAll("#theme-toggle, #theme-toggle-settings").forEach(btn => {
            btn.textContent = isLight ? "🌙 Dark mode" : "☀️ Light mode";
        });
        localStorage.setItem("libra-theme", theme);
    }

    function initLibrarianNav() {
        const trigger = document.getElementById("librarian-profile-trigger");
        const menu = document.getElementById("librarian-profile-menu");
        const area = document.getElementById("librarian-profile-area");
        const mobileToggle = document.getElementById("librarian-mobile-toggle");
        const navMenu = document.getElementById("librarian-nav-menu");
        const themeBtn = document.getElementById("theme-toggle");

        // Theme initialization
        const savedTheme = localStorage.getItem("libra-theme") || "dark";
        applyLibrarianTheme(savedTheme);

        // Theme button click listeners (navbar + settings view)
        document.querySelectorAll("#theme-toggle, #theme-toggle-settings").forEach(btn => {
            btn.addEventListener("click", function () {
                const isLight = document.body.classList.contains("light");
                applyLibrarianTheme(isLight ? "dark" : "light");
            });
        });

        // Profile dropdown toggle
        if (trigger && menu) {
            trigger.addEventListener("click", function (e) {
                e.stopPropagation();
                const isOpen = menu.classList.toggle("open");
                trigger.setAttribute("aria-expanded", String(isOpen));
            });
        }

        // Mobile drawer toggle
        if (mobileToggle && navMenu) {
            mobileToggle.addEventListener("click", function (e) {
                e.stopPropagation();
                const isOpen = navMenu.classList.toggle("mobile-open");
                mobileToggle.setAttribute("aria-expanded", String(isOpen));
            });
        }

        // Close dropdowns on outside click or escape
        document.addEventListener("click", function (e) {
            if (area && menu && !area.contains(e.target)) {
                menu.classList.remove("open");
                if (trigger) trigger.setAttribute("aria-expanded", "false");
            }
            if (navMenu && mobileToggle && !navMenu.contains(e.target) && !mobileToggle.contains(e.target)) {
                navMenu.classList.remove("mobile-open");
                mobileToggle.setAttribute("aria-expanded", "false");
            }
        });

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape") {
                if (menu) {
                    menu.classList.remove("open");
                    if (trigger) trigger.setAttribute("aria-expanded", "false");
                }
                if (navMenu) {
                    navMenu.classList.remove("mobile-open");
                    if (mobileToggle) mobileToggle.setAttribute("aria-expanded", "false");
                }
            }
        });

        // Local view switching for /librarian (dashboard, books, settings)
        const localViews = document.querySelectorAll(".view");
        if (localViews.length > 0) {
            function activateView(viewName) {
                if (!viewName) viewName = "dashboard";
                const targetView = document.getElementById(viewName);
                if (!targetView) return false;

                localViews.forEach(v => v.classList.remove("active"));
                targetView.classList.add("active");

                const navLinks = document.querySelectorAll(".librarian-nav-link");
                navLinks.forEach(link => {
                    const dv = link.getAttribute("data-view");
                    if (dv === viewName) {
                        link.classList.add("active");
                    } else if (dv) {
                        link.classList.remove("active");
                    }
                });

                const pageTitle = document.getElementById("page-title");
                if (pageTitle) {
                    if (viewName === "books") pageTitle.textContent = "Manage Books";
                    else if (viewName === "settings") pageTitle.textContent = "Settings";
                    else pageTitle.textContent = "Dashboard";
                }
                return true;
            }

            // Listen for clicks on data-view links
            document.querySelectorAll(".librarian-nav-link[data-view]").forEach(link => {
                link.addEventListener("click", function (e) {
                    const dv = this.getAttribute("data-view");
                    if (dv && activateView(dv)) {
                        history.pushState(null, "", dv === "dashboard" ? "/librarian" : "#" + dv);
                        if (navMenu) navMenu.classList.remove("mobile-open");
                    }
                });
            });

            // Activate initial view from hash or default to dashboard
            function handleHashChange() {
                const hash = window.location.hash ? window.location.hash.substring(1) : "dashboard";
                activateView(hash);
            }

            window.addEventListener("hashchange", handleHashChange);
            if (window.location.hash) {
                handleHashChange();
            }
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initLibrarianNav);
    } else {
        initLibrarianNav();
    }
})();
