/**
 * LIBRA LMS - Winter Snowfall Engine
 * Managed and linked via the 'winter' dependency injection package (SkPhilipp/Winter).
 * Exclusively loaded on the login page for an atmospheric, cinematic winter ambiance.
 */
(() => {
    'use strict';

    if (window.__winterSnowInitialized) {
        return;
    }
    window.__winterSnowInitialized = true;

    function bootstrapWinter() {
        const canvas = document.getElementById('winter-login-canvas');
        if (!canvas) {
            console.warn('[Winter] Canvas #winter-login-canvas not found.');
            return;
        }

        if (typeof Winter === 'undefined') {
            console.error('[Winter] Winter dependency injection library is not loaded.');
            return;
        }

        // 1. Initialize Winter DI container from the installed npm package
        const container = new Winter();

        // 2. Register Logger via Winter.LoggerProvider
        if (Winter.LoggerProvider) {
            container.register('logger', new Winter.LoggerProvider());
        }

        // 3. Register Configuration Provider via Winter.ValueProvider
        const config = {
            count: 150,
            wind: 0.35,
            layers: [
                // Layer 0: Foreground (larger, faster, high clarity)
                { weight: 0.25, minRadius: 2.8, maxRadius: 4.4, minSpeedY: 1.2, maxSpeedY: 2.4, minAlpha: 0.75, maxAlpha: 0.98, color: '255, 255, 255' },
                // Layer 1: Midground (medium particles with gentle sway)
                { weight: 0.45, minRadius: 1.6, maxRadius: 2.7, minSpeedY: 0.7, maxSpeedY: 1.4, minAlpha: 0.55, maxAlpha: 0.80, color: '240, 235, 250' },
                // Layer 2: Background (soft, distant depth particles)
                { weight: 0.30, minRadius: 0.9, maxRadius: 1.6, minSpeedY: 0.4, maxSpeedY: 0.8, minAlpha: 0.35, maxAlpha: 0.60, color: '225, 218, 238' }
            ]
        };
        container.register('config', new Winter.ValueProvider(config));
        container.register('canvas', new Winter.ValueProvider(canvas));

        // 4. Register Snowfall Engine Provider via Winter Module / Custom Provider pattern
        container.register('snowfallEngine', {
            dependencies: function () {
                return ['config', 'canvas'];
            },
            instantiate: function (providers) {
                const cfg = providers['config'].instantiate();
                const cvs = providers['canvas'].instantiate();
                const engine = new SnowfallEngine(cvs, cfg);
                window.__winterSnowEngine = engine;
                return engine;
            }
        });

        // 5. Link container dependencies and start engine
        container.link().then(() => {
            console.info('[Winter] Dependencies linked successfully. Starting snowfall effect.');
        }).catch((err) => {
            console.error('[Winter] Linking failed:', err);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bootstrapWinter);
    } else {
        bootstrapWinter();
    }

    /**
     * SnowfallEngine: High-performance, depth-layered canvas snow simulation.
     */
    class SnowfallEngine {
        constructor(canvas, config) {
            this.canvas = canvas;
            this.config = config;
            this.ctx = canvas.getContext('2d');
            this.particles = [];
            this.width = window.innerWidth || 1920;
            this.height = window.innerHeight || 1080;
            this.dpr = Math.min(window.devicePixelRatio || 1, 2);
            this.animationFrame = null;
            this.isRunning = false;
            this.motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

            this.init();
        }

        init() {
            this.resize();
            this.createParticles();
            this.bindEvents();
            // Always run the animation loop so snowflakes visibly fall across the viewport
            this.start();
        }

        bindEvents() {
            window.addEventListener('resize', () => {
                this.resize();
            }, { passive: true });

            document.addEventListener('visibilitychange', () => {
                if (document.hidden) {
                    this.stop();
                } else {
                    this.start();
                }
            });
        }

        resize() {
            this.width = window.innerWidth || document.documentElement.clientWidth || 1920;
            this.height = window.innerHeight || document.documentElement.clientHeight || 1080;
            this.canvas.width = Math.floor(this.width * this.dpr);
            this.canvas.height = Math.floor(this.height * this.dpr);
            this.canvas.style.width = '100vw';
            this.canvas.style.height = '100vh';
            this.ctx.setTransform(1, 0, 0, 1, 0, 0);
            this.ctx.scale(this.dpr, this.dpr);
        }

        createParticles() {
            this.particles = [];
            const count = this.config.count;
            const layers = this.config.layers;

            for (let i = 0; i < count; i++) {
                const rand = Math.random();
                let layer = layers[1];
                if (rand < layers[0].weight) {
                    layer = layers[0];
                } else if (rand > 1 - layers[2].weight) {
                    layer = layers[2];
                }

                this.particles.push(new Snowflake(this.width, this.height, layer));
            }
        }

        start() {
            if (this.isRunning) return;
            this.isRunning = true;
            const loop = (timestamp) => {
                if (!this.isRunning) return;
                this.update(timestamp);
                this.draw();
                this.animationFrame = requestAnimationFrame(loop);
            };
            this.animationFrame = requestAnimationFrame(loop);
        }

        stop() {
            this.isRunning = false;
            if (this.animationFrame) {
                cancelAnimationFrame(this.animationFrame);
                this.animationFrame = null;
            }
        }

        update(timestamp) {
            // If reduced motion is requested, maintain calm, meditative descent speed (0.5x)
            const speedFactor = this.motionQuery.matches ? 0.5 : 1.0;
            const wind = (Math.sin((timestamp || 0) * 0.0008) * 0.3 + this.config.wind) * speedFactor;

            for (let i = 0; i < this.particles.length; i++) {
                this.particles[i].update(this.width, this.height, wind, speedFactor);
            }
        }

        draw() {
            this.ctx.clearRect(0, 0, this.width, this.height);

            for (let i = 0; i < this.particles.length; i++) {
                this.particles[i].draw(this.ctx);
            }
        }
    }

    /**
     * Snowflake: Individual snowflake with layered depth, wobble, and soft halo.
     */
    class Snowflake {
        constructor(width, height, layer) {
            this.layer = layer;
            this.reset(width, height, true);
        }

        reset(width, height, initial) {
            this.x = Math.random() * (width || 1920);
            this.y = initial ? Math.random() * (height || 1080) : -15;
            this.radius = this.layer.minRadius + Math.random() * (this.layer.maxRadius - this.layer.minRadius);
            this.speedY = this.layer.minSpeedY + Math.random() * (this.layer.maxSpeedY - this.layer.minSpeedY);
            this.speedX = (Math.random() - 0.5) * 0.35;
            this.alpha = this.layer.minAlpha + Math.random() * (this.layer.maxAlpha - this.layer.minAlpha);
            this.swayAngle = Math.random() * Math.PI * 2;
            this.swaySpeed = 0.012 + Math.random() * 0.02;
            this.swayRadius = 0.6 + Math.random() * 1.0;
        }

        update(width, height, wind, speedFactor) {
            this.swayAngle += this.swaySpeed;
            this.x += (Math.sin(this.swayAngle) * this.swayRadius + this.speedX + wind) * speedFactor;
            this.y += this.speedY * speedFactor;

            // Recycle seamlessly
            if (this.y > height + 15) {
                this.reset(width, height, false);
            } else if (this.x < -20) {
                this.x = width + 10;
            } else if (this.x > width + 20) {
                this.x = -10;
            }
        }

        draw(ctx) {
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(${this.layer.color}, ${this.alpha})`;
            ctx.fill();

            // Subtle crystalline corona for larger foreground flakes
            if (this.radius > 2.7) {
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.radius * 1.6, 0, Math.PI * 2);
                ctx.fillStyle = `rgba(${this.layer.color}, ${this.alpha * 0.22})`;
                ctx.fill();
            }
        }
    }
})();
