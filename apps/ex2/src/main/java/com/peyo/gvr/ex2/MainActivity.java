package com.peyo.gvr.ex2;

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

    private int mProgram0;
    private int uMVPMatrixHandle0;
    private int aPositionHandle0;
    private int aColorHandle0;
    Triangle mTriangle;
    Grid mGrid;

    private int mProgram1;
    private int uMVPMatrixHandle1;
    private int aPositionHandle1;
    private int aTexCoordHandle1;
    Square mSquare;
    Sphere mSphere;

    private int mProgram2;
    private int uMVPMatrixHandle2;
    private int uColorHandle2;
    private int aPositionHandle2;
    ObjLoader mObjLoader;
    Column mColumn;

    private float mAngle = 0;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);
        GvrView gvrView = (GvrView) findViewById(R.id.cardboard_view);
        gvrView.setRenderer(this);
        setGvrView(gvrView);

        camera = new float[16];

        mObjLoader = new ObjLoader(this);
        mObjLoader.load(R.raw.column);
    }

	@Override
	public void onSurfaceCreated(EGLConfig config) {
        mProgram0 = GLToolbox.createProgram(this, R.raw.color_vertex_shader,
                R.raw.color_fragment_shader);
        uMVPMatrixHandle0 = GLES20.glGetUniformLocation(mProgram0, "uMVPMatrix");
        aPositionHandle0 = GLES20.glGetAttribLocation(mProgram0, "aPosition");
        aColorHandle0 = GLES20.glGetAttribLocation(mProgram0, "aColor");
        mGrid = new Grid(aPositionHandle0, aColorHandle0, uMVPMatrixHandle0);
        mTriangle = new Triangle(aPositionHandle0, aColorHandle0);
        GLToolbox.checkGLError(TAG, "Program and Object for Grid/Triangle");

        mProgram1 = GLToolbox.createProgram(this, R.raw.texture_vertex_shader,
        		R.raw.texture_fragment_shader);
        uMVPMatrixHandle1 = GLES20.glGetUniformLocation(mProgram1, "uMVPMatrix");
        aPositionHandle1 = GLES20.glGetAttribLocation(mProgram1, "aPosition");
        aTexCoordHandle1 = GLES20.glGetAttribLocation(mProgram1, "aTexCoord");
        int uSamplerHandle = GLES20.glGetUniformLocation(mProgram1, "uSamplerTex");
        mSquare = new Square(aPositionHandle1, aTexCoordHandle1);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ground);
        mSquare.setTexture(uSamplerHandle, bmp);
        bmp.recycle();
        GLToolbox.checkGLError(TAG, "Program and Object for Square/Sphere");
        mSphere = new Sphere(aPositionHandle1, aTexCoordHandle1);
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.globe);
        mSphere.setTexture(uSamplerHandle, bmp);
        bmp.recycle();


        mProgram2 = GLToolbox.createProgram(this, R.raw.position_vertex_shader,
                R.raw.solid_fragment_shader);
        uMVPMatrixHandle2 = GLES20.glGetUniformLocation(mProgram2, "uMVPMatrix");
        aPositionHandle2 = GLES20.glGetAttribLocation(mProgram2, "aPosition");
        uColorHandle2 = GLES20.glGetUniformLocation(mProgram2, "uColor");
        GLES20.glEnableVertexAttribArray(aPositionHandle2);
        mColumn = new Column(mObjLoader.vertices, aPositionHandle2, uColorHandle2);
        GLToolbox.checkGLError(TAG, "Program and Object for Column");

        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
	}

    private static final float CAMERA_X = 2.0f;
    private static final float CAMERA_Y = 2.0f;
    private static final float CAMERA_Z = 2.0f;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private float[] camera;

    @Override
	public void onNewFrame(HeadTransform headTransform) {
        GLES20.glEnableVertexAttribArray(aPositionHandle0);
        GLES20.glEnableVertexAttribArray(aColorHandle0);
        GLES20.glEnableVertexAttribArray(aPositionHandle1);
        GLES20.glEnableVertexAttribArray(aTexCoordHandle1);
        GLES20.glEnableVertexAttribArray(aPositionHandle2);
        Matrix.setLookAtM(camera, 0, (float) Math.sin(mAngle / 1000.0f) * CAMERA_X,
                (float) Math.sin(mAngle / 1000.0f) * CAMERA_Y,
                (float) Math.cos(mAngle / 1000.0f) * CAMERA_Z,
                0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
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

        GLES20.glUseProgram(mProgram0);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle0, 1, false, mvpMatrix, 0);
        mGrid.draw(mvpMatrix);

        float[] mvpMatrix1 = new float[16];
        Matrix.translateM(mvpMatrix1, 0, mvpMatrix, 0, -4, 0, -4);
        GLES20.glUseProgram(mProgram1);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle1, 1, false, mvpMatrix1, 0);
        mSquare.draw();
        GLES20.glUseProgram(mProgram0);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle0, 1, false, mvpMatrix1, 0);
        mTriangle.draw();

        float[] mvpMatrix2 = new float[16];
        Matrix.rotateM(mvpMatrix2, 0, mvpMatrix, 0, ++mAngle/10.0f,  0, 1, 0);
        GLES20.glUseProgram(mProgram1);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle1, 1, false, mvpMatrix2, 0);
        mSphere.draw();
        GLES20.glUseProgram(mProgram2);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle2, 1, false, mvpMatrix2, 0);
        mColumn.draw();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(mProgram0);
        GLES20.glDeleteProgram(mProgram1);
        GLES20.glDeleteProgram(mProgram2);
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
