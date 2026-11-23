/**
 * LIBRA LMS — Librarian Navigation Component
 * Handles profile menu, mobile drawer, theme toggling, and local view routing on /librarian.
 */
(function () {
    "use strict";

    // ----------------------------------------------------
    // INK THEME TRANSITION (Organic bleeding ink reveal)
    // ----------------------------------------------------
    var DURATION = 3200; // ms (slow, noticeable organic ink bleed reveal)
    var RAGGED = 0.35;   // 0 = clean circle, higher = wilder edge
    var POINTS = 220;    // polygon resolution

    function makeNoise() {
        var h = [];
        for (var k = 2; k <= 16; k++) {
            h.push({ k: k, a: 1 / Math.pow(k, 0.8), ph: Math.random() * 6.283, sp: (Math.random() - 0.5) * 6 });
        }
        var norm = h.reduce(function (s, x) { return s + x.a; }, 0);
        return function (theta, t) {
            return h.reduce(function (s, x) { return s + x.a * Math.sin(x.k * theta + x.ph + x.sp * t); }, 0) / norm;
        };
    }

    function inkPolygon(cx, cy, r, noise, t) {
        var pts = [];
        for (var i = 0; i < POINTS; i++) {
            var th = (i / POINTS) * Math.PI * 2;
            var rr = Math.max(0, r * (1 + RAGGED * noise(th, t)));
            pts.push((cx + rr * Math.cos(th)).toFixed(1) + "px " + (cy + rr * Math.sin(th)).toFixed(1) + "px");
        }
        return "polygon(" + pts.join(",") + ")";
    }

    var ease = function (t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    };

    function applyLibrarianTheme(theme, persist) {
        const root = document.documentElement;
        root.dataset.theme = theme;
        root.setAttribute("data-theme", theme);
        const isLight = theme === "light";
        if (document.body) document.body.classList.toggle("light", isLight);
        root.classList.toggle("light", isLight);

        document.querySelectorAll("#theme-toggle, #theme-toggle-settings").forEach(btn => {
            btn.textContent = isLight ? "🌙 Dark mode" : "☀️ Light mode";
        });
        localStorage.setItem("theme", theme);
        localStorage.setItem("libra-theme", theme);

        if (persist) {
            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');
            const headers = { "Content-Type": "application/x-www-form-urlencoded" };
            if (tokenMeta && headerMeta) {
                headers[headerMeta.getAttribute("content")] = tokenMeta.getAttribute("content");
            }
            fetch("/api/settings/theme", {
                method: "POST",
                headers: headers,
                body: "theme=" + encodeURIComponent(theme)
            }).catch(() => {});
        }
    }

    let isTransitioning = false;
    async function toggleLibrarianTheme(event) {
        if (isTransitioning) return;
        const root = document.documentElement;
        const isCurrentlyLight = root.dataset.theme === "light" ||
                                 root.classList.contains("light") ||
                                 (document.body && document.body.classList.contains("light"));
        const nextTheme = isCurrentlyLight ? "dark" : "light";

        const btn = (event && event.currentTarget) ||
                    (event && event.target && event.target.closest("button")) ||
                    document.getElementById("themeBtn") ||
                    document.getElementById("theme-toggle-btn") ||
                    document.getElementById("theme-toggle");
        const b = btn ? btn.getBoundingClientRect() : null;
        const cx = (event && typeof event.clientX === "number" && event.clientX > 0) ? event.clientX : (b ? b.left + b.width / 2 : window.innerWidth - 80);
        const cy = (event && typeof event.clientY === "number" && event.clientY > 0) ? event.clientY : (b ? b.top + b.height / 2 : 36);

        const far = Math.hypot(Math.max(cx, window.innerWidth - cx), Math.max(cy, window.innerHeight - cy));
        const rMax = (far / (1 - RAGGED)) * 1.05;
        const noise = makeNoise();

        isTransitioning = true;

        if (document.startViewTransition) {
            try {
                root.style.setProperty("--vt-duration", (DURATION / 1000) + "s");
                const vt = document.startViewTransition(() => {
                    applyLibrarianTheme(nextTheme, true);
                });
                await vt.ready;
                const start = performance.now();
                await new Promise((resolve) => {
                    function frame(now) {
                        const t = Math.min((now - start) / DURATION, 1);
                        root.style.setProperty("--ink-clip", inkPolygon(cx, cy, rMax * ease(t), noise, t));
                        if (t < 1) {
                            requestAnimationFrame(frame);
                        } else {
                            resolve();
                        }
                    }
                    requestAnimationFrame(frame);
                });
                await vt.finished;
            } catch (err) {
                console.warn("Librarian theme transition error", err);
                applyLibrarianTheme(nextTheme, true);
            } finally {
                root.style.removeProperty("--ink-clip");
                root.style.removeProperty("--vt-duration");
                isTransitioning = false;
            }
        } else {
            // Universal organic bleeding ink ripple overlay for Firefox, Safari & unsupported browsers
            try {
                const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svg.id = "ink-theme-overlay";
                svg.setAttribute("style", "position:fixed;inset:0;width:100vw;height:100vh;z-index:999999;pointer-events:none;overflow:hidden;");
                const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
                const nextBg = nextTheme === "light" ? "#f4f1eb" : "#0d0f14";
                path.setAttribute("fill", nextBg);
                svg.appendChild(path);
                document.body.appendChild(svg);

                const startFallback = performance.now();
                let switched = false;

                await new Promise((resolve) => {
                    function frameFallback(now) {
                        const t = Math.min((now - startFallback) / DURATION, 1);
                        const r = rMax * ease(t);
                        const pts = [];
                        for (let i = 0; i < POINTS; i++) {
                            const th = (i / POINTS) * Math.PI * 2;
                            const rr = Math.max(0, r * (1 + RAGGED * noise(th, t)));
                            pts.push((cx + rr * Math.cos(th)).toFixed(1) + "," + (cy + rr * Math.sin(th)).toFixed(1));
                        }
                        path.setAttribute("d", "M" + pts.join(" L") + " Z");

                        if (t >= 0.42 && !switched) {
                            switched = true;
                            applyLibrarianTheme(nextTheme, true);
                        }

                        if (t < 1) {
                            requestAnimationFrame(frameFallback);
                        } else {
                            resolve();
                        }
                    }
                    requestAnimationFrame(frameFallback);
                });

                if (!switched) applyLibrarianTheme(nextTheme, true);
                svg.style.transition = "opacity 0.2s ease";
                svg.style.opacity = "0";
                setTimeout(() => {
                    if (svg.parentNode) svg.parentNode.removeChild(svg);
                }, 220);
            } catch (fallbackErr) {
                console.warn("Librarian ink fallback error", fallbackErr);
                applyLibrarianTheme(nextTheme, true);
            } finally {
                isTransitioning = false;
            }
        }
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
        applyLibrarianTheme(savedTheme, false);

        // Theme button click listeners (navbar + settings view)
        document.querySelectorAll("#themeBtn, #theme-toggle-btn, #theme-toggle, #theme-toggle-settings").forEach(btn => {
            btn.addEventListener("click", function (e) {
                if (typeof window.toggleLibraTheme === "function") {
                    window.toggleLibraTheme(e);
                } else {
                    toggleLibrarianTheme(e);
                }
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

    function initGlobalCursor() {
        if (window.matchMedia && window.matchMedia('(pointer: coarse)').matches) return;
        if (window.__libraGlobalCursorInitialized) return;
        window.__libraGlobalCursorInitialized = true;

        var dot = document.getElementById('cursor');
        if (!dot) {
            dot = document.createElement('div');
            dot.id = 'cursor';
            dot.className = 'cur-dot';
            dot.setAttribute('aria-hidden', 'true');
            document.body.appendChild(dot);
        }

        var x = window.innerWidth / 2, y = window.innerHeight / 2;
        var tx = x, ty = y;

        window.addEventListener('pointermove', function (e) {
            tx = e.clientX;
            ty = e.clientY;
        }, { passive: true });

        var interactiveSelectors = 'a, button, input, select, textarea, [role="button"], [data-cursor], .card, .chip, .hero-pill, .nav-link, .btn, .les, .profile-trigger, .librarian-nav-link, .stat-card, tr, .tab-btn';

        document.addEventListener('mouseover', function (e) {
            if (e.target && e.target.closest && e.target.closest(interactiveSelectors)) {
                dot.classList.add('act');
            }
        }, { passive: true });

        document.addEventListener('mouseout', function (e) {
            if (e.target && e.target.closest && e.target.closest(interactiveSelectors)) {
                dot.classList.remove('act');
            }
        }, { passive: true });

        function renderCursor() {
            x += (tx - x) * 0.18;
            y += (ty - y) * 0.18;
            dot.style.transform = 'translate3d(' + x.toFixed(1) + 'px,' + y.toFixed(1) + 'px,0)';
            requestAnimationFrame(renderCursor);
        }
        requestAnimationFrame(renderCursor);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function() {
            initLibrarianNav();
            initGlobalCursor();
        });
    } else {
        initLibrarianNav();
        initGlobalCursor();
    }
})();
