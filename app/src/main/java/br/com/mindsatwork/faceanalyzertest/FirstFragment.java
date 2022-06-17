package br.com.mindsatwork.faceanalyzertest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import br.com.mindsatwork.faceanalyzer.FaceAnalyser;
import br.com.mindsatwork.faceanalyzer.constants.FrameAnalysisStatus;
import br.com.mindsatwork.faceanalyzertest.databinding.FragmentFirstBinding;
import br.com.mindsatwork.faceanalyzertest.services.CameraPreview;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    private CameraPreview cameraPreview;

    private CameraManager cameraManager;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);

        Context context = this.getContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this.getActivity(), new String[] {Manifest.permission.CAMERA}, 100);
        }

        boolean externalManage = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(! ( externalManage = Environment.isExternalStorageManager() ) ){

                try {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception ex){
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }

            }
        }

        if  (   (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            &&  (externalManage))
        {
            try
            {
                Camera.CameraInfo info = new Camera.CameraInfo();

                Camera.getCameraInfo( Camera.CameraInfo.CAMERA_FACING_FRONT, info );

                Camera currentCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

                FrameLayout frame = (FrameLayout) binding.getRoot().findViewById(R.id.camera_preview);

                TextView statusText = (TextView) binding.getRoot().findViewById(R.id.status);

                this.cameraPreview = new CameraPreview(
                                            context
                                        ,   currentCamera
                                        ,   (matched) -> {
                                            this.getActivity().runOnUiThread( () -> {
                                                statusText.setText( matched ? "USUÁRIO RECONHECIDO" : "USUÁRIO DIFERENTE" );
                                            });
                                        },
                                        (status)->{
                                            this.getActivity().runOnUiThread( () -> {
                                                statusText.setText( status == FrameAnalysisStatus.NO_FACE ? "PROCURANDO" : "ERRO");
                                            });
                                        }
                );

                frame.addView(this.cameraPreview);

            }catch (Throwable e)
            {
                Log.i("CAMERA VIEW", e.getMessage());
                e.printStackTrace();
            }
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}