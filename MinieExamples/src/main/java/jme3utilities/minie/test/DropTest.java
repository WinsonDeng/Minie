/*
 Copyright (c) 2018-2019, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.minie.test;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.collision.shapes.SimplexCollisionShape;
import com.jme3.bullet.collision.shapes.infos.DebugMeshNormals;
import com.jme3.bullet.debug.DebugInitListener;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.math.MyMath;
import jme3utilities.math.VectorSet;
import jme3utilities.math.VectorSetUsingBuffer;
import jme3utilities.math.noise.Generator;
import jme3utilities.minie.DumpFlags;
import jme3utilities.minie.FilterAll;
import jme3utilities.minie.PhysicsDumper;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Signals;

/**
 * Demo/testbed for convex collision shapes.
 * <p>
 * Seen in the November 2018 demo video:
 * https://www.youtube.com/watch?v=OS2zjB01c6E
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DropTest
        extends ActionApplication
        implements DebugInitListener {
    // *************************************************************************
    // constants and loggers

    /**
     * upper limit on the number of gems
     */
    final private static int maxNumGems = 80;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(DropTest.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = DropTest.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * status displayed in the upper-left corner of the GUI node
     */
    private BitmapText statusText;
    /**
     * AppState to manage the PhysicsSpace
     */
    final private BulletAppState bulletAppState = new BulletAppState();
    /**
     * shape for the new gem
     */
    private CollisionShape gemShape;
    /**
     * shape for a torus (generated using V-HACD, by the MakeTorus app)
     */
    private CompoundCollisionShape torusShape;
    /**
     * added gems, in order of creation
     */
    final private Deque<PhysicsRigidBody> gems = new ArrayDeque<>(maxNumGems);
    /**
     * filter to control visualization of axis-aligned bounding boxes
     */
    private FilterAll bbFilter;
    /**
     * filter to control visualization of swept spheres
     */
    private FilterAll ssFilter;
    /**
     * damping fraction for all gems (&ge;0, &le;1)
     */
    private float damping = 0.6f;
    /**
     * friction coefficient for all gems (&ge;0)
     */
    private float friction = 0.5f;
    /**
     * bounding-sphere radius for the new gem
     */
    private float gemRadius;
    /**
     * enhanced pseudo-random generator
     */
    final private Generator random = new Generator();
    /**
     * materials to visualize gems
     */
    final private Material gemMaterials[] = new Material[4];
    /**
     * single-sided green material to visualize the platform
     */
    private Material greenMaterial;
    /**
     * GUI node for displaying hotkey help/hints
     */
    private Node helpNode;
    /**
     * dump debugging information to System.out
     */
    final private PhysicsDumper dumper = new PhysicsDumper();
    /**
     * space for physics simulation
     */
    private PhysicsSpace physicsSpace;
    /**
     * name of shape for current platform
     */
    private String platformName = "box";
    /**
     * name of shape for new gems
     */
    private String shapeName = "multiSphere";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the DropTest application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Misc.setLoggingLevels(Level.WARNING);

        Application application = new DropTest();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);

        settings.setGammaCorrection(true);
        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        configureCamera();
        configureDumper();
        configureMaterials();
        configurePhysics();
        ColorRGBA sky = new ColorRGBA(0.1f, 0.2f, 0.4f, 1f);
        viewPort.setBackgroundColor(sky);

        addBox();
        String torusPath = "CollisionShapes/torus.j3o";
        torusShape = (CompoundCollisionShape) assetManager.loadAsset(torusPath);
        addAGem();
        /*
         * Add the status text to the GUI.
         */
        statusText = new BitmapText(guiFont, false);
        statusText.setLocalTranslation(0f, cam.getHeight(), 0f);
        guiNode.attachChild(statusText);
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("add", KeyInput.KEY_INSERT);

        dim.bind("delete", KeyInput.KEY_BACK);
        dim.bind("delete", KeyInput.KEY_DELETE);

        dim.bind("dump physicsSpace", KeyInput.KEY_O);
        dim.bind("dump scenes", KeyInput.KEY_P);

        dim.bind("less damping", KeyInput.KEY_B);
        dim.bind("less friction", KeyInput.KEY_V);

        dim.bind("platform box", KeyInput.KEY_1);
        dim.bind("platform heightfield", KeyInput.KEY_2);

        dim.bind("more damping", KeyInput.KEY_G);
        dim.bind("more friction", KeyInput.KEY_F);

        dim.bind("shape box", KeyInput.KEY_F3);
        dim.bind("shape cone", KeyInput.KEY_F4);
        dim.bind("shape cylinder", KeyInput.KEY_F6);
        dim.bind("shape hull", KeyInput.KEY_F2);
        dim.bind("shape multiSphere", KeyInput.KEY_F1);
        dim.bind("shape tetrahedron", KeyInput.KEY_F7);
        dim.bind("shape torus", KeyInput.KEY_F8);

        dim.bind("signal " + CameraInput.FLYCAM_LOWER, KeyInput.KEY_DOWN);
        dim.bind("signal " + CameraInput.FLYCAM_RISE, KeyInput.KEY_UP);
        dim.bind("signal orbitLeft", KeyInput.KEY_LEFT);
        dim.bind("signal orbitRight", KeyInput.KEY_RIGHT);
        dim.bind("signal shower", KeyInput.KEY_I);

        dim.bind("toggle axes", KeyInput.KEY_SEMICOLON);
        dim.bind("toggle boxes", KeyInput.KEY_APOSTROPHE);
        dim.bind("toggle help", KeyInput.KEY_H);
        dim.bind("toggle pause", KeyInput.KEY_PERIOD);
        dim.bind("toggle spheres", KeyInput.KEY_L);

        float x = 10f;
        float y = cam.getHeight() - 40f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle rectangle = new Rectangle(x, y, width, height);

        float space = 20f;
        helpNode = HelpUtils.buildNode(dim, rectangle, guiFont, space);
        guiNode.attachChild(helpNode);
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "add":
                    addAGem();
                    return;

                case "delete":
                    delete();
                    return;

                case "dump physicsSpace":
                    dumper.dump(physicsSpace);
                    return;

                case "dump scenes":
                    dumper.dump(renderManager);
                    return;

                case "less damping":
                    incrementDamping(-0.1f);
                    return;

                case "less friction":
                    multiplyFriction(0.5f);
                    return;

                case "more damping":
                    incrementDamping(0.1f);
                    return;

                case "more friction":
                    multiplyFriction(2f);
                    return;

                case "toggle axes":
                    toggleAxes();
                    return;

                case "toggle boxes":
                    toggleBoxes();
                    return;

                case "toggle help":
                    toggleHelp();
                    return;

                case "toggle pause":
                    togglePause();
                    return;

                case "toggle spheres":
                    toggleSpheres();
                    return;
            }

            String[] words = actionString.split(" ");
            if (words.length >= 2 && "platform".equals(words[0])) {
                platformName = words[1];
                restartTest();
                return;
            } else if (words.length >= 2 && "shape".equals(words[0])) {
                shapeName = words[1];
                return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        Signals signals = getSignals();
        if (signals.test("shower")) {
            addAGem();
        }

        updateStatusText();
    }
    // *************************************************************************
    // DebugInitListener methods

    /**
     * Callback from BulletDebugAppState, invoked just before the debug scene is
     * added to the debug viewports.
     *
     * @param physicsDebugRootNode the root node of the debug scene (not null)
     */
    @Override
    public void bulletDebugInit(Node physicsDebugRootNode) {
        addLighting(physicsDebugRootNode);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a gem (dynamic rigid body) to the scene.
     */
    private void addAGem() {
        if (gems.size() >= maxNumGems) {
            return; // too many gems
        }

        DebugMeshNormals debugMeshNormals;
        switch (shapeName) {
            case "box":
                randomBox();
                debugMeshNormals = DebugMeshNormals.Facet;
                break;

            case "cone":
                randomCone();
                debugMeshNormals = DebugMeshNormals.Smooth;
                break;

            case "cylinder":
                randomCylinder();
                debugMeshNormals = DebugMeshNormals.Smooth;
                break;

            case "hull":
                randomHull();
                debugMeshNormals = DebugMeshNormals.Facet;
                break;

            case "multiSphere":
                randomMultiSphere();
                debugMeshNormals = DebugMeshNormals.Smooth;
                break;

            case "tetrahedron":
                randomTetrahedron();
                debugMeshNormals = DebugMeshNormals.Facet;
                break;

            case "torus":
                gemRadius = 0.38f;
                gemShape = torusShape;
                debugMeshNormals = DebugMeshNormals.Smooth;
                break;

            default:
                String message = "shapeName = " + MyString.quote(shapeName);
                throw new RuntimeException(message);
        }

        Vector3f startLocation = random.nextVector3f();
        startLocation.multLocal(0.5f, 1f, 0.5f);
        startLocation.y += 4f;

        Quaternion startOrientation = random.nextQuaternion();

        Material debugMaterial = (Material) random.pick(gemMaterials);

        float mass = 1f;
        PhysicsRigidBody body = new PhysicsRigidBody(gemShape, mass);
        body.setCcdSweptSphereRadius(gemRadius);
        body.setCcdMotionThreshold(1f);
        body.setDamping(damping, damping);
        body.setDebugMaterial(debugMaterial);
        body.setDebugMeshNormals(debugMeshNormals);
        body.setDebugMeshResolution(DebugShapeFactory.highResolution);
        body.setFriction(friction);
        body.setPhysicsLocation(startLocation);
        body.setPhysicsRotation(startOrientation);

        physicsSpace.add(body);
        gems.addLast(body);
    }

    /**
     * Add a platform (static rigid body) to the scene.
     */
    private void addAPlatform() {
        switch (platformName) {
            case "box":
                addBox();
                break;

            case "heightfield":
                addHeightfield();
                break;

            default:
                String message
                        = "platformName = " + MyString.quote(platformName);
                throw new RuntimeException(message);
        }
    }

    /**
     * Add a large static box to the scene, to serve as a platform.
     */
    private void addBox() {
        float halfExtent = 4f;
        BoxCollisionShape shape = new BoxCollisionShape(halfExtent);
        float boxMass = PhysicsRigidBody.massForStatic;
        PhysicsRigidBody boxBody = new PhysicsRigidBody(shape, boxMass);

        boxBody.setDebugMaterial(greenMaterial);
        boxBody.setDebugMeshNormals(DebugMeshNormals.Facet);
        boxBody.setFriction(friction);
        boxBody.setPhysicsLocation(new Vector3f(0f, -halfExtent, 0f));
        physicsSpace.add(boxBody);
    }

    /**
     * Add a static heightfield to the scene, to serve as a platform.
     */
    private void addHeightfield() {
        int n = 64;
        float halfNm1 = (n - 1) / 2f;
        float[] heightmap = new float[n * n];
        for (int i = 0; i < n; ++i) {
            float x = -1f + i / halfNm1; // -1 .. +1
            for (int j = 0; j < n; ++j) {
                float y = -1f + j / halfNm1; // -1 .. +1
                float r = MyMath.hypotenuse(x, y);
                int floatIndex = n * i + j;
                heightmap[floatIndex] = -0.4f + (r - 0.8f) * (r - 0.8f);
            }
        }
        Vector3f scale = new Vector3f(4f / halfNm1, 2.5f, 4f / halfNm1);
        HeightfieldCollisionShape shape
                = new HeightfieldCollisionShape(heightmap, scale);
        float mass = PhysicsRigidBody.massForStatic;
        PhysicsRigidBody body = new PhysicsRigidBody(shape, mass);

        body.setDebugMaterial(greenMaterial);
        body.setDebugMeshNormals(DebugMeshNormals.Smooth);
        body.setFriction(friction);
        physicsSpace.add(body);
    }

    /**
     * Add lighting and shadows to the specified scene.
     */
    private void addLighting(Spatial rootSpatial) {
        ColorRGBA ambientColor = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootSpatial.addLight(ambient);

        ColorRGBA directColor = new ColorRGBA(0.7f, 0.7f, 0.7f, 1f);
        Vector3f direction = new Vector3f(1f, -2f, -1f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction, directColor);
        rootSpatial.addLight(sun);

        rootSpatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 2_048, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.5f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        float near = 0.02f;
        float far = 20f;
        MyCamera.setNearFar(cam, near, far);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(2f);

        cam.setLocation(new Vector3f(0f, 4f, 8f));
        cam.setRotation(new Quaternion(0f, 0.9649f, -0.263f, 0f));

        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);
    }

    /**
     * Configure the PhysicsDumper during startup.
     */
    private void configureDumper() {
        dumper.setEnabled(DumpFlags.MatParams, true);
        dumper.setEnabled(DumpFlags.ShadowModes, true);
        dumper.setEnabled(DumpFlags.Transforms, true);
    }

    /**
     * Configure materials during startup.
     */
    private void configureMaterials() {
        ColorRGBA green = new ColorRGBA(0f, 0.12f, 0f, 1f);
        greenMaterial = MyAsset.createShadedMaterial(assetManager, green);
        greenMaterial.setName("green");

        ColorRGBA gemColors[] = new ColorRGBA[gemMaterials.length];
        gemColors[0] = new ColorRGBA(0.2f, 0f, 0f, 1f); // ruby
        gemColors[1] = new ColorRGBA(0f, 0.07f, 0f, 1f); // emerald
        gemColors[2] = new ColorRGBA(0f, 0f, 0.3f, 1f); // sapphire
        gemColors[3] = new ColorRGBA(0.2f, 0.1f, 0f, 1f); // topaz

        for (int i = 0; i < gemMaterials.length; ++i) {
            ColorRGBA color = gemColors[i];
            gemMaterials[i]
                    = MyAsset.createShinyMaterial(assetManager, color);
            gemMaterials[i].setFloat("Shininess", 15f);
        }
    }

    /**
     * Configure physics during startup.
     */
    private void configurePhysics() {
        bulletAppState.setDebugEnabled(true);
        bulletAppState.setDebugInitListener(this);
        stateManager.attach(bulletAppState);

        physicsSpace = bulletAppState.getPhysicsSpace();
    }

    /**
     * Delete the most recently added gem.
     */
    private void delete() {
        PhysicsRigidBody latestGem = gems.peekLast();
        if (latestGem != null) {
            physicsSpace.remove(latestGem);
            gems.removeLast();
        }
    }

    /**
     * Alter the damping fractions for all gems.
     *
     * @param increment the amount to increase the fraction (may be negative)
     */
    private void incrementDamping(float increment) {
        float newDamping = FastMath.clamp(damping + increment, 0f, 1f);
        if (newDamping != damping) {
            damping = newDamping;
            for (PhysicsRigidBody gem : gems) {
                gem.setDamping(damping, damping);
            }
        }
    }

    /**
     * Alter the friction coefficients for all rigid bodies.
     *
     * @param factor the factor to increase the coefficient (&gt;0)
     */
    private void multiplyFriction(float factor) {
        assert factor > 0f : factor;

        friction *= factor;
        for (PhysicsRigidBody body : physicsSpace.getRigidBodyList()) {
            body.setFriction(friction);
        }
    }

    /**
     * Generate a box shape with random extents.
     */
    private void randomBox() {
        float rx = 0.1f + 0.3f * random.nextFloat();
        float ry = 0.1f + 0.3f * random.nextFloat();
        float rz = 0.1f + 0.3f * random.nextFloat();
        Vector3f halfExtents = new Vector3f(rx, ry, rz);
        gemRadius = halfExtents.length();

        gemShape = new BoxCollisionShape(halfExtents);
    }

    /**
     * Randomly generate a Y-axis cone shape.
     */
    private void randomCone() {
        float baseRadius = 0.1f + 0.2f * random.nextFloat();
        float height = 0.1f + 0.4f * random.nextFloat();

        gemRadius = MyMath.hypotenuse(baseRadius, height / 2f);
        gemRadius += CollisionShape.getDefaultMargin();

        gemShape = new ConeCollisionShape(baseRadius, height);
    }

    /**
     * Generate a Z-axis cylinder shape with random extents.
     */
    private void randomCylinder() {
        float baseRadius = 0.1f + 0.2f * random.nextFloat();
        float halfHeight = 0.1f + 0.3f * random.nextFloat();
        gemRadius = MyMath.hypotenuse(baseRadius, halfHeight);

        Vector3f halfExtents = new Vector3f(baseRadius, baseRadius, halfHeight);
        gemShape = new CylinderCollisionShape(halfExtents);
    }

    /**
     * Randomly generate a hull shape based on 5-20 vertices.
     */
    private void randomHull() {
        int numVertices = 5 + random.nextInt(16);
        VectorSet vertices = new VectorSetUsingBuffer(numVertices);

        gemRadius = 0f;
        vertices.add(new Vector3f(0f, 0f, 0f));
        for (int vertexIndex = 1; vertexIndex < numVertices; ++vertexIndex) {
            Vector3f location = random.nextUnitVector3f();
            location.multLocal(0.3f);
            vertices.add(location);
            float distance = location.length();
            gemRadius = Math.max(gemRadius, distance);
        }
        gemRadius += CollisionShape.getDefaultMargin();

        FloatBuffer buffer = vertices.toBuffer();
        gemShape = new HullCollisionShape(buffer);
    }

    /**
     * Randomly generate a MultiSphere shape with 1-4 spheres.
     */
    private void randomMultiSphere() {
        int numSpheres = 1 + random.nextInt(4);
        List<Vector3f> centers = new ArrayList<>(numSpheres);
        List<Float> radii = new ArrayList<>(numSpheres);

        centers.add(Vector3f.ZERO);
        float mainRadius = 0.1f + 0.2f * random.nextFloat();
        radii.add(mainRadius);
        gemRadius = mainRadius;

        for (int sphereIndex = 1; sphereIndex < numSpheres; ++sphereIndex) {
            Vector3f center = random.nextUnitVector3f();
            center.multLocal(mainRadius);
            centers.add(center);

            float radius = mainRadius * (0.2f + 0.8f * random.nextFloat());
            radii.add(radius);
            float extRadius = center.length() + radius;
            gemRadius = Math.max(gemRadius, extRadius);
        }

        gemShape = new MultiSphere(centers, radii);
    }

    /**
     * Randomly generate a simplex shape with 4 vertices.
     */
    private void randomTetrahedron() {
        float r1 = 0.03f + 0.3f * random.nextFloat();
        float r2 = 0.03f + 0.3f * random.nextFloat();
        float r3 = 0.03f + 0.3f * random.nextFloat();
        float r4 = 0.03f + 0.3f * random.nextFloat();
        gemRadius = FastMath.sqrt(3f) * MyMath.max(r1, r2, Math.max(r3, r4));

        Vector3f p1 = new Vector3f(r1, r1, r1);
        Vector3f p2 = new Vector3f(r2, -r2, -r2);
        Vector3f p3 = new Vector3f(-r3, -r3, r3);
        Vector3f p4 = new Vector3f(-r4, r4, -r4);
        gemShape = new SimplexCollisionShape(p1, p2, p3, p4);
    }

    /**
     * Start a new test using the named platform.
     */
    private void restartTest() {
        gems.clear();

        Collection<PhysicsRigidBody> bodies = physicsSpace.getRigidBodyList();
        for (PhysicsRigidBody body : bodies) {
            physicsSpace.remove(body);
        }

        addAPlatform();
    }

    /**
     * Toggle visualization of collision-object axes.
     */
    private void toggleAxes() {
        float length = bulletAppState.debugAxisLength();
        bulletAppState.setDebugAxisLength(0.5f - length);
    }

    /**
     * Toggle visualization of collision-object bounding boxes.
     */
    private void toggleBoxes() {
        if (bbFilter == null) {
            bbFilter = new FilterAll(true);
        } else {
            bbFilter = null;
        }

        bulletAppState.setDebugBoundingBoxFilter(bbFilter);
    }

    /**
     * Toggle visibility of the helpNode.
     */
    private void toggleHelp() {
        if (helpNode.getCullHint() == Spatial.CullHint.Always) {
            helpNode.setCullHint(Spatial.CullHint.Never);
        } else {
            helpNode.setCullHint(Spatial.CullHint.Always);
        }
    }

    /**
     * Toggle the physics simulation: paused/running.
     */
    private void togglePause() {
        float newSpeed = (speed > 1e-12f) ? 1e-12f : 1f;
        setSpeed(newSpeed);
    }

    /**
     * Toggle visualization of collision-object swept spheres.
     */
    private void toggleSpheres() {
        if (ssFilter == null) {
            ssFilter = new FilterAll(true);
        } else {
            ssFilter = null;
        }

        bulletAppState.setDebugSweptSphereFilter(ssFilter);
    }

    /**
     * Update the status text in the GUI.
     */
    private void updateStatusText() {
        int numGems = gems.size();
        String message = String.format(
                "platform=%s, shape=%s, count=%d, friction=%s, damping=%.1f",
                platformName, shapeName, numGems, friction, damping);
        statusText.setText(message);
    }
}