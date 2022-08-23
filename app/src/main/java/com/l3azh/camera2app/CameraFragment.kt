package com.l3azh.camera2app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.l3azh.camera2app.databinding.FragmentCameraBinding
import java.util.*


class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var cameraRequestPermission: ActivityResultLauncher<String>
    private lateinit var sizeForJPGE: Size


    companion object {
        const val TAG = "CAMERA_FRAGMENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(layoutInflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraRequestPermission = requireActivity().registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (!it) cameraRequestPermission.launch(Manifest.permission.CAMERA)
        }
        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated: ......")
                val result: List<Any> = setUpCamera()
                /*val surfaceViewLayoutParams = binding.cameraSurfaceView.layoutParams
                surfaceViewLayoutParams.width = 800
                surfaceViewLayoutParams.height = 1200
                binding.cameraSurfaceView.layoutParams = surfaceViewLayoutParams*/
                openCamera(
                    result[0] as CameraManager,
                    result[1] as String,
                    result[2] as ImageReader
                )

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "surfaceChanged: ......")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceDestroyed: ......")
            }
        })
    }


    fun setUpCamera(): List<Any> {
        val cameraManger: CameraManager =
            requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds: Array<String> = cameraManger.cameraIdList
        var chooseCameraId = ""
        var imageReader: ImageReader? = null
        for (id in cameraIds) {
            val cameraCharacteristics = cameraManger.getCameraCharacteristics(id)
            //If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                continue
            val listSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                .getOutputSizes(ImageFormat.JPEG)
            val previewSize =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.JPEG)[5]

            imageReader =
                ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 3)
            sizeForJPGE = previewSize
            chooseCameraId = id
        }
        return listOf(cameraManger, chooseCameraId, imageReader!!)
    }

    fun openCamera(cameraManager: CameraManager, cameraId: String, imageReader: ImageReader) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraRequestPermission.launch(Manifest.permission.CAMERA)
        } else {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onOpened(camera: CameraDevice) {
                    val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(binding.cameraSurfaceView.holder.surface)
                    camera.createCaptureSession(
                        listOf(
                            binding.cameraSurfaceView.holder.surface,
                            imageReader.surface
                        ), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                Log.d(TAG, "onConfigured: ")
                                val captureRequestBuilder =
                                    session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                captureRequestBuilder.set(
                                    CaptureRequest.FLASH_MODE,
                                    CaptureRequest.FLASH_MODE_TORCH
                                )
                                //captureRequestBuilder.addTarget(imageReader.surface)
                                captureRequestBuilder.addTarget(
                                    binding.cameraSurfaceView.holder.surface
                                )

                                session.setRepeatingRequest(
                                    captureRequestBuilder.build(),
                                    null,
                                    null
                                )
                                // A single request sometime later
                                val stillCaptureRequestBuilder =
                                    session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                                stillCaptureRequestBuilder.addTarget(binding.cameraSurfaceView.holder.surface)
                                session.capture(stillCaptureRequestBuilder.build(), null, null)

                                /*imageReader.setOnImageAvailableListener({ imgReader ->
                                    val image: Image = imgReader.acquireLatestImage()
                                }, null)*/
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.d(TAG, "onConfigureFailed: ")
                            }
                        }, null
                    )
                }

                override fun onClosed(camera: CameraDevice) {
                    super.onClosed(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d(TAG, "onDisconnected: ")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "onError: $error")
                }
            }, null)
        }
    }
}