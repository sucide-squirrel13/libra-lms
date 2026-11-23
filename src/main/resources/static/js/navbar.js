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

    var applyThemeDOM = function (nextTheme) {
        var root = document.documentElement;
        root.setAttribute("data-theme", nextTheme);
        root.dataset.theme = nextTheme;
        if (nextTheme === "light") {
            root.classList.add("light");
            if (document.body) document.body.classList.add("light");
        } else {
            root.classList.remove("light");
            if (document.body) document.body.classList.remove("light");
        }
        try {
            localStorage.setItem("theme", nextTheme);
            localStorage.setItem("libra-theme", nextTheme);
        } catch (e) {}

        var tokenMeta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        var headers = { "Content-Type": "application/x-www-form-urlencoded" };
        if (tokenMeta && headerMeta) {
            headers[headerMeta.getAttribute("content")] = tokenMeta.getAttribute("content");
        }
        fetch("/api/settings/theme", {
            method: "POST",
            headers: headers,
            body: "theme=" + encodeURIComponent(nextTheme)
        }).catch(function () {});

        window.dispatchEvent(new CustomEvent("libra-theme-changed", { detail: { theme: nextTheme } }));
    };

    var isTransitioning = false;
    window.toggleLibraTheme = async function (event) {
        if (isTransitioning) return;
        var root = document.documentElement;
        var isCurrentlyLight = root.dataset.theme === "light" ||
                               root.classList.contains("light") ||
                               (document.body && document.body.classList.contains("light"));
        var nextTheme = isCurrentlyLight ? "dark" : "light";

        var btn = (event && event.currentTarget) ||
                  (event && event.target && event.target.closest("button")) ||
                  document.getElementById("themeBtn") ||
                  document.getElementById("theme-toggle-btn") ||
                  document.getElementById("theme-toggle");
        var b = btn ? btn.getBoundingClientRect() : null;
        var cx = (event && typeof event.clientX === "number" && event.clientX > 0) ? event.clientX : (b ? b.left + b.width / 2 : window.innerWidth - 80);
        var cy = (event && typeof event.clientY === "number" && event.clientY > 0) ? event.clientY : (b ? b.top + b.height / 2 : 36);

        // farthest corner, scaled so even the lowest wobble still covers the screen
        var far = Math.hypot(Math.max(cx, window.innerWidth - cx), Math.max(cy, window.innerHeight - cy));
        var rMax = (far / (1 - RAGGED)) * 1.05;
        var noise = makeNoise();

        isTransitioning = true;

        if (document.startViewTransition) {
            try {
                root.style.setProperty("--vt-duration", (DURATION / 1000) + "s");
                var vt = document.startViewTransition(function () {
                    applyThemeDOM(nextTheme);
                });
                await vt.ready;
                var start = performance.now();
                await new Promise(function (resolve) {
                    function frame(now) {
                        var t = Math.min((now - start) / DURATION, 1);
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
                console.warn("Theme transition error", err);
                applyThemeDOM(nextTheme);
            } finally {
                root.style.removeProperty("--ink-clip");
                root.style.removeProperty("--vt-duration");
                isTransitioning = false;
            }
        } else {
            // Universal organic bleeding ink ripple overlay for Firefox, Safari & unsupported browsers
            try {
                var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svg.id = "ink-theme-overlay";
                svg.setAttribute("style", "position:fixed;inset:0;width:100vw;height:100vh;z-index:999999;pointer-events:none;overflow:hidden;");
                var path = document.createElementNS("http://www.w3.org/2000/svg", "path");
                var nextBg = nextTheme === "light" ? "#f4f1eb" : "#0d0f14";
                path.setAttribute("fill", nextBg);
                svg.appendChild(path);
                document.body.appendChild(svg);

                var startFallback = performance.now();
                var switched = false;

                await new Promise(function (resolve) {
                    function frameFallback(now) {
                        var t = Math.min((now - startFallback) / DURATION, 1);
                        var r = rMax * ease(t);
                        var pts = [];
                        for (var i = 0; i < POINTS; i++) {
                            var th = (i / POINTS) * Math.PI * 2;
                            var rr = Math.max(0, r * (1 + RAGGED * noise(th, t)));
                            pts.push((cx + rr * Math.cos(th)).toFixed(1) + "," + (cy + rr * Math.sin(th)).toFixed(1));
                        }
                        path.setAttribute("d", "M" + pts.join(" L") + " Z");

                        if (t >= 0.42 && !switched) {
                            switched = true;
                            applyThemeDOM(nextTheme);
                        }

                        if (t < 1) {
                            requestAnimationFrame(frameFallback);
                        } else {
                            resolve();
                        }
                    }
                    requestAnimationFrame(frameFallback);
                });

                if (!switched) applyThemeDOM(nextTheme);
                svg.style.transition = "opacity 0.2s ease";
                svg.style.opacity = "0";
                setTimeout(function () {
                    if (svg.parentNode) svg.parentNode.removeChild(svg);
                }, 220);
            } catch (fallbackErr) {
                console.warn("Ink fallback error", fallbackErr);
                applyThemeDOM(nextTheme);
            } finally {
                isTransitioning = false;
            }
        }
    };

    function initNavbar() {
        // Synchronize local theme preference across all student & shared pages
        var savedTheme = localStorage.getItem("libra-theme");
        var docTheme = document.documentElement.getAttribute("data-theme");
        var activeTheme = docTheme || savedTheme || "dark";
        if (activeTheme === "light") {
            document.body.classList.add("light");
        } else if (activeTheme === "dark") {
            document.body.classList.remove("light");
        }

        var trigger = document.getElementById("profile-trigger");
        var menu = document.getElementById("profile-menu");
        var mobileButton = document.getElementById("mobile-menu-button");
        var nav = document.getElementById("nav-right");
        var photoInput = document.getElementById("profile-photo");
        var photoForm = document.getElementById("profile-photo-form");
        var changePhotoButton = document.getElementById("change-photo-button");
        var themeToggleBtn = document.getElementById("themeBtn") || document.getElementById("theme-toggle-btn");

        if (themeToggleBtn) {
            themeToggleBtn.addEventListener("click", function (e) {
                window.toggleLibraTheme(e);
            });
        }

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

        // Initialize global editorial circle cursor on all pages
        initGlobalCursor();
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

        var interactiveSelectors = 'a, button, input, select, textarea, [role="button"], [data-cursor], .card, .chip, .hero-pill, .nav-link, .btn, .les, .profile-trigger, .mode-button, .reading-lamp-stage, .quick-nav-pill, .filter-chip';

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
        document.addEventListener("DOMContentLoaded", initNavbar);
    } else {
        initNavbar();
    }
})();