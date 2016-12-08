package com.peyo.gvr.ex1;

import javax.microedition.khronos.egl.EGLConfig;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {
    private static final String TAG = "MainActivity";
    
    private int mProgram;
    private int uMVPMatrixHandle;
    private int aPositionHandle;
    private int aTexCoordHandle;
    Sphere mSphere;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);
        GvrView gvrView = (GvrView) findViewById(R.id.cardboard_view);
        gvrView.setRenderer(this);
        setGvrView(gvrView);

        camera = new float[16];
    }

	@Override
	public void onSurfaceCreated(EGLConfig config) {
        mProgram = GLToolbox.createProgram(this, R.raw.texture_vertex_shader,
        		R.raw.texture_fragment_shader);
        GLES20.glUseProgram(mProgram);
        GLES20.glFrontFace(GLES20.GL_CW);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        
        uMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        aPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        aTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        int uSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSamplerTex");

        mSphere = new Sphere(aPositionHandle, aTexCoordHandle);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.andes);
        mSphere.setTexture(uSamplerHandle, bmp);
        bmp.recycle();
        GLToolbox.checkGLError(TAG, "Program and Object for Sphere");
	}

    private static final float CAMERA_Z = 0.01f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private float[] camera;

    @Override
	public void onNewFrame(HeadTransform headTransform) {
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glEnableVertexAttribArray(aTexCoordHandle);
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
	}


    @Override
	public void onDrawEye(Eye eye) {
	    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] viewMatrix = new float[16];
        Matrix.multiplyMM(viewMatrix, 0, eye.getEyeView(), 0, camera, 0);
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, perspective, 0, viewMatrix, 0);

        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        mSphere.draw();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(mProgram);
    }


    @Override
    protected void onPause() {
        super.onPause();
        finish();
        System.exit(0);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onRendererShutdown() {

    }

}
