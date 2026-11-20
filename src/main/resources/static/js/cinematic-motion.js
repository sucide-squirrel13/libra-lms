/**
 * LIBRA LMS — Cinematic Motion System ("Beyond the Pages")
 * Orchestrates GSAP ScrollTrigger choreography, 3D card tilt physics,
 * and optional Lenis smooth inertia with strict graceful fallbacks.
 */
(function () {
    "use strict";

    // 1. Accessibility pre-flight: Respect reduced motion preferences
    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (prefersReducedMotion) {
        return; // Retain native static styles without script-driven movement
    }

    // 2. Core Library Pre-flight: Register GSAP plugins early if available
    const hasGsap = typeof window.gsap !== "undefined";
    const hasScrollTrigger = typeof window.ScrollTrigger !== "undefined";

    if (hasGsap && hasScrollTrigger) {
        window.gsap.registerPlugin(window.ScrollTrigger);
    }

    // 3. Initialize Lenis Smooth Scrolling (Desktop only, fallback seamlessly)
    function initLenis() {
        if (typeof window.Lenis !== "function" || window.innerWidth <= 992) {
            return null; // Keep mobile/tablet touch gesture scrolling 100% native
        }

        try {
            const lenis = new window.Lenis({
                duration: 1.1,
                smoothWheel: true,
                wheelMultiplier: 0.9
            });

            // Synchronize with GSAP ticker & ScrollTrigger if present
            if (hasGsap && hasScrollTrigger) {
                lenis.on("scroll", window.ScrollTrigger.update);

                window.gsap.ticker.add(function (time) {
                    lenis.raf(time * 1000);
                });

                // Disable lag smoothing to prevent stutter on frame drops
                window.gsap.ticker.lagSmoothing(0);
            } else {
                // Fallback requestAnimationFrame loop when GSAP is not present
                function raf(time) {
                    lenis.raf(time);
                    requestAnimationFrame(raf);
                }
                requestAnimationFrame(raf);
            }

            // Expose globally for programmatic scroll controls or debugging
            window.lenis = lenis;
            return lenis;
        } catch (e) {
            console.warn("LIBRA: Lenis initialization skipped, falling back to native scroll.", e);
            return null;
        }
    }

    // 4. GSAP & ScrollTrigger Staged Motion Choreography
    function initGSAPAnimations() {
        if (!hasGsap) {
            return; // Graceful fallback: CSS renders fully visible content
        }

        const gsap = window.gsap;

        // Hero Entrance Sequence
        const heroTl = gsap.timeline({ defaults: { ease: "power3.out", duration: 0.9 } });
        heroTl
            .from(".cinematic-eyebrow", { opacity: 0, y: -16, duration: 0.6 })
            .from(".cinematic-hero-title", { opacity: 0, y: 24, duration: 0.8 }, "-=0.3")
            .from(".cinematic-hero-sub", { opacity: 0, y: 18, duration: 0.7 }, "-=0.5")
            .from(".cinematic-search-bar", { opacity: 0, y: 16, scale: 0.98, duration: 0.6 }, "-=0.4")
            .from(".cinematic-hero-pills .cinematic-pill", { opacity: 0, y: 10, stagger: 0.08, duration: 0.5 }, "-=0.3")
            .from(".cinematic-telemetry", { opacity: 0, y: 20, duration: 0.7 }, "-=0.4");

        // Scroll-Triggered Section Entrances (if ScrollTrigger is active)
        if (hasScrollTrigger) {
            // Editorial Quote Reveal
            gsap.from(".editorial-quote-box", {
                scrollTrigger: {
                    trigger: ".editorial-intro-section",
                    start: "top 80%"
                },
                opacity: 0,
                x: -30,
                duration: 0.9,
                ease: "power2.out"
            });

            gsap.from(".editorial-manifesto", {
                scrollTrigger: {
                    trigger: ".editorial-intro-section",
                    start: "top 80%"
                },
                opacity: 0,
                x: 30,
                duration: 0.9,
                ease: "power2.out"
            });

            // Exhibition Cards Stagger
            gsap.from(".book-showcase-card", {
                scrollTrigger: {
                    trigger: ".exhibition-grid",
                    start: "top 85%"
                },
                opacity: 0,
                y: 36,
                stagger: 0.12,
                duration: 0.8,
                ease: "power2.out"
            });

            // Discovery Grid Stagger
            gsap.from(".discipline-card", {
                scrollTrigger: {
                    trigger: ".discovery-grid",
                    start: "top 85%"
                },
                opacity: 0,
                y: 28,
                stagger: 0.1,
                duration: 0.75,
                ease: "power2.out"
            });

            // Storytelling Pillars Stagger
            gsap.from(".story-pillar-card", {
                scrollTrigger: {
                    trigger: ".storytelling-pillars",
                    start: "top 85%"
                },
                opacity: 0,
                y: 30,
                stagger: 0.14,
                duration: 0.8,
                ease: "power2.out"
            });

            // Features Grid Stagger
            gsap.from(".feature-arch-card", {
                scrollTrigger: {
                    trigger: ".features-grid",
                    start: "top 85%"
                },
                opacity: 0,
                y: 24,
                stagger: 0.12,
                duration: 0.75,
                ease: "power2.out"
            });

            // Recalculate all trigger points after DOM layout and Lenis initialization
            window.ScrollTrigger.refresh();
        }
    }

    // 5. 3D Card Interactive Tilt Physics
    function initInteractive3DTilt() {
        const cards = document.querySelectorAll(".book-showcase-card, .discipline-card");
        if (window.innerWidth < 768) return; // Skip on touch/mobile for battery efficiency

        cards.forEach(function (card) {
            card.addEventListener("mousemove", function (e) {
                const rect = card.getBoundingClientRect();
                const x = e.clientX - rect.left;
                const y = e.clientY - rect.top;
                const centerX = rect.width / 2;
                const centerY = rect.height / 2;

                const rotateX = ((y - centerY) / centerY) * -5;
                const rotateY = ((x - centerX) / centerX) * 5;

                card.style.transform = `perspective(1000px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) translateY(-4px)`;
            });

            card.addEventListener("mouseleave", function () {
                card.style.transform = "";
            });
        });
    }

    // 6. Initialize all components on DOM ready in synchronized order
    function init() {
        initLenis();
        initGSAPAnimations();
        initInteractive3DTilt();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
