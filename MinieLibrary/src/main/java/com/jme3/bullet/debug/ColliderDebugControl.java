/*
 * Copyright (c) 2020 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.debug;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.DebugMeshNormals;
import com.jme3.bullet.objects.MultiBodyCollider;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.logging.Logger;

/**
 * A physics-debug control used to visualize a MultiBodyCollider.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ColliderDebugControl extends AbstractPhysicsDebugControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(ColliderDebugControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * shape for which debugSpatial was generated (not null)
     */
    private CollisionShape myShape;
    /**
     * debug-mesh normals option for which debugSpatial was generated
     */
    private DebugMeshNormals oldNormals;
    /**
     * collision-shape margin for which debugSpatial was generated
     */
    private float oldMargin;
    /**
     * debug-mesh resolution for which debugSpatial was generated
     */
    private int oldResolution;
    /**
     * collider to visualize (not null)
     */
    final private MultiBodyCollider collider;
    /**
     * temporary storage for physics rotation
     */
    final private Quaternion rotation = new Quaternion();
    /**
     * Spatial to visualize myShape (not null)
     */
    private Spatial debugSpatial;
    /**
     * temporary storage for physics location
     */
    final private Vector3f location = new Vector3f();
    /**
     * physics scale for which debugSpatial was generated
     */
    final private Vector3f oldScale = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control to visualize the specified collider.
     *
     * @param debugAppState which app state (not null, alias created)
     * @param mbc the collider to visualize (not null, alias created)
     */
    public ColliderDebugControl(BulletDebugAppState debugAppState,
            MultiBodyCollider mbc) {
        super(debugAppState);
        collider = mbc;

        myShape = collider.getCollisionShape();
        oldMargin = myShape.getMargin();
        oldNormals = collider.debugMeshNormals();
        oldResolution = collider.debugMeshResolution();
        myShape.getScale(oldScale);

        debugSpatial = DebugShapeFactory.getDebugShape(collider);
        debugSpatial.setName(mbc.toString());
        updateMaterial();
    }
    // *************************************************************************
    // AbstractPhysicsDebugControl methods

    /**
     * Update this control. Invoked once per frame during the logical-state
     * update, provided the control is enabled and added to a scene. Should be
     * invoked only by a subclass or by AbstractControl.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float tpf) {
        CollisionShape newShape = collider.getCollisionShape();
        float newMargin = newShape.getMargin();
        DebugMeshNormals newNormals = collider.debugMeshNormals();
        int newResolution = collider.debugMeshResolution();
        Vector3f newScale = newShape.getScale(null);

        boolean rebuild;
        if (newShape instanceof CompoundCollisionShape) {
            rebuild = true;
        } else if (myShape != newShape) {
            rebuild = true;
        } else if (oldMargin != newMargin) {
            rebuild = true;
        } else if (oldNormals != newNormals) {
            rebuild = true;
        } else if (oldResolution != newResolution) {
            rebuild = true;
        } else if (!oldScale.equals(newScale)) {
            rebuild = true;
        } else {
            rebuild = false;
        }

        if (rebuild) {
            myShape = newShape;
            oldMargin = newMargin;
            oldNormals = newNormals;
            oldResolution = newResolution;
            oldScale.set(newScale);

            Node node = (Node) spatial;
            node.detachChild(debugSpatial);

            debugSpatial = DebugShapeFactory.getDebugShape(collider);
            debugSpatial.setName(collider.toString());

            node.attachChild(debugSpatial);
        }

        updateMaterial();
        collider.getPhysicsLocation(location);
        collider.getPhysicsRotation(rotation);
        applyPhysicsTransform(location, rotation);
    }

    /**
     * Alter which Spatial is controlled. Invoked when the Control is added to
     * or removed from a Spatial. Should be invoked only by a subclass or from
     * Spatial. Do not invoke directly from user code.
     *
     * @param spatial the Spatial to control (or null)
     */
    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            node.attachChild(debugSpatial);
        } else if (spatial == null && this.spatial != null) {
            Node node = (Node) this.spatial;
            node.detachChild(debugSpatial);
        }
        super.setSpatial(spatial);
    }
    // *************************************************************************
    // private methods

    /**
     * Update the Material applied to the debug geometries, based on properties
     * of the collider.
     */
    private void updateMaterial() {
        Material material = collider.getDebugMaterial();

        if (material == BulletDebugAppState.enableChildColoring) {
            if (debugSpatial instanceof Node) {
                /*
                 * Color each child of the CompoundCollisionShape.
                 */
                List<Spatial> children = ((Node) debugSpatial).getChildren();
                int numChildren = children.size();
                for (int childI = 0; childI < numChildren; ++childI) {
                    Spatial child = children.get(childI);
                    material = debugAppState.getChildMaterial(childI);
                    child.setMaterial(material);
                }
                return;
            }
            material = null;
        }

        if (material == null) { // apply one of the default materials
            int numSides = collider.debugNumSides();
            if (!collider.isContactResponse()) {
                material = debugAppState.getGhostMaterial(numSides);
            } else if (collider.isActive()) {
                material = debugAppState.getActiveMaterial(numSides);
            } else {
                material = debugAppState.getInactiveMaterial(numSides);
            }
        }
        debugSpatial.setMaterial(material);
    }
}