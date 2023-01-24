package de.javagl.jgltf.model.creation;

import de.javagl.jgltf.impl.v2.Node;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.impl.*;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.v2.GltfModelWriterV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import dev.thecodewarrior.binarysmd.studiomdl.SkeletonBlock;
import dev.thecodewarrior.binarysmd.studiomdl.TrianglesBlock;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class Blep {
    public static void main(String[] args) throws IOException {
        var gltfReference = new GltfAssetReader().readWithoutReferences(Files.newInputStream(Path.of("C:\\Users\\water\\Downloads\\berry.glb")));

        var m = GltfModels.create(gltfReference);

        var fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(new File("C:\\Users\\water\\Downloads\\"));
        fc.setMultiSelectionEnabled(true);
        fc.setFileSystemView(FileSystemView.getFileSystemView());
        fc.showOpenDialog(null);

        var path = fc.getSelectedFile().toPath();

        var list = Files.walk(path).toList();

        for (var x : list) {
            if(x.toString().contains(".pqc")) {
                var model = ModelResources.of(x);

                if(model.isEmpty()) continue;

                var body = model.get().model();

                var skeleton = body.skeleton();
                var nodes = body.nodes();
                var triangles = body.triangles();

                var boneMap = new HashMap<Integer, SMDModelAdjuster.Node>();
                var bondList = new ArrayList<>();
                var rootNode = SMDModelAdjuster.readStructure(nodes, boneMap, skeleton);

//                var rootTransform = new Pair<>(rootNode.getRotation(), rootNode.getTranslation());
//                rootNode.setRotation(null);
//                rootNode.setTranslation(null);

                var skin = createSkin(rootNode);

                var animations = model.get().animations();

                var animationList =
                animations.entrySet().stream().map(entry -> {
                    var key = entry.getKey();
                    var value = entry.getValue();
                    var keyframes = parseKeyFrames(boneMap, value.skeleton().keyframes);
                    var input = new ArrayList<Float>();
                    var outputPos = new HashMap<NodeModel, List<Vector3f>>();
                    var outputRot = new HashMap<NodeModel, List<Quaternionf>>();

                    for (int time = 0; time < keyframes.size(); time++) {
                        var states = keyframes.get(time);
                        input.add((float) time);

                        for (SmdAnimationData data : states) {
                            outputPos.computeIfAbsent(data.bone, d -> new ArrayList<>()).add(data.pos);

                            outputRot.computeIfAbsent(data.bone, d -> new ArrayList<>()).add(data.rot);
                        }
                    }

                    var animationModel = new DefaultAnimationModel();
                    animationModel.setName(key);

                    outputPos.forEach((k, v) -> {
                        var pair = processAnimationChannel(input, v);
                        var timestamps = AccessorModels.create(GltfConstants.GL_FLOAT, "SCALAR", false, Buffers.createByteBufferFrom(convert(pair.a())));
                        var rotations = AccessorModels.create(GltfConstants.GL_FLOAT, "VEC3", false, Buffers.createByteBufferFrom(convertVec(pair.b())));

                        var sampler = new DefaultAnimationModel.DefaultSampler(timestamps, AnimationModel.Interpolation.LINEAR, rotations);
                        animationModel.addChannel(new DefaultAnimationModel.DefaultChannel(sampler, k, "translation"));
                    });

                    System.out.println(entry.getKey());
                    outputRot.forEach((k, v) -> {
                        var pair = processAnimationChannel(input, v);
                        var timestamps = AccessorModels.create(GltfConstants.GL_FLOAT, "SCALAR", false, Buffers.createByteBufferFrom(convert(pair.a())));
                        var rotations = AccessorModels.create(GltfConstants.GL_FLOAT, "VEC4", false, Buffers.createByteBufferFrom(convertQuat(pair.b())));

                        var sampler = new DefaultAnimationModel.DefaultSampler(timestamps, AnimationModel.Interpolation.LINEAR, rotations);
                        animationModel.addChannel(new DefaultAnimationModel.DefaultChannel(sampler, k, "rotation"));
                    });

                    return animationModel;
                }).toList();


                var vertices = new ArrayList<TrianglesBlock.Vertex>();
                var indexBuffer = new ArrayList<Integer>();

                var posBuffer = new ArrayList<Float>();
                var texCoordBuffer = new ArrayList<Float>();
                var normalBuffer = new ArrayList<Float>();
                var jointBuffer = new ArrayList<Integer>();
                var weightBuffer = new ArrayList<Float>();

                Function<Integer, Integer> boneCorrection = index -> skin.getJoints().indexOf(boneMap.get(index));

                for (TrianglesBlock.Triangle triangle : triangles.triangles) {
                    for (TrianglesBlock.Vertex vertex : triangle.vertices) {
                        var index = vertices.indexOf(vertex);

                        if (index == -1) {
                            index = vertices.size();
                            vertices.add(vertex);
                            posBuffer.add(vertex.posX);
                            posBuffer.add(-vertex.posY);
                            posBuffer.add(-vertex.posZ);
                            texCoordBuffer.add(vertex.u);
                            texCoordBuffer.add(1 - vertex.v);
                            normalBuffer.add(vertex.normX);
                            normalBuffer.add(-vertex.normY);
                            normalBuffer.add(-vertex.normZ);

                            Pair<int[], float[]> pair = getTopLinks(boneCorrection, vertex.links);
                            int[] joint = pair.a();
                            jointBuffer.add(joint[0]);
                            jointBuffer.add(joint[1]);
                            jointBuffer.add(joint[2]);
                            jointBuffer.add(joint[3]);

                            float[] weight = pair.b();
                            weightBuffer.add(weight[0]);
                            weightBuffer.add(weight[1]);
                            weightBuffer.add(weight[2]);
                            weightBuffer.add(weight[3]);
                        }

                        indexBuffer.add(index);
                    }
                }

                var meshPrimitive = MeshPrimitiveBuilder.create()
                        .setIntIndices(convertInt(indexBuffer))
                        .addPositions3D(convert(posBuffer))
                        .addTexCoords02D(convert(texCoordBuffer))
                        .addNormals3D(convert(normalBuffer))
                        .addAttribute("JOINTS_0", AccessorModels.create(GltfConstants.GL_UNSIGNED_BYTE, "VEC4", false, Buffers.castToByteBuffer(IntBuffer.wrap(jointBuffer.stream().mapToInt(i -> i).toArray()))))
                        .addAttribute("WEIGHTS_0", AccessorModels.createFloat4D(convert(weightBuffer)))
                        .build();

                fc = new JFileChooser();
//                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setCurrentDirectory(new File(x.getParent().toString()));
                fc.setMultiSelectionEnabled(true);
                fc.setFileSystemView(FileSystemView.getFileSystemView());
                fc.showOpenDialog(null);

                var textureModel = new DefaultTextureModel();
                textureModel.setImageModel(ImageModels.create(fc.getSelectedFile().toURI().toString(), "derp"));

                MaterialBuilder materialBuilder =  MaterialBuilder.create();
                materialBuilder.setBaseColorTexture(textureModel, 0);
                MaterialModelV2 materialModel = materialBuilder.build();
                meshPrimitive.setMaterialModel(materialModel);

                // Create a mesh with the mesh primitive
                DefaultMeshModel meshModel = new DefaultMeshModel();
                meshModel.addMeshPrimitiveModel(meshPrimitive);

                // Create a node with the mesh
                rootNode.addMeshModel(meshModel);
                rootNode.setSkinModel(skin);

                // Create a scene with the node
                DefaultSceneModel sceneModel = new DefaultSceneModel();
                sceneModel.addNode(rootNode);

                // Pass the scene to the model builder. It will take care
                // of the other model elements that are contained in the scene.
                // (I.e. the mesh primitive and its accessors, and the material
                // and its textures)
                GltfModelBuilder gltfModelBuilder = GltfModelBuilder.create();
                gltfModelBuilder.addSkinModel(skin);
                gltfModelBuilder.addSceneModel(sceneModel);
//                gltfModelBuilder.addAnimationModels(animationList);
                DefaultGltfModel gltfModel = gltfModelBuilder.build();

                // Print the glTF to the console.
                var gltfWriter = new GltfModelWriterV2();
                gltfWriter.writeEmbedded(gltfModel, new FileOutputStream(Path.of(path.relativize(x).getFileName().toString().replace(".pqc", "") + ".gltf").toFile()));
            }
        }
    }

    private static DefaultSkinModel createSkin(SMDModelAdjuster.Node rootNode) {
        var skin = new DefaultSkinModel();
        var joints = new ArrayList<NodeModel>();
        var inverseBindPoses = computeInverseBindPose(rootNode, joints);
        joints.forEach(skin::addJoint);
        skin.setInverseBindMatrices(AccessorModels.create(GltfConstants.GL_FLOAT, "MAT4", false, Buffers.createByteBufferFrom(convertMatrix(inverseBindPoses))));
        skin.setSkeleton(rootNode);
        return skin;
    }

    private static FloatBuffer convertQuat(List<Quaternionf> v) {
        var length = v.size() * 4;
        var buffer = FloatBuffer.allocate(length);

        for (var quat : v) {
            buffer.put(quat.x()).put(quat.y()).put(quat.z()).put(quat.w());
        }

        return buffer;
    }

    private static FloatBuffer convertVec(List<Vector3f> v) {
        var length = v.size() * 3;
        var buffer = FloatBuffer.allocate(length);

        for (var quat : v) {
            buffer.put(quat.x()).put(quat.y()).put(quat.z());
        }

        return buffer;
    }

    private static Quaternionf eulertoQuatArray(float x, float y, float z) {
        return new Quaternionf().rotateXYZ(x, y, z);
    }

    private static List<List<SmdAnimationData>> parseKeyFrames(Map<Integer, SMDModelAdjuster.Node> boneMap, List<SkeletonBlock.Keyframe> keyframes) {
        keyframes.sort(Comparator.comparingInt(a -> a.time));


        List<List<SmdAnimationData>> list = new ArrayList<>();
        for (SkeletonBlock.Keyframe a : keyframes) {
            List<SmdAnimationData> pairs = a.states.stream().map(state -> new SmdAnimationData(boneMap.get(state.bone), new Vector3f(state.posX, state.posY, state.posZ), eulertoQuatArray(state.rotX, state.rotY, state.rotZ))).toList();
            list.add(pairs);
        }
        return list;
    }

    public record SmdAnimationData(SMDModelAdjuster.Node bone, Vector3f pos, Quaternionf rot) {}

    public static <T> Pair<List<Float>, List<T>> processAnimationChannel(List<Float> timestamps, List<T> channelData) {
        List<Float> newTimestamps = new ArrayList<>();
        Set<T> uniqueElements = new HashSet<>();

        for (int i = 0; i < channelData.size(); i++) {
            T currentElement = channelData.get(i);
            if (uniqueElements.add(currentElement)) {
                newTimestamps.add(timestamps.get(i));
            }
        }

        return new Pair(newTimestamps, new ArrayList<>(uniqueElements));
    }

    private static IntBuffer convertInt(List<Integer> values) {
        int length = values.size();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = values.get(i);
        }

        return IntBuffer.wrap(result);
    }

    private static FloatBuffer convert(List<Float> values) {
        int length = values.size();
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = values.get(i);
        }

        return FloatBuffer.wrap(result);
    }

    public static Pair<int[], float[]> getTopLinks(Function<Integer, Integer> boneMap, List<TrianglesBlock.Link> links) {

        int[] bones = new int[4];
        Arrays.fill(bones, 0);
        float[] weights = new float[4];
        Arrays.fill(weights, 0);
        for (int i = 0; i < 4 && i < links.size(); i++) {
            TrianglesBlock.Link link = links.get(i);

            bones[i] = boneMap.apply(link.bone);
            weights[i] = link.weight;
        }
        return new Pair<>(bones, weights);
    }

    public static boolean allZero(float... numbers) {
        for (var number : numbers) {
            if(number != 0) return false;
        }

        return true;
    }

    public static List<Matrix4f> computeInverseBindPose(NodeModel skeleton, List<NodeModel> joints) {
        List<Matrix4f> inverseBindPoseMatrices = new ArrayList<Matrix4f>();

        for (NodeModel child : skeleton.getChildren()) computeInverseBindPose(child, skeleton, inverseBindPoseMatrices, joints);
        return inverseBindPoseMatrices;
    }

    private static void computeInverseBindPose(NodeModel joint, NodeModel parent, List<Matrix4f> inverseBindPoseMatrices, List<NodeModel> joints) {
        Matrix4f localTransform = new Matrix4f();
        Supplier<float[]> localTransSupplier = joint.createLocalTransformSupplier();
        localTransform.set(localTransSupplier.get());
        localTransform.invert();
        if (parent != null) {
            int parentIndex = joints.indexOf(parent);
            if(parentIndex != -1){
                localTransform.mul(inverseBindPoseMatrices.get(parentIndex));
            }
            inverseBindPoseMatrices.add(localTransform);
            joints.add(joint);
        }else{
            inverseBindPoseMatrices.add(localTransform);
        }
        for (NodeModel child : joint.getChildren()) {
            computeInverseBindPose(child, joint, inverseBindPoseMatrices, joints);
        }
    }

    private static FloatBuffer convertMatrix(List<Matrix4f> inverseBindPoses) {
        int size = inverseBindPoses.size() * 16;
        float[] array = new float[size];
        int i = 0;
        for (Matrix4f matrix : inverseBindPoses) {
            array[i++] = matrix.m00();
            array[i++] = matrix.m01();
            array[i++] = matrix.m02();
            array[i++] = matrix.m03();
            array[i++] = matrix.m10();
            array[i++] = matrix.m11();
            array[i++] = matrix.m12();
            array[i++] = matrix.m13();
            array[i++] = matrix.m20();
            array[i++] = matrix.m21();
            array[i++] = matrix.m22();
            array[i++] = matrix.m23();
            array[i++] = matrix.m30();
            array[i++] = matrix.m31();
            array[i++] = matrix.m32();
            array[i++] = matrix.m33();
        }
        return FloatBuffer.wrap(array);
    }
}
