/*
 * www.javagl.de - JglTF
 *
 * Copyright 2015-2016 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package de.javagl.jgltf.viewer;

import de.javagl.jgltf.impl.Material;
import de.javagl.jgltf.impl.Program;
import de.javagl.jgltf.impl.Technique;
import de.javagl.jgltf.model.Materials;
import de.javagl.jgltf.model.Programs;
import de.javagl.jgltf.model.Shaders;
import de.javagl.jgltf.model.Techniques;
import de.javagl.jgltf.impl.Shader;

/**
 * A class containing the default {@link Shader}, {@link Program}, 
 * {@link Technique} and {@link Material} objects according to 
 * https://github.com/KhronosGroup/glTF/blob/master/specification/README.md#appendix-a
 *
 */
class GltfDefaults
{
    /**
     * An ID for the default vertex {@link Shader}
     */
    private static final String DEFAULT_VERTEX_SHADER_ID = 
        "de.javagl.jgltf.GltfDefaults.DEFAULT_VERTEX_SHADER_ID";

    /**
     * An ID for the default fragment {@link Shader}
     */
    private static final String DEFAULT_FRAGMENT_SHADER_ID = 
        "de.javagl.jgltf.GltfDefaults.DEFAULT_FRAGMENT_SHADER_ID";
    
    /**
     * An ID for the default {@link Program}
     */
    private static final String DEFAULT_PROGRAM_ID = 
        "de.javagl.jgltf.GltfDefaults.DEFAULT_PROGRAM_ID";
    
    /**
     * An ID for the default {@link Technique}
     */
    private static final String DEFAULT_TECHNIQUE_ID = 
        "de.javagl.jgltf.GltfDefaults.DEFAULT_TECHNIQUE_ID";
    
    /**
     * An ID for the default {@link Material}
     */
    private static final String DEFAULT_MATERIAL_ID = 
        "de.javagl.jgltf.GltfDefaults.DEFAULT_MATERIAL_ID";
    
    /**
     * The default vertex {@link Shader}
     */
    private static final Shader DEFAULT_VERTEX_SHADER = 
        Shaders.createDefaultVertexShader();
    
    /**
     * The default fragment {@link Shader}
     */
    private static final Shader DEFAULT_FRAGMENT_SHADER = 
        Shaders.createDefaultFragmentShader();
    
    /**
     * The default {@link Program}
     */
    private static final Program DEFAULT_PROGRAM = 
        Programs.createDefaultProgram(
            DEFAULT_VERTEX_SHADER_ID, DEFAULT_FRAGMENT_SHADER_ID);
    
    /**
     * The default {@link Technique}
     */
    private static final Technique DEFAULT_TECHNIQUE = 
        Techniques.createDefaultTechnique(DEFAULT_PROGRAM_ID);
    
    /**
     * The default {@link Material}
     */
    private static final Material DEFAULT_MATERIAL = 
        Materials.createDefaultMaterial(DEFAULT_TECHNIQUE_ID);
    
    /**
     * Returns whether the given ID is the default ID as defined in this class
     * 
     * @param id The ID
     * @return Whether the given ID is the default ID
     */
    static boolean isDefaultVertexShaderId(String id)
    {
        return DEFAULT_VERTEX_SHADER_ID.equals(id);
    }
    
    /**
     * Returns whether the given ID is the default ID as defined in this class
     * 
     * @param id The ID
     * @return Whether the given ID is the default ID
     */
    static boolean isDefaultFragmentShaderId(String id)
    {
        return DEFAULT_FRAGMENT_SHADER_ID.equals(id);
    }
    
    /**
     * Returns whether the given ID is the default ID as defined in this class
     * 
     * @param id The ID
     * @return Whether the given ID is the default ID
     */
    static boolean isDefaultProgramId(String id)
    {
        return DEFAULT_PROGRAM_ID.equals(id);
    }
    
    /**
     * Returns whether the given ID is the default ID as defined in this class
     * 
     * @param id The ID
     * @return Whether the given ID is the default ID
     */
    static boolean isDefaultTechniqueId(String id)
    {
        return DEFAULT_TECHNIQUE_ID.equals(id);
    }
    
    /**
     * Returns whether the given ID is the default ID as defined in this class
     * 
     * @param id The ID
     * @return Whether the given ID is the default ID
     */
    static boolean isDefaultMaterialId(String id)
    {
        return DEFAULT_MATERIAL_ID.equals(id);
    }
    
    /**
     * Returns the default vertex {@link Shader}
     * 
     * @return The {@link Shader}
     */
    static Shader getDefaultVertexShader()
    {
        return DEFAULT_VERTEX_SHADER;
    }
    
    /**
     * Returns the default fragment {@link Shader}
     * 
     * @return The {@link Shader}
     */
    static Shader getDefaultFragmentShader()
    {
        return DEFAULT_FRAGMENT_SHADER;
    }
    
    /**
     * Returns the default {@link Program}
     * 
     * @return The {@link Program}
     */
    static Program getDefaultProgram()
    {
        return DEFAULT_PROGRAM;
    }
    
    /**
     * Returns the default {@link Technique}
     * 
     * @return The {@link Technique}
     */
    static Technique getDefaultTechnique()
    {
        return DEFAULT_TECHNIQUE;
    }
    
    /**
     * Returns the default {@link Material}
     * 
     * @return The {@link Material}
     */
    static Material getDefaultMaterial()
    {
        return DEFAULT_MATERIAL;
    }
    
    
    
    
}


