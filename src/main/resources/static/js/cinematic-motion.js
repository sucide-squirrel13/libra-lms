/**
 * LIBRA LMS — Unified Motion & GSAP ScrollTrigger Choreography
 * Editorial Minimalism · Architectural Depth · Restrained Modern Interaction
 * Orchestrates real GSAP ScrollTrigger animations, desktop Lenis smooth inertia,
 * and pinned architectural chapter tracking with strict accessibility fallbacks.
 */
(function () {
    "use strict";

    // Global Refresh Helper
    window.refreshLibraScroll = function () {
        if (typeof window.ScrollTrigger !== "undefined") {
            window.ScrollTrigger.refresh();
        }
    };

    // 1. Accessibility Pre-flight: Respect Reduced Motion Preferences
    const prefersReducedMotion =
        window.matchMedia("(prefers-reduced-motion: reduce)").matches ||
        document.documentElement.getAttribute("data-motion") === "reduce" ||
        document.documentElement.getAttribute("data-reduced-motion") === "true";

    if (prefersReducedMotion) {
        // Ensure all potential animation targets are immediately visible
        const staticTargets = document.querySelectorAll(
            ".cinematic-hero, .vantage-hero, .editorial-hero, .editorial-hero-showcase, .catalogue-hero, .borrowings-hero, " +
            ".vantage-eyebrow, .editorial-eyebrow, .vantage-hero-title, .editorial-hero-title, .hero-title-line .line-inner, .vantage-title-line .line-inner, " +
            ".vantage-hero-desc, .editorial-hero-description, .vantage-hero-actions, .editorial-hero-ctas, .btn-hero-primary, .btn-hero-secondary, " +
            ".vantage-glass-card, .archival-folio-card, .vantage-hero-meta, .editorial-hero-meta, " +
            ".archival-book-card, .pillar-folio-card, .pinned-narrative-card, .discipline-card, .feature-arch-card, " +
            ".settings-header, .stat-card, .kpi-card, .stat-box, .metric-card, " +
            ".book-card, .book-showcase-card, .catalogue-card, .featured-card, " +
            ".dashboard-section, .table-card, .table-wrapper, .table-container, " +
            ".settings-section, .pull-cord-lamp-card, .panel, .welcome, footer, .cinematic-footer"
        );
        staticTargets.forEach(function (el) {
            el.style.opacity = "1";
            el.style.transform = "none";
        });
        const pillarCards = document.querySelectorAll(".pillar-folio-card");
        pillarCards.forEach(function (p) { p.classList.add("active"); });
        return; // Bypasses all script-driven motion
    }

    // 2. Prevent Duplicate Initialization
    if (window.__libraMotionInitialized) {
        if (typeof window.ScrollTrigger !== "undefined") {
            window.ScrollTrigger.refresh();
        }
        return;
    }
    window.__libraMotionInitialized = true;

    // 3. Core Libraries Check & Registration
    const hasGsap = typeof window.gsap !== "undefined";
    const hasScrollTrigger = typeof window.ScrollTrigger !== "undefined";

    if (hasGsap && hasScrollTrigger) {
        window.gsap.registerPlugin(window.ScrollTrigger);
    }

    // 4. Initialize Lenis Smooth Scrolling (Desktop only, fallback seamlessly)
    function initLenis() {
        if (typeof window.Lenis !== "function" || window.innerWidth <= 992) {
            return null; // Keep mobile/tablet touch gesture scrolling 100% native
        }

        try {
            const lenis = new window.Lenis({
                duration: 1.15,
                easing: function (t) {
                    return Math.min(1, 1.001 - Math.pow(2, -10 * t));
                },
                smoothWheel: true,
                wheelMultiplier: 0.88,
                touchMultiplier: 1.0,
                infinite: false
            });

            // Synchronize Lenis with GSAP ScrollTrigger
            if (hasGsap && hasScrollTrigger) {
                lenis.on("scroll", window.ScrollTrigger.update);

                window.gsap.ticker.add(function (time) {
                    lenis.raf(time * 1000);
                });

                window.gsap.ticker.lagSmoothing(0);
            } else {
                function raf(time) {
                    lenis.raf(time);
                    requestAnimationFrame(raf);
                }
                requestAnimationFrame(raf);
            }

            window.lenis = lenis;
            return lenis;
        } catch (e) {
            console.warn("LIBRA: Lenis initialization skipped, falling back to native scroll.", e);
            return null;
        }
    }

    // 5. GSAP ScrollTrigger Choreography
    function initScrollTriggerAnimations() {
        if (!hasGsap || !hasScrollTrigger) return;

        const gsap = window.gsap;

        // A. Hero Page Entrance Timeline (Page-Load Stage)
        const editorialHero = document.querySelector(".editorial-hero-showcase") || document.querySelector(".vantage-hero");
        if (editorialHero && !prefersReducedMotion) {
            const heroTl = gsap.timeline({
                defaults: { ease: "power3.out" },
                delay: 0.08
            });

            // 1. Background atmospheric reveal (gentle zoom-out and fade-in)
            const bgSelector = document.querySelector(".editorial-hero-backdrop") ? ".editorial-hero-backdrop" : ".vantage-hero-bg";
            if (document.querySelector(bgSelector)) {
                heroTl.fromTo(
                    bgSelector,
                    { opacity: 0, scale: 1.04 },
                    { opacity: 1, scale: 1, duration: 1.2, ease: "power2.out" }
                );
            }

            // 2. Navbar fade and slight upward movement
            heroTl.fromTo(
                "header.navbar, .navbar",
                { opacity: 0, y: -14 },
                { opacity: 1, y: 0, duration: 0.7, ease: "power2.out", clearProps: "transform" },
                "-=0.9"
            );

            // 3. Eyebrow badge
            const eyebrowSelector = document.querySelector(".editorial-eyebrow") ? ".editorial-eyebrow" : ".vantage-eyebrow";
            if (document.querySelector(eyebrowSelector)) {
                heroTl.fromTo(
                    eyebrowSelector,
                    { opacity: 0, y: 14 },
                    { opacity: 1, y: 0, duration: 0.5 },
                    "-=0.55"
                );
            }

            // 4. Headline line-by-line reveal
            const titleLines = document.querySelectorAll(".hero-title-line .line-inner, .vantage-title-line .line-inner");
            if (titleLines.length > 0) {
                heroTl.fromTo(
                    titleLines,
                    { y: "110%", opacity: 0 },
                    { y: "0%", opacity: 1, stagger: 0.1, duration: 0.8, ease: "power4.out" },
                    "-=0.4"
                );
            }

            // 5. Supporting copy fade-in
            const descSelector = document.querySelector(".editorial-hero-description") ? ".editorial-hero-description" : ".vantage-hero-desc";
            if (document.querySelector(descSelector)) {
                heroTl.fromTo(
                    descSelector,
                    { opacity: 0, y: 16 },
                    { opacity: 1, y: 0, duration: 0.6 },
                    "-=0.35"
                );
            }

            // 6. Action buttons entrance
            const ctaButtons = document.querySelectorAll(".editorial-hero-ctas a, .vantage-hero-actions .btn-vantage");
            if (ctaButtons.length > 0) {
                heroTl.fromTo(
                    ctaButtons,
                    { opacity: 0, y: 16, scale: 0.97 },
                    { opacity: 1, y: 0, scale: 1, stagger: 0.08, duration: 0.55 },
                    "-=0.35"
                );
            }

            // 7. Navigation & Telemetry meta strip
            const metaStrip = document.querySelector(".editorial-hero-meta") || document.querySelector(".vantage-hero-meta");
            if (metaStrip) {
                heroTl.fromTo(
                    metaStrip,
                    { opacity: 0, y: 10 },
                    { opacity: 1, y: 0, duration: 0.45 },
                    "-=0.25"
                );
            }

            // 8. Featured archival folio entrance
            const folioCard = document.querySelector(".archival-folio-card") || document.querySelector(".vantage-glass-card");
            if (folioCard) {
                heroTl.fromTo(
                    folioCard,
                    { opacity: 0, y: 28, scale: 0.96 },
                    { opacity: 1, y: 0, scale: 1, duration: 0.9, ease: "power3.out" },
                    "-=0.6"
                );
            }
        }

        // B. Hero Background Parallax Scroll Scrub
        const heroBg = document.querySelector(".editorial-hero-backdrop");
        if (heroBg) {
            gsap.to(heroBg, {
                yPercent: 16,
                ease: "none",
                scrollTrigger: {
                    trigger: ".editorial-hero-showcase",
                    start: "top top",
                    end: "bottom top",
                    scrub: 1.2
                }
            });
        }

        // C. Editorial Intro Section Reveal
        const introSection = document.querySelector(".editorial-intro-section");
        if (introSection) {
            gsap.from(introSection, {
                scrollTrigger: {
                    trigger: introSection,
                    start: "top 88%",
                    once: true
                },
                opacity: 0,
                y: 28,
                duration: 0.8,
                ease: "power2.out",
                clearProps: "transform,opacity"
            });
        }

        // D. Staggered Book Cards Reveal in Master Archive Collection
        const exhibitionGrid = document.querySelector(".exhibition-grid");
        if (exhibitionGrid) {
            const bookCards = exhibitionGrid.querySelectorAll(".archival-book-card, .book-showcase-card");
            if (bookCards.length > 0) {
                gsap.from(bookCards, {
                    scrollTrigger: {
                        trigger: exhibitionGrid,
                        start: "top 85%",
                        once: true
                    },
                    opacity: 0,
                    y: 32,
                    scale: 0.96,
                    stagger: 0.09,
                    duration: 0.75,
                    ease: "power2.out",
                    clearProps: "transform,scale,opacity"
                });
            }
        }

        // E. Discipline Pathways Cards Reveal
        const discoveryGrid = document.querySelector(".discovery-grid");
        if (discoveryGrid) {
            const disciplineCards = discoveryGrid.querySelectorAll(".discipline-card");
            if (disciplineCards.length > 0) {
                gsap.from(disciplineCards, {
                    scrollTrigger: {
                        trigger: discoveryGrid,
                        start: "top 85%",
                        once: true
                    },
                    opacity: 0,
                    y: 24,
                    stagger: 0.1,
                    duration: 0.7,
                    ease: "power2.out",
                    clearProps: "transform,opacity"
                });
            }
        }

        // F. PINNED ARCHITECTURAL SHOWCASE: Institutional Principles
        const pinnedSection = document.querySelector(".pinned-principles-section");
        const pinnedNarrative = document.querySelector(".pinned-narrative-card");
        const pillarCards = document.querySelectorAll(".pillar-folio-card");
        const chapterSteps = document.querySelectorAll(".chapter-step");
        const progressBar = document.getElementById("pinned-progress-bar");

        if (pinnedSection && pillarCards.length > 0) {
            // 1. Desktop Pinning Behavior (Width > 992px)
            if (window.innerWidth > 992 && pinnedNarrative) {
                ScrollTrigger.create({
                    trigger: pinnedSection,
                    start: "top 100px",
                    end: "bottom bottom",
                    pin: pinnedNarrative,
                    pinSpacing: false
                });
            }

            // 2. Active Pillar Tracking & Chapter Indicator Sync
            pillarCards.forEach(function (card, index) {
                ScrollTrigger.create({
                    trigger: card,
                    start: "top 55%",
                    end: "bottom 45%",
                    onEnter: function () {
                        updateActivePillar(index);
                    },
                    onEnterBack: function () {
                        updateActivePillar(index);
                    }
                });

                // Subtle Entrance Drift for each pillar
                gsap.from(card, {
                    scrollTrigger: {
                        trigger: card,
                        start: "top 85%",
                        once: true
                    },
                    opacity: 0,
                    y: 24,
                    duration: 0.65,
                    ease: "power2.out",
                    clearProps: "transform"
                });
            });

            function updateActivePillar(index) {
                pillarCards.forEach(function (p, i) {
                    p.classList.toggle("active", i === index);
                });
                chapterSteps.forEach(function (s, i) {
                    s.classList.toggle("active", i === index);
                });
                if (progressBar) {
                    const pct = ((index + 1) / pillarCards.length) * 100;
                    progressBar.style.width = pct + "%";
                }
            }
        }

        // G. System Capabilities (Features) Section Reveal
        const featuresGrid = document.querySelector(".features-grid");
        if (featuresGrid) {
            const featureCards = featuresGrid.querySelectorAll(".feature-arch-card");
            if (featureCards.length > 0) {
                gsap.from(featureCards, {
                    scrollTrigger: {
                        trigger: featuresGrid,
                        start: "top 85%",
                        once: true
                    },
                    opacity: 0,
                    y: 24,
                    stagger: 0.1,
                    duration: 0.7,
                    ease: "power2.out",
                    clearProps: "transform,opacity"
                });
            }
        }

        // H. Statistics Cards & Animated Count-Up on Viewport Entry
        const statGrids = document.querySelectorAll(
            ".stats, ".concat(".stats-grid, .kpi-grid, .grid-4, .hero-stats-row, .borrowing-stats")
        );
        statGrids.forEach(function (grid) {
            const statCards = grid.querySelectorAll(".stat-card, .kpi-card, .editorial-card, .stat-card-box");
            if (statCards.length > 0) {
                gsap.from(statCards, {
                    scrollTrigger: {
                        trigger: grid,
                        start: "top 85%",
                        once: true
                    },
                    opacity: 0,
                    y: 26,
                    scale: 0.97,
                    stagger: 0.08,
                    duration: 0.65,
                    ease: "power2.out",
                    clearProps: "transform,scale,opacity"
                });
            }
        });

        const statNumberElements = document.querySelectorAll(
            ".stat-number, .kpi-number, .stat-box-val, .metric-number, .stat-val"
        );
        statNumberElements.forEach(function (el) {
            const raw = el.textContent.trim();
            const finalVal = parseInt(raw, 10);
            if (!isNaN(finalVal) && finalVal >= 0) {
                const card = el.closest(".stat-card, .kpi-card, .editorial-card, .stat-card-box") || el;
                const counter = { val: 0 };
                gsap.to(counter, {
                    val: finalVal,
                    duration: 1.3,
                    ease: "power2.out",
                    scrollTrigger: {
                        trigger: card,
                        start: "top 88%",
                        once: true
                    },
                    onStart: function () {
                        el.textContent = "0";
                    },
                    onUpdate: function () {
                        el.textContent = Math.round(counter.val).toLocaleString();
                    },
                    onComplete: function () {
                        el.textContent = finalVal.toLocaleString();
                    }
                });
            }
        });

        // I. Invitation Call-to-Action Banner Reveal
        const invitationBanner = document.querySelector(".invitation-banner");
        if (invitationBanner) {
            gsap.from(invitationBanner, {
                scrollTrigger: {
                    trigger: ".invitation-section",
                    start: "top 85%",
                    once: true
                },
                opacity: 0,
                y: 26,
                duration: 0.8,
                ease: "power2.out",
                clearProps: "transform,opacity"
            });
        }

        // J. Refined Editorial Footer Reveal
        const footer = document.querySelector(".cinematic-footer");
        if (footer) {
            gsap.from(footer, {
                scrollTrigger: {
                    trigger: footer,
                    start: "top 95%",
                    once: true
                },
                opacity: 0,
                y: 20,
                duration: 0.75,
                ease: "power2.out",
                clearProps: "transform,opacity"
            });
        }

        // K. Refresh coordinates
        window.ScrollTrigger.refresh();
    }

    // 6. Interactive Card Subtle Hover Physics (Desktop Only)
    function initInteractive3DTilt() {
        if (window.innerWidth < 768) return;

        const cards = document.querySelectorAll(
            ".archival-book-card, .archival-folio-card, .catalogue-card, .discipline-card"
        );
        cards.forEach(function (card) {
            card.addEventListener("mousemove", function (e) {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;

                const rotateX = ((y - centerY) / centerY) * -2.5;
                const rotateY = ((x - centerX) / centerX) * 2.5;

                card.style.transform = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) translateY(-3px)`;
            });

            card.addEventListener("mouseleave", function () {
                card.style.transform = "";
            });
        });
    }

    // 7. Event Listeners for Dynamic Layout & Content Changes
    window.addEventListener("load", function () {
        if (hasScrollTrigger) window.ScrollTrigger.refresh();
    });

    window.addEventListener("resize", function () {
        if (hasScrollTrigger) window.ScrollTrigger.refresh();
    });

    window.addEventListener("libra:tab-change", function () {
        if (hasScrollTrigger) {
            setTimeout(function () {
                window.ScrollTrigger.refresh();
            }, 60);
        }
    });

    // 8. Synchronized Startup
    function init() {
        initLenis();
        initScrollTriggerAnimations();
        initInteractive3DTilt();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
