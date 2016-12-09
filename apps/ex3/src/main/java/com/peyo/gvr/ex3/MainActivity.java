package com.peyo.gvr.ex3;

import javax.microedition.khronos.egl.EGLConfig;

import android.Manifest;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;

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
    
    private MediaPlayer mPlayer;
    private boolean mPlayerStarted = false;
    private int textureId;

    GvrView mGvrView;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

        setContentView(R.layout.common_ui);
        mGvrView = (GvrView) findViewById(R.id.cardboard_view);
        mGvrView.setRenderer(this);

        camera = new float[16];
    }

	@Override
	public void onSurfaceCreated(EGLConfig config) {

        GLES20.glFrontFace(GLES20.GL_CW);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        mProgram = GLToolbox.createProgram(this, R.raw.texture_vertex_shader,
        		R.raw.texture_fragment_shader);        
        uMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        aPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        aTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        mSphere = new Sphere(aPositionHandle, aTexCoordHandle);
        GLToolbox.checkGLError(TAG, "Program and Object for Sphere");

      makeVideoTexture();
    }

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private void makeVideoTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLToolbox.checkGLError(TAG, "Bind Texture");
        mSurfaceTexture = new SurfaceTexture(textureId);
        mSurface = new Surface(mSurfaceTexture);
        startMediaPlayer();
    }

    private void startMediaPlayer() {
        if (mSurface != null && !mPlayerStarted) {
        mPlayer = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().getPath()+ "/Movies/vr.mp4"));
            mPlayer.setLooping(true);
            mPlayer.setSurface(mSurface);
            mPlayer.start();
            mPlayerStarted = true;
        }
    }

    protected void releaseMediaPlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mPlayerStarted = false;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        startMediaPlayer();
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
        mSurfaceTexture.updateTexImage();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
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
        releaseMediaPlayer();
        super.onDestroy();
        GLES20.glDeleteProgram(mProgram);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    boolean mFirstPause = true;
    @Override
    protected void onPause() {
        super.onPause();
        if (!mFirstPause) {
            finish();
            System.exit(0);
        } else {
            setGvrView(mGvrView);
        }
        mFirstPause =false;
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onRendererShutdown() {

    }

}
