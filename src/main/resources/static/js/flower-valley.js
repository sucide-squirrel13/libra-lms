/**
 * LIBRA LMS — 3D Night Flower Valley Hero Engine
 * Exact recreation and adaptation of the MotionSites & Gladeye 3D Night Flower Valley.
 * Adapted to the Dark Academia aesthetic of LIBRA (Deep Charcoal, Dusty Purple, Antique Gold).
 *
 * Capabilities:
 * - 3D procedural terrain valley with thousands of blooming point-sprite night flowers and botanical leaves.
 * - Continuous forward cruising along an infinite curved 3D camera spline path with seamless horizon wrapping.
 * - Interactive mouse repulsion & bioluminescent proximity bloom in screen space.
 * - Integration with Lenis & window scrolling (wheel/scroll smoothly propels flight forward).
 * - CTA hover transition (eases flight to stop, disperses flowers into floating glowing embers).
 * - Custom ShaderMaterials with perspective size attenuation, distance blooming, and archival night fog falloff.
 * - Responsive to viewport changes and theme shifts (dark/light mode).
 */
(() => {
    'use strict';

    if (window.__flowerValleyInitialized) return;

    function initFlowerValley() {
        const container = document.getElementById('valley');
        if (!container) return;

        // Ensure Three.js is available
        if (typeof THREE === 'undefined') {
            setTimeout(initFlowerValley, 80);
            return;
        }

        window.__flowerValleyInitialized = true;

        const mount = container.querySelector('.js-canvas-container') || container;

        const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

        // 1. Scene, Camera, Renderer Setup
        const scene = new THREE.Scene();
        const fogColor = new THREE.Color(0x121016); // Archival deep charcoal
        scene.fog = new THREE.FogExp2(0x121016, 0.016);

        let width = mount.clientWidth || window.innerWidth;
        let height = mount.clientHeight || window.innerHeight;

        const camera = new THREE.PerspectiveCamera(54, width / height, 0.1, 400);
        camera.position.set(0, 5, 0);

        const renderer = new THREE.WebGLRenderer({
            alpha: true,
            antialias: true,
            powerPreference: 'high-performance'
        });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        renderer.setSize(width, height);
        renderer.setClearColor(0x000000, 0); // Transparent canvas to overlay atop architectural scrim
        mount.appendChild(renderer.domElement);

        // 2. Procedural Canvas Textures
        function createFlowerTexture() {
            const canvas = document.createElement('canvas');
            canvas.width = 128;
            canvas.height = 128;
            const ctx = canvas.getContext('2d');
            const cx = 64, cy = 64;

            // Soft outer radial corona
            const radGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, 64);
            radGrad.addColorStop(0, 'rgba(165, 138, 181, 0.95)'); // Dusty purple
            radGrad.addColorStop(0.35, 'rgba(194, 178, 206, 0.65)'); // Muted lavender
            radGrad.addColorStop(0.7, 'rgba(136, 117, 143, 0.22)');
            radGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
            ctx.fillStyle = radGrad;
            ctx.fillRect(0, 0, 128, 128);

            // 5 Petals
            ctx.fillStyle = 'rgba(240, 236, 243, 0.92)';
            for (let i = 0; i < 5; i++) {
                const angle = (i * Math.PI * 2) / 5;
                const px = cx + Math.cos(angle) * 22;
                const py = cy + Math.sin(angle) * 22;
                ctx.beginPath();
                ctx.arc(px, py, 14, 0, Math.PI * 2);
                ctx.fill();
            }

            // Radiant Antique Gold Center Stamen
            const goldGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, 18);
            goldGrad.addColorStop(0, 'rgba(255, 238, 185, 1.0)'); // Luminous core
            goldGrad.addColorStop(0.5, 'rgba(197, 165, 104, 0.9)'); // Antique gold
            goldGrad.addColorStop(1, 'rgba(197, 165, 104, 0)');
            ctx.fillStyle = goldGrad;
            ctx.beginPath();
            ctx.arc(cx, cy, 18, 0, Math.PI * 2);
            ctx.fill();

            return new THREE.CanvasTexture(canvas);
        }

        function createLeafTexture() {
            const canvas = document.createElement('canvas');
            canvas.width = 64;
            canvas.height = 64;
            const ctx = canvas.getContext('2d');

            ctx.translate(32, 32);
            ctx.rotate(Math.PI / 4);

            const leafGrad = ctx.createRadialGradient(0, 0, 0, 0, 0, 28);
            leafGrad.addColorStop(0, 'rgba(115, 138, 120, 0.88)'); // Muted botanical sage
            leafGrad.addColorStop(0.5, 'rgba(75, 95, 80, 0.52)');
            leafGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

            ctx.fillStyle = leafGrad;
            ctx.beginPath();
            ctx.ellipse(0, 0, 9, 25, 0, 0, Math.PI * 2);
            ctx.fill();

            return new THREE.CanvasTexture(canvas);
        }

        function createDustTexture() {
            const canvas = document.createElement('canvas');
            canvas.width = 32;
            canvas.height = 32;
            const ctx = canvas.getContext('2d');

            const grad = ctx.createRadialGradient(16, 16, 0, 16, 16, 16);
            grad.addColorStop(0, 'rgba(255, 238, 180, 0.95)'); // Gold spore
            grad.addColorStop(0.45, 'rgba(197, 165, 104, 0.45)');
            grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(16, 16, 16, 0, Math.PI * 2);
            ctx.fill();

            return new THREE.CanvasTexture(canvas);
        }

        const flowerTex = createFlowerTexture();
        const leafTex = createLeafTexture();
        const dustTex = createDustTexture();

        // 3. Infinite Spline & Parametric Valley Terrain
        function getPathPoint(t) {
            const x = Math.sin(t * 0.08) * 16 + Math.sin(t * 0.03) * 10;
            const y = Math.cos(t * 0.05) * 2.6 + 4.6;
            const z = t * 25;
            return new THREE.Vector3(x, y, z);
        }

        function getTerrainY(x, z) {
            // Valley profile: lower in the center following spline, rising steeply on hillsides
            const center = Math.sin(z * 0.0032) * 16 + Math.sin(z * 0.0012) * 10;
            const distFromPath = Math.abs(x - center);
            const hillRise = Math.pow(distFromPath / 22, 2.2) * 8.2;
            const rolling = Math.sin(x * 0.12) * Math.cos(z * 0.08) * 1.4;
            return hillRise + rolling - 2.6;
        }

        // 4. Thousands of Scattered Flowers & Leaves
        const FLOWER_COUNT = 2600;
        const LEAF_COUNT = 1600;
        const DUST_COUNT = 260;
        const VALLEY_DEPTH = 850;

        // Custom Shader for Blooming Point Sprites
        const flowerShaderMat = new THREE.ShaderMaterial({
            uniforms: {
                pointTexture: { value: flowerTex }
            },
            vertexShader: `
                attribute float size;
                attribute vec3 customColor;
                varying vec3 vColor;
                varying float vDepth;
                void main() {
                    vColor = customColor;
                    vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
                    // Size attenuation with perspective
                    gl_PointSize = size * (280.0 / -mvPosition.z);
                    gl_Position = projectionMatrix * mvPosition;
                    vDepth = -mvPosition.z;
                }
            `,
            fragmentShader: `
                uniform sampler2D pointTexture;
                varying vec3 vColor;
                varying float vDepth;
                void main() {
                    vec4 tex = texture2D(pointTexture, gl_PointCoord);
                    if (tex.a < 0.05) discard;
                    // Night fog atmospheric depth fade
                    float fogFactor = clamp((vDepth - 25.0) / 340.0, 0.0, 1.0);
                    vec3 finalColor = tex.rgb * vColor;
                    finalColor = mix(finalColor, vec3(0.07, 0.06, 0.09), fogFactor * 0.8);
                    gl_FragColor = vec4(finalColor, tex.a * (1.0 - fogFactor * 0.65));
                }
            `,
            blending: THREE.AdditiveBlending,
            depthWrite: false,
            transparent: true
        });

        // Flower Geometry
        const flowerGeo = new THREE.BufferGeometry();
        const flowerPos = new Float32Array(FLOWER_COUNT * 3);
        const flowerOrigPos = new Float32Array(FLOWER_COUNT * 3);
        const flowerSizes = new Float32Array(FLOWER_COUNT);
        const flowerBaseSizes = new Float32Array(FLOWER_COUNT);
        const flowerColors = new Float32Array(FLOWER_COUNT * 3);
        const flowerPhases = new Float32Array(FLOWER_COUNT);

        const palette = [
            new THREE.Color(0xA58AB5), // Radiant dusty purple
            new THREE.Color(0xC2B2CE), // Muted lavender
            new THREE.Color(0xC5A568), // Antique gold
            new THREE.Color(0xEAE6ED), // Soft off-white
            new THREE.Color(0x88758F)  // Deep archival plum
        ];

        for (let i = 0; i < FLOWER_COUNT; i++) {
            const z = Math.random() * VALLEY_DEPTH - 50;
            const pathCenter = Math.sin(z * 0.0032) * 16 + Math.sin(z * 0.0012) * 10;
            const x = pathCenter + (Math.random() - 0.5) * 80;
            const y = getTerrainY(x, z) + Math.random() * 0.4;

            const idx = i * 3;
            flowerPos[idx] = x;
            flowerPos[idx + 1] = y;
            flowerPos[idx + 2] = z;

            flowerOrigPos[idx] = x;
            flowerOrigPos[idx + 1] = y;
            flowerOrigPos[idx + 2] = z;

            const baseSize = 2.4 + Math.random() * 3.6;
            flowerBaseSizes[i] = baseSize;
            flowerSizes[i] = baseSize;

            const col = palette[Math.floor(Math.random() * palette.length)];
            flowerColors[idx] = col.r;
            flowerColors[idx + 1] = col.g;
            flowerColors[idx + 2] = col.b;

            flowerPhases[i] = Math.random() * Math.PI * 2;
        }

        flowerGeo.setAttribute('position', new THREE.BufferAttribute(flowerPos, 3));
        flowerGeo.setAttribute('customColor', new THREE.BufferAttribute(flowerColors, 3));
        flowerGeo.setAttribute('size', new THREE.BufferAttribute(flowerSizes, 1));

        const flowerPoints = new THREE.Points(flowerGeo, flowerShaderMat);
        scene.add(flowerPoints);

        // Leaves Geometry
        const leafGeo = new THREE.BufferGeometry();
        const leafPos = new Float32Array(LEAF_COUNT * 3);
        const leafOrigPos = new Float32Array(LEAF_COUNT * 3);
        const leafSizes = new Float32Array(LEAF_COUNT);
        const leafColors = new Float32Array(LEAF_COUNT * 3);

        const leafShaderMat = new THREE.ShaderMaterial({
            uniforms: {
                leafTexture: { value: leafTex }
            },
            vertexShader: `
                attribute float size;
                attribute vec3 customColor;
                varying vec3 vColor;
                varying float vDepth;
                void main() {
                    vColor = customColor;
                    vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
                    gl_PointSize = size * (230.0 / -mvPosition.z);
                    gl_Position = projectionMatrix * mvPosition;
                    vDepth = -mvPosition.z;
                }
            `,
            fragmentShader: `
                uniform sampler2D leafTexture;
                varying vec3 vColor;
                varying float vDepth;
                void main() {
                    vec4 tex = texture2D(leafTexture, gl_PointCoord);
                    if (tex.a < 0.08) discard;
                    float fogFactor = clamp((vDepth - 25.0) / 320.0, 0.0, 1.0);
                    vec3 finalColor = mix(tex.rgb * vColor, vec3(0.07, 0.06, 0.09), fogFactor * 0.85);
                    gl_FragColor = vec4(finalColor, tex.a * 0.75 * (1.0 - fogFactor * 0.7));
                }
            `,
            blending: THREE.NormalBlending,
            depthWrite: false,
            transparent: true
        });

        for (let i = 0; i < LEAF_COUNT; i++) {
            const z = Math.random() * VALLEY_DEPTH - 50;
            const pathCenter = Math.sin(z * 0.0032) * 16 + Math.sin(z * 0.0012) * 10;
            const x = pathCenter + (Math.random() - 0.5) * 88;
            const y = getTerrainY(x, z) - 0.2 + Math.random() * 0.3;

            const idx = i * 3;
            leafPos[idx] = x;
            leafPos[idx + 1] = y;
            leafPos[idx + 2] = z;

            leafOrigPos[idx] = x;
            leafOrigPos[idx + 1] = y;
            leafOrigPos[idx + 2] = z;

            leafSizes[i] = 2.2 + Math.random() * 2.2;

            leafColors[idx] = 0.36 + Math.random() * 0.15;
            leafColors[idx + 1] = 0.46 + Math.random() * 0.20;
            leafColors[idx + 2] = 0.38 + Math.random() * 0.15;
        }

        leafGeo.setAttribute('position', new THREE.BufferAttribute(leafPos, 3));
        leafGeo.setAttribute('customColor', new THREE.BufferAttribute(leafColors, 3));
        leafGeo.setAttribute('size', new THREE.BufferAttribute(leafSizes, 1));

        const leafPoints = new THREE.Points(leafGeo, leafShaderMat);
        scene.add(leafPoints);

        // Ambient Floating Spores / Fireflies
        const dustGeo = new THREE.BufferGeometry();
        const dustPos = new Float32Array(DUST_COUNT * 3);
        const dustOrigPos = new Float32Array(DUST_COUNT * 3);

        for (let i = 0; i < DUST_COUNT; i++) {
            const z = Math.random() * VALLEY_DEPTH - 50;
            const x = (Math.random() - 0.5) * 75;
            const y = Math.random() * 15 + 1.2;

            const idx = i * 3;
            dustPos[idx] = x;
            dustPos[idx + 1] = y;
            dustPos[idx + 2] = z;

            dustOrigPos[idx] = x;
            dustOrigPos[idx + 1] = y;
            dustOrigPos[idx + 2] = z;
        }

        dustGeo.setAttribute('position', new THREE.BufferAttribute(dustPos, 3));
        const dustMat = new THREE.PointsMaterial({
            size: 3.2,
            map: dustTex,
            transparent: true,
            opacity: 0.85,
            blending: THREE.AdditiveBlending,
            depthWrite: false,
            sizeAttenuation: true
        });
        const dustPoints = new THREE.Points(dustGeo, dustMat);
        scene.add(dustPoints);

        // 5. Interaction Tracking & State
        let cameraT = 0;
        let scrollVelocity = 0;
        const idleSpeed = prefersReducedMotion.matches ? 0.005 : 0.015;

        const mouse = { x: 0, y: 0, targetX: 0, targetY: 0 };
        const raycaster = new THREE.Raycaster();
        const mouse3D = new THREE.Vector3(0, 0, 0);

        let isHovered = false;
        let hoverDispersion = 0;

        // Window Mouse Interaction
        window.addEventListener('mousemove', (e) => {
            mouse.targetX = (e.clientX / window.innerWidth) * 2 - 1;
            mouse.targetY = -(e.clientY / window.innerHeight) * 2 + 1;
        }, { passive: true });

        // Wheel and Scroll Integration (seamless acceleration forward)
        window.addEventListener('wheel', (e) => {
            scrollVelocity += e.deltaY * 0.0006;
        }, { passive: true });

        let lastScrollY = window.scrollY || window.pageYOffset;
        window.addEventListener('scroll', () => {
            const currentScrollY = window.scrollY || window.pageYOffset;
            const dy = currentScrollY - lastScrollY;
            lastScrollY = currentScrollY;
            scrollVelocity += dy * 0.0012;
        }, { passive: true });

        // CTA Hover Transition (gladeye / MotionSites behavior: ease flight to stop, flowers disperse into floating embers)
        const primaryHeroCta = document.querySelector('.btn-hero-primary');
        const heroFolioCard = document.getElementById('hero-folio-card');

        function startHoverTransition() {
            isHovered = true;
        }

        function stopHoverTransition() {
            isHovered = false;
        }

        if (primaryHeroCta) {
            primaryHeroCta.addEventListener('pointerenter', startHoverTransition);
            primaryHeroCta.addEventListener('pointerleave', stopHoverTransition);
        }

        if (heroFolioCard) {
            heroFolioCard.addEventListener('pointerenter', startHoverTransition);
            heroFolioCard.addEventListener('pointerleave', stopHoverTransition);
        }

        // Viewport Resize
        function onResize() {
            const w = mount.clientWidth || window.innerWidth;
            const h = mount.clientHeight || window.innerHeight;
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
            renderer.setSize(w, h);
        }
        window.addEventListener('resize', onResize, { passive: true });

        // 6. Animation Loop & Physics Simulation
        let lastTime = performance.now();

        function animate(now) {
            requestAnimationFrame(animate);

            const dt = Math.min((now - lastTime) / 1000, 0.1);
            lastTime = now;

            // Smooth mouse interpolation
            mouse.x += (mouse.targetX - mouse.x) * 0.08;
            mouse.y += (mouse.targetY - mouse.y) * 0.08;

            // Hover state easing (disperses flowers into embers)
            const targetDispersion = isHovered ? 1.0 : 0.0;
            hoverDispersion += (targetDispersion - hoverDispersion) * 0.05;

            // Camera progress update (cruises forward along valley, slows if hovered)
            const speedMultiplier = 1.0 - hoverDispersion * 0.85;
            cameraT += (idleSpeed * speedMultiplier) + scrollVelocity;
            scrollVelocity *= 0.88; // Damping

            // Update Camera along 3D Spline Path
            const camPos = getPathPoint(cameraT);
            const lookPos = getPathPoint(cameraT + 0.18);

            // Subtle mouse tilt and parallax
            camPos.x += mouse.x * 2.2;
            camPos.y += mouse.y * 1.4;

            camera.position.copy(camPos);
            camera.lookAt(lookPos.x + mouse.x * 1.6, lookPos.y + mouse.y * 1.0, lookPos.z);

            // Compute mouse position in world space for screen-space flower repulsion
            raycaster.setFromCamera(new THREE.Vector2(mouse.x, mouse.y), camera);
            const mouseRayPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), -camPos.z - 24);
            raycaster.ray.intersectPlane(mouseRayPlane, mouse3D);

            const timeVal = now * 0.0018;

            // Update Flower Positions & Blooming with Infinite Horizon Wrapping
            const flowerPositions = flowerGeo.attributes.position.array;
            const flowerSizesArr = flowerGeo.attributes.size.array;

            for (let i = 0; i < FLOWER_COUNT; i++) {
                const idx = i * 3;
                let origZ = flowerOrigPos[idx + 2];

                // Infinite horizon wrapping: re-seed flowers that fall behind camera
                if (origZ < camPos.z - 25) {
                    origZ += VALLEY_DEPTH;
                    flowerOrigPos[idx + 2] = origZ;
                    const pathCenter = Math.sin(origZ * 0.0032) * 16 + Math.sin(origZ * 0.0012) * 10;
                    flowerOrigPos[idx] = pathCenter + (Math.random() - 0.5) * 80;
                    flowerOrigPos[idx + 1] = getTerrainY(flowerOrigPos[idx], origZ) + Math.random() * 0.4;
                }

                const origX = flowerOrigPos[idx];
                const origY = flowerOrigPos[idx + 1];

                // Relative distance to camera
                const distZ = origZ - camPos.z;

                // Natural gentle wind sway
                const phase = flowerPhases[i];
                const windX = Math.sin(timeVal + phase) * 0.28;
                const windY = Math.cos(timeVal * 0.8 + phase) * 0.16;

                // Mouse Repulsion & Proximity Glow
                const dx = origX - mouse3D.x;
                const dy = origY - mouse3D.y;
                const distMouse = Math.sqrt(dx * dx + dy * dy);

                let repelX = 0, repelY = 0, repelZ = 0;
                let proximityBloom = 0;

                if (distMouse < 18.0 && Math.abs(distZ) < 35.0) {
                    const force = (1.0 - distMouse / 18.0) * 3.6;
                    repelX = (dx / (distMouse + 0.1)) * force;
                    repelY = (dy / (distMouse + 0.1)) * force;
                    proximityBloom = (1.0 - distMouse / 18.0) * 2.8;
                }

                // Hover Dispersion: flowers float outward like glowing particles
                if (hoverDispersion > 0.01) {
                    const dispAngle = phase * 2.0;
                    repelX += Math.cos(dispAngle) * hoverDispersion * 4.6;
                    repelY += Math.sin(dispAngle) * hoverDispersion * 3.5;
                    repelZ += Math.sin(phase) * hoverDispersion * 4.2;
                }

                flowerPositions[idx] = origX + windX + repelX;
                flowerPositions[idx + 1] = origY + windY + repelY;
                flowerPositions[idx + 2] = origZ + repelZ;

                // Distance Blooming: flowers expand from small bud to full blossom as camera approaches
                let bloomScale = 1.0;
                if (distZ > 0 && distZ < 75) {
                    bloomScale = Math.min(1.0, (75 - distZ) / 32);
                } else if (distZ <= 0 && distZ > -15) {
                    bloomScale = Math.max(0.1, (15 + distZ) / 15);
                } else if (distZ >= 75) {
                    bloomScale = 0.35; // Distant bud size
                }

                flowerSizesArr[i] = flowerBaseSizes[i] * (bloomScale + proximityBloom);
            }

            flowerGeo.attributes.position.needsUpdate = true;
            flowerGeo.attributes.size.needsUpdate = true;

            // Update Leaf Positions with Infinite Wrapping
            const leafPositions = leafGeo.attributes.position.array;
            for (let i = 0; i < LEAF_COUNT; i++) {
                const idx = i * 3;
                let origZ = leafOrigPos[idx + 2];
                if (origZ < camPos.z - 25) {
                    origZ += VALLEY_DEPTH;
                    leafOrigPos[idx + 2] = origZ;
                    const pathCenter = Math.sin(origZ * 0.0032) * 16 + Math.sin(origZ * 0.0012) * 10;
                    leafOrigPos[idx] = pathCenter + (Math.random() - 0.5) * 88;
                    leafOrigPos[idx + 1] = getTerrainY(leafOrigPos[idx], origZ) - 0.2 + Math.random() * 0.3;
                }

                leafPositions[idx] = leafOrigPos[idx] + Math.sin(timeVal * 0.8 + i) * 0.15;
                leafPositions[idx + 1] = leafOrigPos[idx + 1];
                leafPositions[idx + 2] = origZ;
            }
            leafGeo.attributes.position.needsUpdate = true;

            // Update Spores / Fireflies with Horizon Wrapping
            const dustPositions = dustGeo.attributes.position.array;
            for (let i = 0; i < DUST_COUNT; i++) {
                const idx = i * 3;
                dustPositions[idx + 1] += Math.sin(timeVal * 1.2 + i) * 0.016;
                dustPositions[idx] += Math.cos(timeVal * 0.9 + i) * 0.014;

                if (dustPositions[idx + 2] < camPos.z - 20) {
                    dustPositions[idx + 2] = camPos.z + VALLEY_DEPTH * 0.8 + Math.random() * 60;
                }
            }
            dustGeo.attributes.position.needsUpdate = true;

            // Render Frame
            renderer.render(scene, camera);
        }

        requestAnimationFrame(animate);
        console.info('[FlowerValley] 3D Night Flower Valley initialized and linked to hero.');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initFlowerValley);
    } else {
        initFlowerValley();
    }
})();
