package rocks.androidthings.arduwrap;

import android.media.UnsupportedSchemeException;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by alishaikh on 7/10/17.
 */

public class AdafruitMotorShieldV1Driver implements BaseSensor, AutoCloseable {
    private static final String TAG = "Dht22Driver";

    // Starting at 1 for parity between 'M' number on the board and command number
    public static final int DC_MOTOR_1 = 1; // M1
    public static final int DC_MOTOR_2 = 2; // M2
    public static final int DC_MOTOR_3 = 3; // M3
    public static final int DC_MOTOR_4 = 4; // M4
    @IntDef({DC_MOTOR_1, DC_MOTOR_2, DC_MOTOR_3, DC_MOTOR_4})
    public @interface DcMotorNumber {}

    public static final int STOP = 0;
    public static final int FORWARD = 1;
    public static final int BACKWARD = 2;
    @IntDef({STOP, FORWARD, BACKWARD})
    public @interface DcMotorDirection {}

    private static final int CHUNK_SIZE = 10;
    private ByteBuffer mMessageBuffer = ByteBuffer.allocate(CHUNK_SIZE);
    private final Arduino arduino;

    private UartDevice mDevice;
    private boolean receiving;

    public AdafruitMotorShieldV1Driver(Arduino arduino){
        this.arduino = arduino;
    }

    @Override
    public void startup() {
        PeripheralManagerService mPeripheralManagerService = new PeripheralManagerService();

        try {
            mDevice = mPeripheralManagerService.openUartDevice(arduino.getUartDeviceName());
            //TODO : Must fix data size
            mDevice.setDataSize(arduino.getDataBits());
            mDevice.setParity(UartDevice.PARITY_NONE);
            mDevice.setStopBits(arduino.getStopBits());
            mDevice.setBaudrate(arduino.getBaudRate());
        } catch (IOException e){
            try {
                close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw new IllegalStateException("Sensor can't start", e);
        }
    }

    public void setDcMotor(@DcMotorNumber int motorNum, @DcMotorDirection int direction, int speed) {
        String command = String.format("M%dD%dS%d", motorNum, direction, speed);
        try {sendCommand(command);
        } catch (IOException e) {
            try {close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    //TODO: Add Stepper comnand
    public void setStepperMotor() throws UnsupportedOperationException {}

    private void  sendCommand(String command) throws IOException {
        mDevice.write(command.getBytes(), command.length());
    }

    @Override
    public void close() throws Exception {
        if(mDevice != null){
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    @Override
    public void shutdown() {
        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }
    }
}

