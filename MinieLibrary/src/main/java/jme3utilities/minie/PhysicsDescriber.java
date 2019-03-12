/*
 Copyright (c) 2013-2019, Stephen Gold
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
package jme3utilities.minie;

import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.ConeCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.Describer;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;

/**
 * Generate compact textual descriptions of Minie data structures for debugging
 * purposes.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PhysicsDescriber extends Describer {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsDescriber.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description for a CollisionShape.
     *
     * @param shape (not null, unaffected)
     * @return description (not null)
     */
    public String describe(CollisionShape shape) {
        Validate.nonNull(shape, "shape");

        StringBuilder result = new StringBuilder(40);
        String name = shape.getClass().getSimpleName();
        if (name.endsWith("CollisionShape")) {
            name = MyString.removeSuffix(name, "CollisionShape");
        }
        result.append(name);

        if (shape instanceof BoxCollisionShape) {
            BoxCollisionShape box = (BoxCollisionShape) shape;
            Vector3f he = box.getHalfExtents(null);
            String desc = describeHalfExtents(he);
            result.append(desc);

        } else if (shape instanceof CapsuleCollisionShape) {
            CapsuleCollisionShape capsule = (CapsuleCollisionShape) shape;
            int axis = capsule.getAxis();
            String desc = describeAxis(axis);
            result.append(desc);

            float height = capsule.getHeight();
            float radius = capsule.getRadius();
            desc = describeHeightAndRadius(height, radius);
            result.append(desc);

        } else if (shape instanceof ConeCollisionShape) {
            ConeCollisionShape cone = (ConeCollisionShape) shape;
            int axis = cone.getAxis();
            String desc = describeAxis(axis);
            result.append(desc);

            float height = cone.getHeight();
            float radius = cone.getRadius();
            desc = describeHeightAndRadius(height, radius);
            result.append(desc);

        } else if (shape instanceof CompoundCollisionShape) {
            CompoundCollisionShape compound = (CompoundCollisionShape) shape;
            String desc = describeChildShapes(compound);
            String desc2 = String.format("[%s]", desc);
            result.append(desc2);

        } else if (shape instanceof CylinderCollisionShape) {
            CylinderCollisionShape cylinder = (CylinderCollisionShape) shape;
            int axis = cylinder.getAxis();
            String desc = describeAxis(axis);
            result.append(desc);

            Vector3f he = cylinder.getHalfExtents(null);
            desc = describeHalfExtents(he);
            result.append(desc);

        } else if (shape instanceof GImpactCollisionShape) {
            GImpactCollisionShape gimpact = (GImpactCollisionShape) shape;
            int numV = gimpact.countMeshVertices();
            String desc = String.format("[%d]", numV);
            result.append(desc);

        } else if (shape instanceof HullCollisionShape) {
            HullCollisionShape hull = (HullCollisionShape) shape;
            int numV = hull.countHullVertices();
            String desc = String.format("[%d]", numV);
            result.append(desc);

        } else if (shape instanceof MeshCollisionShape) {
            MeshCollisionShape mesh = (MeshCollisionShape) shape;
            int numV = mesh.countMeshVertices();
            String desc = String.format("[%d]", numV);
            result.append(desc);

        } else if (shape instanceof MultiSphere) {
            MultiSphere multiSphere = (MultiSphere) shape;
            int numSpheres = multiSphere.countSpheres();
            result.append("[r=");
            for (int sphereIndex = 0; sphereIndex < numSpheres; ++sphereIndex) {
                float radius = multiSphere.getRadius(sphereIndex);
                if (sphereIndex > 0) {
                    result.append(',');
                }
                String desc = MyString.describe(radius);
                result.append(desc);
            }
            result.append(']');

        } else if (shape instanceof SphereCollisionShape) {
            SphereCollisionShape sphere = (SphereCollisionShape) shape;
            float radius = sphere.getRadius();
            String rText = MyString.describe(radius);
            String desc = String.format("[r=%s]", rText);
            result.append(desc);
        }

        return result.toString();
    }

    /**
     * Generate a brief textual description for a PhysicsJoint.
     *
     * @param joint (not null, unaffected)
     * @return description (not null, not empty)
     */
    public String describe(PhysicsJoint joint) {
        StringBuilder result = new StringBuilder(40);

        String type = joint.getClass().getSimpleName();
        if (type.endsWith("Joint")) {
            type = MyString.removeSuffix(type, "Joint");
        }
        result.append(type);

        result.append(" #");
        long jointId = joint.getObjectId();
        result.append(Long.toHexString(jointId));

        if (!joint.isEnabled()) {
            result.append(" DISABLED");
        }

        return result.toString();
    }

    /**
     * Generate a textual description of a compound shape's children.
     *
     * @param compound shape being described (not null)
     * @return description (not null)
     */
    public String describeChildShapes(CompoundCollisionShape compound) {
        StringBuilder result = new StringBuilder(20);
        boolean addSeparators = false;
        ChildCollisionShape[] children = compound.listChildren();
        for (ChildCollisionShape child : children) {
            if (addSeparators) {
                result.append("  ");
            } else {
                addSeparators = true;
            }
            String desc = describe(child.getShape());
            result.append(desc);

            Vector3f location = child.getLocation(null);
            String locString = MyVector3f.describe(location);
            desc = String.format("@[%s]", locString);
            result.append(desc);

            Quaternion rotation = new Quaternion();
            rotation.fromRotationMatrix(child.getRotation(null));
            if (!MyQuaternion.isRotationIdentity(rotation)) {
                result.append("rot[");
                desc = MyQuaternion.describe(rotation);
                result.append(desc);
                result.append("]");
            }
        }

        return result.toString();
    }

    /**
     * Describe the specified PhysicsJoint in the context of the specified rigid
     * body.
     *
     * @param joint the joint to describe (not null, unaffected)
     * @param body one end of the joint
     * @return descriptive text (not null, not empty)
     */
    public String describeJointInBody(PhysicsJoint joint,
            PhysicsRigidBody body) {
        StringBuilder result = new StringBuilder(80);

        String desc = describe(joint);
        result.append(desc);

        long otherBodyId = 0L;
        Vector3f pivot;
        if (joint.getBodyA() == body) {
            PhysicsRigidBody bodyB = joint.getBodyB();
            if (bodyB != null) {
                otherBodyId = bodyB.getObjectId();
            }
            pivot = joint.getPivotA(null);
        } else {
            assert joint.getBodyB() == body;
            PhysicsRigidBody bodyA = joint.getBodyA();
            if (bodyA != null) {
                otherBodyId = joint.getBodyA().getObjectId();
            }
            pivot = joint.getPivotB(null);
        }

        if (otherBodyId == 0L) {
            result.append(" single-ended");
        } else {
            result.append(" to=#");
            result.append(Long.toHexString(otherBodyId));
        }
        result.append(" piv=[");
        result.append(MyVector3f.describe(pivot));
        result.append("]");

        return result.toString();
    }

    /**
     * Describe the specified PhysicsJoint in the context of a PhysicsSpace.
     *
     * @param joint the joint to describe (not null, unaffected)
     * @return descriptive text (not null, not empty)
     */
    public String describeJointInSpace(PhysicsJoint joint) {
        StringBuilder result = new StringBuilder(80);

        String desc = describe(joint);
        result.append(desc);

        float bit = joint.getBreakingImpulseThreshold();
        if (bit != Float.MAX_VALUE) {
            result.append(" bit=");
            desc = MyString.describe(bit);
            result.append(desc);
        }

        int numDyn = 0;
        PhysicsRigidBody bodyA = joint.getBodyA();
        if (bodyA != null) {
            result.append(" a=");
            long aId = joint.getBodyA().getObjectId();
            result.append(Long.toHexString(aId));
            if (!bodyA.isInWorld()) {
                result.append("_NOT_IN_WORLD");
            }
            if (bodyA.isDynamic()) {
                ++numDyn;
            }
        }

        PhysicsRigidBody bodyB = joint.getBodyB();
        if (bodyB != null) {
            result.append(" b=");
            long bId = bodyB.getObjectId();
            result.append(Long.toHexString(bId));
            if (!bodyB.isInWorld()) {
                result.append("_NOT_IN_WORLD");
            }
            if (bodyB.isDynamic()) {
                ++numDyn;
            }
        }

        if (numDyn == 0) {
            result.append("   NO_DYNAMIC_END");
        }

        return result.toString();
    }

    /**
     * Generate a textual description of ray-test flags.
     *
     * @param flags
     * @return description (not null)
     */
    String describeRayTestFlags(int flags) {
        List<String> flagList = new ArrayList<>(4);
        if ((flags & 0x1) != 0) {
            flagList.add("FilterBackfaces");
            /*
             * prevent returned face normal getting flipped when a
             * ray hits a back-facing triangle
             */
        }
        if ((flags & 0x2) != 0) {
            flagList.add("KeepUnflippedNormal");
        }
        if ((flags & 0x4) != 0) {
            flagList.add("SubSimplexConvexCast");
        }
        if ((flags & 0x8) != 0) {
            flagList.add("GjkConvexCast");
        }

        StringBuilder result = new StringBuilder(40);
        boolean addSeparators = false;
        for (String flagName : flagList) {
            if (addSeparators) {
                result.append(",");
            } else {
                addSeparators = true;
            }
            result.append(flagName);
        }

        return result.toString();
    }

    /**
     * Describe the user of a collision object.
     *
     * @param pco the collision object to describe (not null, unaffected)
     * @return a descriptive string (not null)
     */
    public String describeUser(PhysicsCollisionObject pco) {
        Validate.nonNull(pco, "collision object");

        StringBuilder builder = new StringBuilder(32);
        Object user = pco.getUserObject();
        if (user != null) {
            builder.append(" user=");
            builder.append(user.getClass().getSimpleName());
            if (user instanceof Spatial) {
                Spatial spatial = (Spatial) user;
                String name = spatial.getName();
                String text = MyString.quote(name);
                builder.append(text);
            } else if (user instanceof PhysicsLink) {
                PhysicsLink link = (PhysicsLink) user;
                String name = link.boneName();
                String text = MyString.quote(name);
                builder.append(text);
            }
        }

        return builder.toString();
    }
    // *************************************************************************
    // Describer methods

    /**
     * Create a copy of this PhysicsDescriber.
     *
     * @return a new instance, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public PhysicsDescriber clone() throws CloneNotSupportedException {
        PhysicsDescriber clone = (PhysicsDescriber) super.clone();
        return clone;
    }

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control (not null)
     * @return description (not null)
     */
    @Override
    protected String describe(Control control) {
        Validate.nonNull(control, "control");
        String result = MyControlP.describe(control);
        return result;
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    @Override
    protected boolean isControlEnabled(Control control) {
        Validate.nonNull(control, "control");

        boolean result = !MyControlP.canDisable(control)
                || MyControlP.isEnabled(control);

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Describe the specified half extents.
     *
     * @param he the half extent for each local axis (not null, unaffected)
     * @return a bracketed description (not null, not empty)
     */
    private String describeHalfExtents(Vector3f he) {
        String xText = MyString.describe(he.x);
        String yText = MyString.describe(he.y);
        String zText = MyString.describe(he.z);
        String result
                = String.format("[hx=%s,hy=%s,hz=%s]", xText, yText, zText);

        return result;
    }

    /**
     * Describe the specified height and radius.
     *
     * @param height the height of the shape
     * @param radius the radius of the shape
     * @return a bracketed description (not null, not empty)
     */
    private String describeHeightAndRadius(float height, float radius) {
        String hText = MyString.describe(height);
        String rText = MyString.describe(radius);
        String result = String.format("[h=%s,r=%s]", hText, rText);

        return result;
    }
}
