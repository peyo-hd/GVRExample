package com.peyo.gvr.ex3;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GLToolbox extends GLES20 {

    public static void checkGLError(String tag, String label) {
        int error;
        while((error = glGetError()) != GL_NO_ERROR) {
            Log.e(tag, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }
    
    private static String readShaderString(Context context, int resId) {
        InputStream inputStream = context.getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int loadShader(int shaderType, String source) {
        int shader = glCreateShader(shaderType);
        if (shader != 0) {
            glShaderSource(shader, source);
            glCompileShader(shader);
            int[] compiled = new int[1];
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = glGetShaderInfoLog(shader);
                glDeleteShader(shader);
                throw new RuntimeException("Could not compile shader " + shaderType + ":" + info);
            }
        }
        return shader;
    }

    public static int createProgram(Context context, int vertexId, int fragId) {
    	return createProgram(readShaderString(context, vertexId),
    			readShaderString(context, fragId));
    }
    
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = glCreateProgram();
        if (program != 0) {
            glAttachShader(program, vertexShader);
            glAttachShader(program, pixelShader);
            glLinkProgram(program);
            int[] linkStatus = new int[1];
            glGetProgramiv(program, GL_LINK_STATUS, linkStatus,
                    0);
            if (linkStatus[0] != GL_TRUE) {
                String info = glGetProgramInfoLog(program);
                glDeleteProgram(program);
                throw new RuntimeException("Could not link program: " + info);
            }
        }
        return program;
    }

    static public FloatBuffer loadBuffer(float[] farray) {
        ByteBuffer bbuffer = ByteBuffer.allocateDirect(farray.length * 4);
        bbuffer.order(ByteOrder.nativeOrder());

        FloatBuffer fbuffer = bbuffer.asFloatBuffer();
        fbuffer.put(farray);
        fbuffer.position(0);
        return fbuffer;
    }

    static public ShortBuffer loadBuffer(short[] sarray) {
        ByteBuffer bbuffer = ByteBuffer.allocateDirect(sarray.length * 2);
        bbuffer.order(ByteOrder.nativeOrder());

        ShortBuffer sbuffer = bbuffer.asShortBuffer();
        sbuffer.put(sarray);
        sbuffer.position(0);
        return sbuffer;
    }

    static public void loadFloatVBO(int[] vbo, float[] farray) {
        FloatBuffer buffer = GLToolbox.loadBuffer(farray);
        glGenBuffers(1, vbo, 0);
        glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        glBufferData(GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    static public void loadElementVBO(int[] vbo, short[] sarray) {
        ShortBuffer buffer = GLToolbox.loadBuffer(sarray);
        glGenBuffers(1, vbo, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer.capacity() * 2, buffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

}
