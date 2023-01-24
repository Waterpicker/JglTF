package de.javagl.jgltf.model.creation;

import dev.thecodewarrior.binarysmd.formats.SMDBinaryReader;
import dev.thecodewarrior.binarysmd.formats.SMDTextReader;
import dev.thecodewarrior.binarysmd.studiomdl.NodesBlock;
import dev.thecodewarrior.binarysmd.studiomdl.SMDFile;
import dev.thecodewarrior.binarysmd.studiomdl.SkeletonBlock;
import dev.thecodewarrior.binarysmd.studiomdl.TrianglesBlock;
import org.msgpack.core.MessagePack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public record ModelResources(Model model, Map<String, Model> animations, double scale) {

    public static Optional<ModelResources> of(Path filePath) {
        try {
            System.out.println(filePath);
            var animations = new HashMap<String, Model>();
            Optional<Model> body = Optional.empty();
            double scale = 0;
            List<String> lines = Files.readAllLines(filePath);
            for (String line : lines) {

                String[] parts = line.replace("\t", "").split(" ");
                switch (parts[0]) {
                    case "$body" -> {
                        body = Model.of(filePath.getParent().resolve(parts[1]));
                    }
                    case "$anim" -> Model.of(filePath.getParent().resolve(parts[2])).ifPresent(model -> animations.put(parts[1], model));
                    case "$scale" -> scale = Double.parseDouble(parts[1]);
                }
            }

            if(body.isEmpty()) return Optional.empty();

            return Optional.of(new ModelResources(body.get(), animations, scale));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<SMDFile> extract(Path file) throws IOException {
        var extension = file.getFileName().toString();
        switch (extension.substring(extension.indexOf('.') + 1)) {
            case "smdx" -> {
                try (var is = Files.newInputStream(file); var unpacker = MessagePack.newDefaultUnpacker(is)) {
                    return Optional.of(new SMDBinaryReader().read(unpacker));
                } catch (Exception e) {
                    return Optional.empty();
                }
            }
            case "smd" -> {
                try {
                    return Optional.of(new SMDTextReader().read(Files.readString(file)));
                } catch (Exception e) {
                    return Optional.empty();
                }
            }
            case "bmd" -> {
                var bin = new BufferedInputStream(new FileInputStream(file.toFile()));
                var in = new DataInputStream(bin);
                in.readByte(); //This is the version. This is unused by SmdFile
                var smdFile = new SMDFile();
                smdFile.blocks.add(parseNodes(in));
                smdFile.blocks.add(parseSkeleton(in));
                smdFile.blocks.add(parseTriangles(in));

                in.close();
                bin.close();

                return Optional.of(smdFile);
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private static TrianglesBlock parseTriangles(DataInputStream is) throws IOException {
        var block = new TrianglesBlock();
        var triangles = block.triangles;

        List<String> material = new ArrayList<>();
        int numMaterial = is.readShort();
        for (int i = 0; i < numMaterial; i++) {
            material.add(readNullTerm(is));
        }

        int numTriangles = is.readShort();
        for (int i = 0; i < numTriangles; i++) {
            var triangle = parseTriangle(material.get(is.readByte()), is);

            triangles.add(triangle);
        }

        return block;
    }

    private static TrianglesBlock.Triangle parseTriangle(String material, DataInputStream in) throws IOException {
        return new TrianglesBlock.Triangle(material, parseVertex(in), parseVertex(in), parseVertex(in));

    }

    private static TrianglesBlock.Vertex parseVertex(DataInputStream in) throws IOException {
        var vertex =  new TrianglesBlock.Vertex(in.readShort(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
        List<TrianglesBlock.Link> links = vertex.links;

        int bound = in.readByte();
        for (int i = 0; i < bound; i++) {
            TrianglesBlock.Link link = new TrianglesBlock.Link(in.readShort(), in.readFloat());
            links.add(link);
        }
        return vertex;
    }

    private static SkeletonBlock parseSkeleton(DataInputStream is) throws IOException {
        var block = new SkeletonBlock();
        var skeleton = block.keyframes;

        int numSkeletons = is.readShort();
        for (short i = 0; i < numSkeletons; i++) {
            var keyframe = new SkeletonBlock.Keyframe(i);

            int numBones = is.readShort(); // Number of bone definitions in this frame
            for (int j = 0; j < numBones; j++) {
                var state = new SkeletonBlock.BoneState(is.readShort(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat(), is.readFloat());
                keyframe.states.add(state);
            }

            skeleton.add(keyframe);
        }

        return block;
    }

    private static NodesBlock parseNodes(DataInputStream in) throws IOException {
        var nodes = new ArrayList<NodesBlock.Bone>();

        var numNodes = in.readShort();
        for(int i = 0; i < numNodes; ++i) {
            var id = in.readShort();
            var parent = in.readShort();
            var name = readNullTerm(in);
            nodes.add(new NodesBlock.Bone(id, name, parent));
        }

        var block = new NodesBlock();
        block.bones = nodes;
        return block;
    }

    private static boolean allZero(float... numbers) {
        for (var number : numbers) {
            if(number != 0) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "ModelResources{" +
                "body=" + model +
                ", animations=" + animations +
                ", scale=" + scale +
                '}';
    }

    public record Model(NodesBlock nodes, SkeletonBlock skeleton, TrianglesBlock triangles) {
        public static Optional<Model> of(Path path) throws IOException {
            var smd = extract(path);

            if(smd.isEmpty()) return Optional.empty();

            NodesBlock nodes = null;
            SkeletonBlock skeleton = null;
            TrianglesBlock triangles = null;

            var blocks = smd.get().blocks;

            for (var block : blocks) {
                if(block instanceof NodesBlock n) nodes = n;
                if(block instanceof SkeletonBlock skel) skeleton = skel;
                if(block instanceof TrianglesBlock triangle) triangles = triangle;
            }

            return Optional.of(new Model(nodes, skeleton, triangles));
        }
    }

    private static String readNullTerm(DataInputStream in) throws IOException {
        StringBuilder str = new StringBuilder();
        char ch;
        while ((ch = in.readChar()) != 0) {
            str.append(ch);
        }
        return str.toString();
    }
}