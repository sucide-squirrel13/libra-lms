/**
 * LIBRA LMS — Floating Reading Lamp & Theme Controller
 * Dark Academia Aesthetics: Midnight Archive & Lavender Parchment
 * Smooth, restrained tactile pull cord with spring recoil, keyboard, and touch accessibility.
 */
(function () {
    "use strict";

    function getCsrfInfo() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        return {
            token: tokenMeta ? tokenMeta.getAttribute("content") : "",
            header: headerMeta ? headerMeta.getAttribute("content") : "X-CSRF-TOKEN"
        };
    }

    // Application-wide Theme Synchronizer
    window.applyLibraTheme = function (theme, persist) {
        const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        let effectiveTheme = theme;
        if (theme === "system" || !theme) {
            effectiveTheme = prefersDark ? "dark" : "light";
        }
        const isLight = effectiveTheme === "light";

        // Update root attributes and classes
        document.documentElement.setAttribute("data-theme", effectiveTheme);
        document.body.classList.toggle("light", isLight);
        document.body.classList.toggle("dark", !isLight);

        // Update Floating Reading Lamp UI
        const lampStage = document.getElementById("lamp-stage");
        if (lampStage) {
            lampStage.classList.toggle("lamp-on", isLight);
            lampStage.classList.toggle("lamp-off", !isLight);
            lampStage.setAttribute("aria-checked", String(isLight));
        }

        // Update Theme Pill Buttons (e.g. in Settings)
        document.querySelectorAll(".theme-pill-btn").forEach(btn => {
            const btnTheme = btn.getAttribute("data-theme-val");
            const active = btnTheme === theme || (btnTheme === "system" && theme === "system");
            btn.classList.toggle("active", active);
            btn.setAttribute("aria-pressed", String(active));
        });

        // Update classic text toggles if any exist
        document.querySelectorAll("#theme-toggle, #theme-toggle-settings").forEach(btn => {
            btn.textContent = isLight ? "🌙 Dark mode" : "☀️ Light mode";
        });

        // Cache in localStorage
        localStorage.setItem("libra-theme", theme);

        // Backend Persistence via AJAX
        if (persist !== false) {
            const csrf = getCsrfInfo();
            const headers = { "Content-Type": "application/x-www-form-urlencoded" };
            if (csrf.token) {
                headers[csrf.header] = csrf.token;
            }

            fetch("/api/settings/theme", {
                method: "POST",
                headers: headers,
                body: "theme=" + encodeURIComponent(theme)
            }).catch(() => {
                // Ignore background network errors
            });
        }

        // Global Event for Lenis/ScrollTrigger or dynamic canvas refresh
        window.dispatchEvent(new CustomEvent("libra:theme-change", { detail: { theme: effectiveTheme } }));
    };

    window.toggleLibraTheme = function () {
        const currentTheme = document.documentElement.getAttribute("data-theme") || "dark";
        const nextTheme = currentTheme === "light" ? "dark" : "light";
        window.applyLibraTheme(nextTheme, true);
    };

    function initReadingLamp() {
        const floatingDock = document.getElementById("floating-reading-lamp-dock") || document.querySelector(".floating-reading-lamp-dock");
        if (floatingDock && floatingDock.parentElement !== document.body) {
            document.body.appendChild(floatingDock);
        }

        const lampStage = document.getElementById("lamp-stage");
        if (!lampStage) return;

        const cordLine = document.getElementById("lamp-cord-line");
        const fobGroup = document.getElementById("lamp-pull-fob-group");
        const REST_Y = 108;
        const MAX_PULL = 24;
        const THRESHOLD = 14;

        let isDragging = false;
        let startY = 0;
        let currentPull = 0;

        function setPull(pullPx) {
            currentPull = pullPx;
            const targetY = REST_Y + pullPx;
            if (cordLine) cordLine.setAttribute("y2", String(targetY));
            if (fobGroup) fobGroup.setAttribute("transform", `translate(28, ${targetY})`);
        }

        function triggerRecoil(andToggle) {
            lampStage.classList.add("is-dragging");
            // Pull down briefly
            setPull(MAX_PULL);

            setTimeout(() => {
                // Snap back with spring
                setPull(0);
                lampStage.classList.remove("is-dragging");
                if (andToggle) {
                    window.toggleLibraTheme();
                }
            }, 120);
        }

        // Pointer Drag Events
        lampStage.addEventListener("pointerdown", function (e) {
            isDragging = true;
            startY = e.clientY;
            currentPull = 0;
            lampStage.classList.add("is-dragging");
            lampStage.setPointerCapture(e.pointerId);
        });

        lampStage.addEventListener("pointermove", function (e) {
            if (!isDragging) return;
            const deltaY = e.clientY - startY;
            if (deltaY > 0) {
                const pull = Math.min(MAX_PULL, deltaY * 0.7);
                setPull(pull);
            } else {
                setPull(0);
            }
        });

        function handlePointerUp(e) {
            if (!isDragging) return;
            isDragging = false;
            lampStage.classList.remove("is-dragging");
            try {
                lampStage.releasePointerCapture(e.pointerId);
            } catch (err) {}

            const shouldToggle = currentPull >= THRESHOLD || currentPull === 0;
            setPull(0);
            if (shouldToggle) {
                window.toggleLibraTheme();
            }
        }

        lampStage.addEventListener("pointerup", handlePointerUp);
        lampStage.addEventListener("pointercancel", handlePointerUp);

        // Keyboard Accessibility (Enter / Space)
        lampStage.addEventListener("keydown", function (e) {
            if (e.key === "Enter" || e.key === " " || e.key === "Spacebar") {
                e.preventDefault();
                triggerRecoil(true);
            }
        });

        // Initialize Theme from localStorage or Server attribute
        const savedTheme = localStorage.getItem("libra-theme") ||
                            document.documentElement.getAttribute("data-theme") ||
                            "dark";
        window.applyLibraTheme(savedTheme, false);
    }

    // Initialize Pill Buttons (e.g. in Settings)
    function initThemePills() {
        document.querySelectorAll(".theme-pill-btn").forEach(btn => {
            btn.addEventListener("click", function () {
                const val = this.getAttribute("data-theme-val");
                if (val) {
                    window.applyLibraTheme(val, true);
                }
            });
        });
    }

    // System theme change listener
    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (e) => {
        const savedTheme = localStorage.getItem("libra-theme");
        if (savedTheme === "system") {
            window.applyLibraTheme("system", false);
        }
    });

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            initReadingLamp();
            initThemePills();
        });
    } else {
        initReadingLamp();
        initThemePills();
    }
})();
