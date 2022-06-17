package br.com.mindsatwork.faceanalyzertest.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import br.com.mindsatwork.faceanalyzer.FaceAnalyser;
import br.com.mindsatwork.faceanalyzer.constants.FrameAnalysisStatus;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public interface OnFindFace
    {
        void callback(boolean isEqualReference);
    }

    public interface OnError
    {
        void callback( FrameAnalysisStatus status );
    }

    private SurfaceHolder holder;
    private Camera camera;

    private FaceAnalyser faceAnalyser;
    private boolean analyzing;

    private static byte[] referenceFir = null;

    private OnFindFace onFindFace;

    private OnError onError;

    public CameraPreview(Context context, Camera camera, OnFindFace onFindFace, OnError onError) throws IOException {
        super(context);

        this.camera = camera;

        camera.setPreviewCallback(this);

        this.holder = getHolder();
        this.holder.addCallback(this);
        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        this.faceAnalyser = FaceAnalyser.build(
                "/storage/emulated/0/FRSDK/activationkey-SDK-9.6.X.cfg"
            ,   0.85F
            ,   0.2F
            ,   0.8F
            ,   0.5F
            ,   "/storage/emulated/0/FRSDK"
        );

        this.faceAnalyser.Start();

        byte[] reference = Files.readAllBytes(Paths.get("/storage/emulated/0/FRSDK/referencia.jpg"));

        this.onFindFace = onFindFace;
        this.onError = onError;

        try {
            this.faceAnalyser.analyze( reference,
                                        (fir) -> {
                                            referenceFir = fir;
                                        },
                                        (error) -> {

                                        }
                                    );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if(referenceFir == null)
        {
            Log.i("ANALYZER", "NO REFERENCE");
            return;
        }

        camera.getParameters().getPreviewFormat();

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();

        YuvImage image = new YuvImage(
                                data
                            ,   ImageFormat.NV21
                            ,   size.width
                            ,   size.height
                            , null);

        Rect rectangle = new Rect();
        rectangle.bottom = size.height;
        rectangle.top = 0;
        rectangle.left = 0;
        rectangle.right = size.width;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compressToJpeg(rectangle, 100, out);

        byte[] frameJpeg =  out.toByteArray();


        Matrix matrix = new Matrix();
        matrix.postRotate(-90);

        ByteArrayOutputStream rotatedOut = new ByteArrayOutputStream();

        Bitmap bitmap = BitmapFactory.decodeByteArray(frameJpeg, 0, frameJpeg.length);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0 , 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        rotated.compress(Bitmap.CompressFormat.JPEG, 100, rotatedOut);

        byte[] rotatedFrameJpeg = rotatedOut.toByteArray();

        final Context currentContext = this.getContext();

        try {

            if(!this.analyzing)
            {
                this.analyzing = true;

                this.faceAnalyser.analyze(
                        rotatedFrameJpeg,
                        (fir) -> {

                            Log.i("ANALYZER", "Face Found");

                            boolean matched = faceAnalyser.compare(referenceFir, fir, "id");
                            onFindFace.callback(matched);

                            this.analyzing = false;
                        }
                    ,   (error) -> {
                            Log.i("ANALYZER", error.name());

                            this.analyzing = false;

                            this.onError.callback(error);
                        }
                );
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

        try {
            this.camera.setPreviewDisplay(this.holder);
            this.camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (holder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            camera.setPreviewDisplay(holder);
            //camera.setDisplayOrientation(getResources().getConfiguration().orientation);
            camera.setDisplayOrientation(90);
            camera.startPreview();

        } catch (Exception e){
            Log.d("CAMERA", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }
}
