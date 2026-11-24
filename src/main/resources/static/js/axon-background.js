/**
 * AXON Background Motion — Sculpted GPU Particle Cloud with Fluid Cursor Interaction
 * Renders an organic sculpted particle ring/cloud on a fixed canvas behind the UI.
 *
 * Theme-aware:
 * - Dark theme: pure dark background (#08070c) with glowing white & lavender dots
 * - Light theme: pure white background (#ffffff) with crisp black & plum dots
 *
 * Excludes home page ('/') and login page ('/login').
 */
(function () {
    "use strict";

    // 1. Strict Page Guard: Never run on Home page or Login page
    var p = window.location.pathname.toLowerCase();
    if (p === "/" || p === "" || p === "/home" || p.indexOf("/login") !== -1 ||
        document.getElementById("kage-canvas") ||
        document.querySelector(".kage-hero") ||
        document.querySelector(".login-container")) {
        return;
    }

    // Prevent duplicate initialization
    if (window.__axonBackgroundInitialized) return;
    window.__axonBackgroundInitialized = true;

    // Helper to check if light theme is active
    function isLightTheme() {
        var dt = document.documentElement.getAttribute("data-theme");
        if (dt === "light") return true;
        if (dt === "dark") return false;
        if (document.documentElement.classList.contains("light") || document.body.classList.contains("light")) {
            return true;
        }
        try {
            var s = localStorage.getItem("libra-theme");
            if (s === "light") return true;
        } catch(e) {}
        return false;
    }

    // 2. Ensure Canvas & Theme Styles are Injected
    function setupCanvas() {
        if (!document.getElementById("axon-bg")) {
            var canvas = document.createElement("canvas");
            canvas.id = "axon-bg";
            canvas.setAttribute("aria-hidden", "true");
            document.body.prepend(canvas);
        }

        if (!document.getElementById("axon-bg-styles")) {
            var style = document.createElement("style");
            style.id = "axon-bg-styles";
            style.textContent = [
                "#axon-bg {",
                "    position: fixed;",
                "    top: 0;",
                "    left: 0;",
                "    width: 100vw;",
                "    height: 100vh;",
                "    z-index: 0;",
                "    pointer-events: none;",
                "    display: block;",
                "}",
                "body {",
                "    background: #08070c !important;",
                "}",
                "body.light,",
                "[data-theme=\"light\"],",
                "[data-theme=\"light\"] body {",
                "    background: #ffffff !important;",
                "}",
                ".librarian-container, main, .container, .catalogue-wrapper,",
                ".borrowing-wrapper, .settings-container, .content, .dashboard-wrapper {",
                "    position: relative;",
                "    z-index: 1;",
                "}"
            ].join("\n");
            document.head.appendChild(style);
        }
    }

    // 3. Load Three.js if needed, then initialize
    function ensureThree(callback) {
        if (typeof window.THREE !== "undefined") {
            callback();
            return;
        }
        var script = document.createElement("script");
        script.src = "/js/three.min.js";
        script.onload = callback;
        script.onerror = function () {
            console.warn("Failed to load local three.min.js for AXON background.");
        };
        document.head.appendChild(script);
    }

    // 4. Generate 64x64 soft spherical matcap texture
    function createMatcapTexture(THREE) {
        var canvas = document.createElement("canvas");
        canvas.width = 64;
        canvas.height = 64;
        var ctx = canvas.getContext("2d");

        // Sphere radial gradient
        var grad = ctx.createRadialGradient(26, 22, 2, 32, 32, 31);
        grad.addColorStop(0.0, "#ffffff");
        grad.addColorStop(0.3, "#ddd8ea");
        grad.addColorStop(0.65, "#8a819c");
        grad.addColorStop(0.95, "#2d2838");
        grad.addColorStop(1.0, "#15121c");

        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(32, 32, 31, 0, Math.PI * 2);
        ctx.fill();

        var texture = new THREE.CanvasTexture(canvas);
        texture.generateMipmaps = true;
        return texture;
    }

    // 5. Initialize the Particle System
    function initAxon() {
        var THREE = window.THREE;
        if (!THREE) return;

        setupCanvas();
        var canvas = document.getElementById("axon-bg");
        if (!canvas) return;

        var lightActive = isLightTheme();

        // Renderer
        var renderer;
        try {
            renderer = new THREE.WebGLRenderer({
                canvas: canvas,
                alpha: false,
                antialias: true,
                powerPreference: "high-performance"
            });
        } catch (e) {
            console.warn("WebGL not supported for AXON background:", e);
            return;
        }

        var dpr = Math.min(window.devicePixelRatio || 1, 2);
        renderer.setPixelRatio(dpr);
        renderer.setSize(window.innerWidth, window.innerHeight);
        renderer.setClearColor(lightActive ? 0xffffff : 0x08070c, 1.0);

        // Scene
        var scene = new THREE.Scene();

        // Camera: FOV 30, near 0.1, far 1000, base pos (-1.17, 0, 6) looking at origin
        var camera = new THREE.PerspectiveCamera(30, window.innerWidth / window.innerHeight, 0.1, 1000);
        var baseCameraPos = new THREE.Vector3(-1.17, 0, 6);
        camera.position.copy(baseCameraPos);
        camera.lookAt(0, 0, 0);

        // Group holding points: position (0.343, 0.16, -1), rotation degrees (-28.4, 390, 4.92), scale (5, 5, 6)
        var group = new THREE.Group();
        group.position.set(0.343, 0.16, -1);
        group.rotation.set(-28.4 * Math.PI / 180, 390 * Math.PI / 180, 4.92 * Math.PI / 180);
        group.scale.set(5, 5, 6);
        scene.add(group);

        // Generate Sculpted Organic Torus / Hollow Mass
        var isMobile = window.innerWidth <= 768;
        var count = isMobile ? 16384 : 65536;

        var positions = new Float32Array(count * 3);
        var origins = new Float32Array(count * 3);
        var randoms = new Float32Array(count * 4);

        // Quaternion rotation: (0.5, 0.5, 0.5, 0.5) normalized
        var q = new THREE.Quaternion(0.5, 0.5, 0.5, 0.5).normalize();

        for (var i = 0; i < count; i++) {
            var i3 = i * 3;
            var i4 = i * 4;

            // Angle around main loop
            var u = Math.random() * Math.PI * 2;
            // Angle around tube
            var v = Math.random() * Math.PI * 2;

            // Lobe variations on bent hollow ring
            var R0 = 0.38 * (1.0 + 0.16 * Math.sin(3 * u) + 0.08 * Math.cos(2 * u));
            var r0 = 0.16 * (1.0 + 0.22 * Math.sin(u));
            // Volume cloud jitter
            var r = r0 * Math.pow(Math.random(), 0.6) * (0.55 + 0.45 * Math.random());
            var zBend = 0.14 * Math.sin(2 * u) - 0.09 * Math.cos(u);

            var px = (R0 + r * Math.cos(v)) * Math.cos(u);
            var py = (R0 + r * Math.cos(v)) * Math.sin(u);
            var pz = r * Math.sin(v) + zBend;

            // Mirror depth axis: p.z = sqrt(p.z*p.z + 0.0215) - sqrt(0.012)
            pz = Math.sqrt(pz * pz + 0.0215) - Math.sqrt(0.012);

            // Apply quaternion rotation
            var vec = new THREE.Vector3(px, py, pz).applyQuaternion(q);

            positions[i3] = vec.x;
            positions[i3 + 1] = vec.y;
            positions[i3 + 2] = vec.z;

            origins[i3] = vec.x;
            origins[i3 + 1] = vec.y;
            origins[i3 + 2] = vec.z;

            // Random attributes
            randoms[i4] = Math.random();     // grey brightness variation
            randoms[i4 + 1] = Math.random(); // sparkle phase
            randoms[i4 + 2] = Math.random(); // lavender / plum highlight weighting
            randoms[i4 + 3] = Math.random(); // curl scale
        }

        var geometry = new THREE.BufferGeometry();
        geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
        geometry.setAttribute("aOrigin", new THREE.BufferAttribute(origins, 3));
        geometry.setAttribute("aRandom", new THREE.BufferAttribute(randoms, 4));

        var matcapTex = createMatcapTexture(THREE);

        // Shaders
        var vertexShader = [
            "attribute vec3 aOrigin;",
            "attribute vec4 aRandom;",
            "uniform float uTime;",
            "uniform vec2 uMouse;",
            "uniform vec2 uMouseVelocity;",
            "uniform float uMouseStrength;",
            "uniform float uPixelRatio;",
            "varying vec4 vRandom;",
            "varying float vDistToCam;",
            "",
            "vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }",
            "vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }",
            "vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }",
            "vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }",
            "",
            "float snoise(vec3 v) {",
            "  const vec2 C = vec2(1.0/6.0, 1.0/3.0);",
            "  const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);",
            "  vec3 i  = floor(v + dot(v, C.yyy));",
            "  vec3 x0 = v - i + dot(i, C.xxx);",
            "  vec3 g = step(x0.yzx, x0.xyz);",
            "  vec3 l = 1.0 - g;",
            "  vec3 i1 = min(g.xyz, l.zxy);",
            "  vec3 i2 = max(g.xyz, l.zxy);",
            "  vec3 x1 = x0 - i1 + C.xxx;",
            "  vec3 x2 = x0 - i2 + C.yyy;",
            "  vec3 x3 = x0 - D.yyy;",
            "  i = mod289(i);",
            "  vec4 p = permute(permute(permute(",
            "             i.z + vec4(0.0, i1.z, i2.z, 1.0))",
            "           + i.y + vec4(0.0, i1.y, i2.y, 1.0))",
            "           + i.x + vec4(0.0, i1.x, i2.x, 1.0));",
            "  float n_ = 0.142857142857;",
            "  vec3  ns = n_ * D.wyz - D.xzx;",
            "  vec4 j = p - 49.0 * floor(p * ns.z * ns.z);",
            "  vec4 x_ = floor(j * ns.z);",
            "  vec4 y_ = floor(j - 7.0 * x_);",
            "  vec4 x = x_ *ns.x + ns.yyyy;",
            "  vec4 y = y_ *ns.x + ns.yyyy;",
            "  vec4 h = 1.0 - abs(x) - abs(y);",
            "  vec4 b0 = vec4(x.xy, y.xy);",
            "  vec4 b1 = vec4(x.zw, y.zw);",
            "  vec4 s0 = floor(b0)*2.0 + 1.0;",
            "  vec4 s1 = floor(b1)*2.0 + 1.0;",
            "  vec4 sh = -step(h, vec4(0.0));",
            "  vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy;",
            "  vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww;",
            "  vec3 p0 = vec3(a0.xy, h.x);",
            "  vec3 p1 = vec3(a0.zw, h.y);",
            "  vec3 p2 = vec3(a1.xy, h.z);",
            "  vec3 p3 = vec3(a1.zw, h.w);",
            "  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));",
            "  p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;",
            "  vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);",
            "  m = m * m;",
            "  return 42.0 * dot(m*m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));",
            "}",
            "",
            "vec3 curlNoise(vec3 p) {",
            "  float eps = 0.015;",
            "  float dx = snoise(p + vec3(eps, 0.0, 0.0)) - snoise(p - vec3(eps, 0.0, 0.0));",
            "  float dy = snoise(p + vec3(0.0, eps, 0.0)) - snoise(p - vec3(0.0, eps, 0.0));",
            "  float dz = snoise(p + vec3(0.0, 0.0, eps)) - snoise(p - vec3(0.0, 0.0, eps));",
            "  return vec3(dy - dz, dz - dx, dx - dy) / (2.0 * eps);",
            "}",
            "",
            "void main() {",
            "  vRandom = aRandom;",
            "  vec3 pos = aOrigin;",
            "",
            "  // Ambient curl noise drift",
            "  vec3 curl = curlNoise(pos * 0.9 + vec3(uTime * 0.06));",
            "  pos += curl * 0.032 * (0.6 + 0.4 * aRandom.w);",
            "",
            "  // Breathing organic pulse",
            "  pos += normalize(pos + vec3(0.001)) * sin(uTime * 1.3 + aRandom.y * 6.28) * 0.006;",
            "",
            "  // Screen-space cursor fluid turbulence",
            "  vec4 worldPos = modelMatrix * vec4(pos, 1.0);",
            "  vec4 screenPos = projectionMatrix * viewMatrix * worldPos;",
            "  vec2 ndc = screenPos.xy / screenPos.w;",
            "",
            "  vec2 mDiff = ndc - uMouse;",
            "  float mDist = length(mDiff);",
            "  float radius = 0.42;",
            "  if (mDist < radius && uMouseStrength > 0.01) {",
            "    float factor = smoothstep(radius, 0.0, mDist);",
            "    vec2 swirl2D = vec2(-mDiff.y, mDiff.x) / (mDist + 0.05);",
            "    vec3 pushDir = vec3(uMouseVelocity * 1.2 + swirl2D * 0.7, -factor * 0.4);",
            "    pos += pushDir * factor * uMouseStrength * 0.09;",
            "  }",
            "",
            "  vec4 mvPosition = viewMatrix * modelMatrix * vec4(pos, 1.0);",
            "  vDistToCam = -mvPosition.z;",
            "",
            "  float scale = smoothstep(1.0, 15.0, vDistToCam) * mix(0.75, 1.25, aRandom.z);",
            "  gl_PointSize = clamp(0.045 * uPixelRatio * 2.0 * scale * (1000.0 / vDistToCam), 2.0, 12.0);",
            "  gl_Position = projectionMatrix * mvPosition;",
            "}"
        ].join("\n");

        var fragmentShader = [
            "uniform float uTime;",
            "uniform float uThemeMode;",
            "uniform sampler2D uMatcap;",
            "varying vec4 vRandom;",
            "varying float vDistToCam;",
            "",
            "void main() {",
            "  vec2 coord = gl_PointCoord - vec2(0.5);",
            "  float dist = length(coord);",
            "  if (dist > 0.5) discard;",
            "",
            "  float alpha = smoothstep(0.5, 0.08, dist);",
            "",
            "  // Sample matcap",
            "  float angle = sin(uTime * 0.6 + vRandom.z * 20.0) * 0.5 + 1.0;",
            "  float s = sin(angle);",
            "  float c = cos(angle);",
            "  vec2 rotCoord = vec2(c * coord.x - s * coord.y, s * coord.x + c * coord.y) + vec2(0.5);",
            "  vec4 matcap = texture2D(uMatcap, clamp(rotCoord, 0.0, 1.0));",
            "",
            "  // --- DARK THEME: White & glowing lavender dots on black ---",
            "  vec3 darkBase = mix(vec3(0.35, 0.35, 0.4), vec3(0.9, 0.9, 0.96), vRandom.x);",
            "  vec3 darkLavender = vec3(0.72, 0.58, 0.88);",
            "  vec3 darkColor = mix(darkBase, darkLavender, pow(vRandom.z, 12.0) * 0.95);",
            "  float darkSparkle = 0.55 + (0.45 + sin(uTime * 3.5 + vRandom.y * 20.0)) * pow(vRandom.z, 6.0) * 2.2;",
            "  darkColor *= darkSparkle;",
            "  darkColor = mix(darkColor, darkColor * matcap.rgb * 1.7, 0.6);",
            "  float darkAlpha = alpha * 0.88;",
            "",
            "  // --- LIGHT THEME: Black dots & deep plum highlights on white ---",
            "  vec3 lightBase = mix(vec3(0.03, 0.02, 0.04), vec3(0.14, 0.12, 0.17), vRandom.x);",
            "  vec3 lightPlum = vec3(0.42, 0.22, 0.48);",
            "  vec3 lightColor = mix(lightBase, lightPlum, pow(vRandom.z, 8.0) * 0.92);",
            "  float lightSparkle = 0.88 + 0.12 * sin(uTime * 2.5 + vRandom.y * 15.0);",
            "  lightColor *= lightSparkle;",
            "  float lightAlpha = alpha * 0.94;",
            "",
            "  // Theme interpolation",
            "  vec3 col = mix(darkColor, lightColor, uThemeMode);",
            "  float finalAlpha = mix(darkAlpha, lightAlpha, uThemeMode);",
            "",
            "  gl_FragColor = vec4(col, finalAlpha);",
            "}"
        ].join("\n");

        var material = new THREE.ShaderMaterial({
            vertexShader: vertexShader,
            fragmentShader: fragmentShader,
            uniforms: {
                uTime: { value: 0 },
                uThemeMode: { value: lightActive ? 1.0 : 0.0 },
                uMouse: { value: new THREE.Vector2(-999, -999) },
                uMouseVelocity: { value: new THREE.Vector2(0, 0) },
                uMouseStrength: { value: 0.0 },
                uPixelRatio: { value: dpr },
                uMatcap: { value: matcapTex }
            },
            transparent: true,
            blending: lightActive ? THREE.NormalBlending : THREE.AdditiveBlending,
            depthTest: true,
            depthWrite: false
        });

        var points = new THREE.Points(geometry, material);
        group.add(points);

        // Mouse & Fluid tracking
        var mouse = { x: -999, y: -999, prevX: -999, prevY: -999, vx: 0, vy: 0, strength: 0 };
        var targetCamX = baseCameraPos.x;
        var targetCamY = baseCameraPos.y;

        function onPointerMove(e) {
            var clientX = e.clientX || (e.touches && e.touches[0] ? e.touches[0].clientX : 0);
            var clientY = e.clientY || (e.touches && e.touches[0] ? e.touches[0].clientY : 0);

            // Normalized Device Coordinates (-1 .. 1)
            var nx = (clientX / window.innerWidth) * 2 - 1;
            var ny = -(clientY / window.innerHeight) * 2 + 1;

            if (mouse.prevX !== -999) {
                var dx = nx - mouse.prevX;
                var dy = ny - mouse.prevY;
                var dist = Math.sqrt(dx * dx + dy * dy);
                mouse.vx = dx * 10.0;
                mouse.vy = dy * 10.0;
                mouse.strength = Math.min(mouse.strength + dist * 8.0, 1.2);
            }

            mouse.x = nx;
            mouse.y = ny;
            mouse.prevX = nx;
            mouse.prevY = ny;

            // Gaze camera: target = base + (mappedX * -1, mappedY * 2)
            targetCamX = baseCameraPos.x + nx * -0.8;
            targetCamY = baseCameraPos.y + (clientY / window.innerHeight * 2 - 1) * -1.5;
        }

        window.addEventListener("pointermove", onPointerMove, { passive: true });
        window.addEventListener("touchmove", onPointerMove, { passive: true });

        // Resize handler
        function onWindowResize() {
            var w = window.innerWidth;
            var h = window.innerHeight;
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
            renderer.setSize(w, h);
            material.uniforms.uPixelRatio.value = Math.min(window.devicePixelRatio || 1, 2);
        }
        window.addEventListener("resize", onWindowResize);

        // Theme sync listener
        var lastIsLight = lightActive;
        function updateTheme(isLight) {
            if (isLight === lastIsLight) return;
            lastIsLight = isLight;
            material.uniforms.uThemeMode.value = isLight ? 1.0 : 0.0;
            material.blending = isLight ? THREE.NormalBlending : THREE.AdditiveBlending;
            material.needsUpdate = true;
            renderer.setClearColor(isLight ? 0xffffff : 0x08070c, 1.0);
        }

        // MutationObserver to catch theme changes immediately
        var themeObserver = new MutationObserver(function () {
            updateTheme(isLightTheme());
        });
        themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme", "class"] });
        themeObserver.observe(document.body, { attributes: true, attributeFilter: ["class"] });

        // Animation Loop
        var clock = new THREE.Clock();
        var isVisible = true;

        document.addEventListener("visibilitychange", function () {
            isVisible = !document.hidden;
        });

        function animate() {
            requestAnimationFrame(animate);
            if (!isVisible) return;

            var delta = clock.getDelta();
            var time = clock.getElapsedTime();

            // Periodic theme check (handles cross-tab or fast toggles)
            var currentLight = isLightTheme();
            if (currentLight !== lastIsLight) {
                updateTheme(currentLight);
            }

            // Camera gaze lerp (factor 0.05)
            camera.position.x += (targetCamX - camera.position.x) * 0.05;
            camera.position.y += (targetCamY - camera.position.y) * 0.05;
            camera.lookAt(0, 0, 0);

            // Decay mouse velocity & fluid impulse back to rest over ~1.5 - 2s
            mouse.strength *= Math.pow(0.25, delta);
            mouse.vx *= Math.pow(0.3, delta);
            mouse.vy *= Math.pow(0.3, delta);

            // Update uniforms
            material.uniforms.uTime.value = time;
            material.uniforms.uMouse.value.set(mouse.x, mouse.y);
            material.uniforms.uMouseVelocity.value.set(mouse.vx, mouse.vy);
            material.uniforms.uMouseStrength.value = mouse.strength;

            // Gentle organic rotation of group
            group.rotation.y = (390 * Math.PI / 180) + Math.sin(time * 0.2) * 0.05;
            group.rotation.x = (-28.4 * Math.PI / 180) + Math.cos(time * 0.15) * 0.03;

            renderer.render(scene, camera);
        }

        animate();
    }

    // Start initialization when DOM is ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            ensureThree(initAxon);
        });
    } else {
        ensureThree(initAxon);
    }
})();
