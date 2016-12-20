package com.peyo.gvr.ex4;

import javax.microedition.khronos.egl.EGLConfig;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Surface;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {
    private static final String TAG = "Home3dActivity";
    
    private int mVideoTextureProgram;
    Screen mScreen;

    private int mIconTextureProgram;
    private Icon[] mIcons;
    private int mSelected = 0;

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
            setContentView(R.layout.common_ui);
            mGvrView = (GvrView) findViewById(R.id.cardboard_view);
            mGvrView.setRenderer(this);

        camera = new float[16];

        mSound = new SoundPool.Builder().setMaxStreams(2).build();
        focusSound = mSound.load(this, R.raw.focus, 1);
        clickSound = mSound.load(this, R.raw.click, 1);
    }

    private SoundPool mSound;
    private int focusSound;
    private int clickSound;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mIcons != null) mIcons[mSelected].bump(0);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mSelected < 4) mSelected++;
                mSound.play(focusSound, 1, 1, 1, 0, 1);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mSelected > 0) mSelected--;
                mSound.play(focusSound, 1, 1, 1, 0, 1);
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                mSound.play(clickSound, 1, 1, 1, 0, 1);
                launchApp(mSelected);
                break;
        }
        if (mIcons != null) mIcons[mSelected].bump(2);
        return super.onKeyDown(keyCode, event);
    }

    private void launchApp(int mSelected) {
        ComponentName name;
        switch(mSelected) {
            case 0:
                name = new ComponentName("com.peyo.gvr.ex3","com.peyo.gvr.ex3.MainActivity");
                break;
            case 1:
                name = new ComponentName("com.peyo.gvr.ex2","com.peyo.gvr.ex2.MainActivity");
                break;
            case 2:
                name = new ComponentName("com.peyo.gvr.ex5","com.peyo.gvr.ex5.MainActivity");
                break;
            default:
                return;
        }
        mVrAppLaunched = true;
        Intent intent = new Intent().setComponent(name);
        startActivity(intent);
    }

    private boolean mVrAppLaunched = false;

    @Override
	public void onSurfaceCreated(EGLConfig config) {
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        mIconTextureProgram = GLToolbox.createProgram(this, R.raw.texture_vertex_shader, R.raw.texture_fragment_shader);
        mIcons = new Icon[] { new Icon(6, 3.4f, -8, -5, mIconTextureProgram),
                new Icon(6, 3.4f, 0, -5, mIconTextureProgram),
                new Icon(6, 3.4f, 8, -5, mIconTextureProgram),
                new Icon(3.4f, 3.4f, 8, 0.5f, mIconTextureProgram),
                new Icon(3.4f, 3.4f, 8, 5.5f, mIconTextureProgram) } ;
        mIcons[0].setTexture(this, R.drawable.icon0);
        mIcons[1].setTexture(this, R.drawable.icon1);
        mIcons[2].setTexture(this, R.drawable.icon2);
        mIcons[3].setTexture(this, R.drawable.icon3);
        mIcons[4].setTexture(this, R.drawable.icon4);
        GLToolbox.checkGLError(TAG, "Icons");

        int uSamplerHandle = GLES20.glGetUniformLocation(mIconTextureProgram, "uSamplerTex");
        int aPositionHandle = GLES20.glGetAttribLocation(mIconTextureProgram, "aPosition");
        int aTexCoordHandle = GLES20.glGetAttribLocation(mIconTextureProgram, "aTexCoord");
        mSphere = new Sphere(aPositionHandle, aTexCoordHandle);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.home);
        mSphere.setTexture(uSamplerHandle, bmp);
        bmp.recycle();

        mVideoTextureProgram = GLToolbox.createProgram(this, R.raw.texture_vertex_shader, R.raw.video_fragment_shader);
        mScreen = new Screen(-3, 3, mVideoTextureProgram);
        GLToolbox.checkGLError(TAG, "Screen for Video");
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
            mPlayer = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().getPath()+ "/Movies/movie.mp4"));
            mPlayer.setLooping(true);
            mPlayer.setSurface(mSurface);
            mPlayer.setVolume(0.3f, 0.3f);
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

    private static final float CAMERA_Z = -0.01f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private float[] camera;

    @Override
	public void onNewFrame(HeadTransform headTransform) {
        mIcons[0].enableAttrib();
        mScreen.enableAttrib();
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

        mIcons[0].setMvp(mvpMatrix);
        mSphere.draw();
        mIcons[0].draw();
        mIcons[1].draw();
        mIcons[2].draw();
        mIcons[3].draw();
        mIcons[4].draw();

        mScreen.setMvp(mvpMatrix);
        mScreen.draw();
    }
    @Override
    protected void onDestroy() {
        releaseMediaPlayer();
        super.onDestroy();
        GLES20.glDeleteProgram(mIconTextureProgram);
        GLES20.glDeleteProgram(mVideoTextureProgram);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    boolean mFirstPause = true;
    @Override
    protected void onPause() {
        super.onPause();
        if (mVrAppLaunched) {
            releaseMediaPlayer();
            mVrAppLaunched = false;
        } else {
            if (!mFirstPause) {
                finish();
                System.exit(0);
            } else {
                setGvrView(mGvrView);
            }
            mFirstPause =false;
        }
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onRendererShutdown() {

    }

}
