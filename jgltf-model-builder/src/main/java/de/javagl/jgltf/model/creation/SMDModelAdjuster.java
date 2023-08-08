package de.javagl.jgltf.model.creation;

import de.javagl.jgltf.model.impl.DefaultNodeModel;
import dev.thecodewarrior.binarysmd.studiomdl.NodesBlock;
import dev.thecodewarrior.binarysmd.studiomdl.SkeletonBlock;
import dev.thecodewarrior.binarysmd.studiomdl.TrianglesBlock;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.javagl.jgltf.model.creation.Blep.allZero;

public class SMDModelAdjuster {
    public static void removeBlenderImplictBone(NodesBlock nodesBlock, SkeletonBlock skeletonBlock, TrianglesBlock trianglesBlock) {
        System.out.println("Before: " + nodesBlock.bones.stream().map(a -> a.parent + " " + a.id).toList());

        // Create a map to store the new bone ids
        HashMap<Integer, Integer> idMap = new HashMap<>();
        // Find and remove the bone from NodesBlock
        int blender_implict_id = nodesBlock.bones.stream()
                .filter(b -> b.name.equals("blender_implicit"))
                .findFirst()
                .map(b -> b.id)
                .orElse(-1);

        nodesBlock.bones.removeIf(b -> b.name.equals("blender_implicit"));
        // Update the parent bones of other bones in NodesBlock
        for (NodesBlock.Bone b : nodesBlock.bones) {
            if (b.parent == blender_implict_id) {
                b.parent = -1; // or set to a different bone's id
            }
            // populate the idMap
            idMap.put(b.id, idMap.size());
            b.id = idMap.get(b.id);
        }

        // Remove the bone from SkeletonBlock
        for (SkeletonBlock.Keyframe kf : skeletonBlock.keyframes) {
            kf.states.removeIf(s -> s.bone == blender_implict_id);
        }
        // Remove the bone from TrianglesBlock
        if (trianglesBlock != null) {

            for (TrianglesBlock.Triangle t : trianglesBlock.triangles) {
                for (TrianglesBlock.Vertex v : t.vertices) {
                    v.links.removeIf(l -> l.bone == blender_implict_id);
                    if (v.parentBone == blender_implict_id) {
                        v.parentBone = -1; // or set to a different bone's id
                    }
                }
            }
        }
        // Update the ids of the remaining bones in NodesBlock
        for (NodesBlock.Bone b : nodesBlock.bones) {
            if (idMap.containsKey(b.id)) {
                b.id = idMap.get(b.id);
            }
            if (idMap.containsKey(b.parent)) {
                b.parent = idMap.get(b.parent);
            }
        }

        // Update the bone ids in SkeletonBlock
        for (SkeletonBlock.Keyframe kf : skeletonBlock.keyframes) {
            for (SkeletonBlock.BoneState bs : kf.states) {
                if (idMap.containsKey(bs.bone)) {
                    bs.bone = idMap.get(bs.bone);
                }
            }
        }

        // Update the bone ids in TrianglesBlock
        if (trianglesBlock != null) {
            for (TrianglesBlock.Triangle t : trianglesBlock.triangles) {
                for (TrianglesBlock.Vertex v : t.vertices) {
                    if (idMap.containsKey(v.parentBone)) {
                        v.parentBone = idMap.get(v.parentBone);
                    }
                    for (TrianglesBlock.Link l : v.links) {
                        if (idMap.containsKey(l.bone)) {
                            l.bone = idMap.get(l.bone);
                        }
                    }
                }
            }
        }
        System.out.println("After1: " + nodesBlock.bones.stream().map(a -> a.parent + " " + a.id).toList());
    }

    private static void printChildBones(DefaultNodeModel bone, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ".repeat(Math.max(0, depth)));
        sb.append(bone.getName());
        System.out.println(sb.toString());

        for (var child : bone.getChildren()) printChildBones((DefaultNodeModel) child, depth + 1);
    }

    public static Node readStructure(NodesBlock nodes, Map<Integer, Node> boneMap, SkeletonBlock skeleton) {
        var bones = nodes.bones.stream().map(Node::new).toList();

        var roots = new ArrayList<Node>();

        for (Node node : bones) {
            if(node.parentId == -1) {
                roots.add(node);
            } else {
                var parent = bones.get(node.parentId);

                parent.addChild(node);
                node.setParent(parent);
            }
        }

        roots.forEach(a -> printChildBones(a, 0));

        var rootNode = roots.size() == 1 ? roots.get(0) : roots.stream().filter(a -> !a.getChildren().isEmpty()).findFirst().get();

        SMDModelAdjuster.Node.populateBoneMap(rootNode, boneMap);

        skeleton.keyframes.get(0).states.stream().map(a -> new Pair<>(boneMap.get(a.bone), a)).forEach(entry -> {
            var node = entry.a();
            var pair = entry.b();
            if (!allZero(pair.posX, pair.posY, pair.posZ)) {
                node.setTranslation(new float[] { pair.posX, -pair.posY, -pair.posZ });
            }
            if (!allZero(pair.rotX, pair.rotY, pair.rotZ)) {
                Quaternionf quaternion = new Quaternionf().rotateXYZ(pair.rotX, -pair.rotY, -pair.rotZ).normalize();
                node.setRotation(new float[] { quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w() });
            }
        });

        return rootNode;
    }

    public static class Node extends DefaultNodeModel {
        public final int parentId;
        public final int id;

        public Node(NodesBlock.Bone bone) {
            this.parentId = bone.parent;
            this.setName(bone.name);
            this.id = bone.id;
        }

        @Override
        public String toString() {
            return getName() + " " + id;
        }

        public static void populateBoneMap(Node node, Map<Integer, Node> boneMap) {
            boneMap.put(node.id, node);

            for (var child : node.getChildren()) {
                populateBoneMap((Node) child, boneMap);
            }
        }
    }
}