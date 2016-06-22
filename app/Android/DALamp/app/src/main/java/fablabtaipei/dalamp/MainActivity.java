package fablabtaipei.dalamp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.CV_HOUGH_GRADIENT;

public class MainActivity extends Activity implements CvCameraViewListener{
    public final String ACTION_USB_PERMISSION = "fablabtaipei.dalamp.arduinousb.USB_PERMISSION";

    static {
        OpenCVLoader.initDebug();
    }

    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);



        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(Mat inputFrame) {
//        Mat grayMat = new Mat(inputFrame.rows(), inputFrame.cols(),
//                CvType.CV_8UC1);

        Mat hsvImage = new Mat(inputFrame.rows(), inputFrame.cols(),
                CvType.CV_8UC1);

        Imgproc.cvtColor(inputFrame, hsvImage, COLOR_BGR2HSV);

        Mat lowerBlueRange = new Mat(hsvImage.rows(), hsvImage.cols(),
                CvType.CV_8UC1);
        Mat upperBlueRange = new Mat(hsvImage.rows(), hsvImage.cols(),
                CvType.CV_8UC1);
        Core.inRange(hsvImage, new Scalar(0, 100, 100), new Scalar(10, 255, 255), lowerBlueRange);
        Core.inRange(hsvImage, new Scalar(160, 100, 100), new Scalar(179, 255, 255), upperBlueRange);

        Mat blueImage = new Mat(hsvImage.rows(), hsvImage.cols(),
                CvType.CV_8UC1);

        Core.addWeighted(lowerBlueRange, 1.0, upperBlueRange, 1.0, 0.0, blueImage);

//        /* convert to grayscale */
//        int colorChannels = (inputFrame.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
//                : ((inputFrame.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);
//
//        Imgproc.cvtColor(inputFrame, grayMat, colorChannels);

        /* reduce the noise so we avoid false circle detection */
        Imgproc.GaussianBlur(blueImage, blueImage, new Size(9, 9), 2, 2);

        // accumulator value
        double dp = 1.2d;
        // minimum distance between the center coordinates of detected circles in pixels
        double minDist = 100;

        // min and max radii (set these values as you desire)
        int minRadius = 15, maxRadius = 250;

        // param1 = gradient value used to handle edge detection
        // param2 = Accumulator threshold value for the
        // cv2.CV_HOUGH_GRADIENT method.
        // The smaller the threshold  is, the more circles will be
        // detected (including false circles).
        // The larger the threshold is, the more circles will
        // potentially be returned.
        double param1 = 90, param2 = 25;

        /* create a Mat object to store the circles detected */
        Mat circles = new Mat(inputFrame.rows(),
                inputFrame.cols(), CvType.CV_8UC1);

        /* find the circle in the image */
        Imgproc.HoughCircles(blueImage, circles, CV_HOUGH_GRADIENT, 1, blueImage.rows()/8, param1, param2, minRadius, maxRadius);

        /* get the number of circles detected */
        int numberOfCircles = (circles.rows() == 0) ? 0 : circles.cols();

        /* draw the circles found on the image */
        for (int i=0; i<numberOfCircles; i++) {

            /* get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
             * (x,y) are the coordinates of the circle's center
             */
            double[] circleCoordinates = circles.get(0, i);


            int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];

            Point center = new Point(x, y);

            int radius = (int) circleCoordinates[2];

            /* circle's outline */
            Imgproc.circle(inputFrame, center, radius, new Scalar(0,
                    255, 0), 4);

            /* circle's center outline */
            Imgproc.rectangle(inputFrame, new Point(x - 5, y - 5),
                    new Point(x + 5, y + 5),
                    new Scalar(0, 128, 255), -1);
        }

        return inputFrame;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        if (!OpenCVLoader.initDebug()) {
//            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
//        } else {
//            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
//        }
//
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
//        startButton = (Button) findViewById(R.id.buttonStart);
//        sendButton = (Button) findViewById(R.id.buttonSend);
//        clearButton = (Button) findViewById(R.id.buttonClear);
//        stopButton = (Button) findViewById(R.id.buttonStop);
//        editText = (EditText) findViewById(R.id.editText);
//        textView = (TextView) findViewById(R.id.textView);
//        setUiEnabled(false);
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_USB_PERMISSION);
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//        registerReceiver(broadcastReceiver, filter);
//    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }

    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");

    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

}
