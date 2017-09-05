package com.novoda.tpbot.bot.device.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.novoda.notils.logger.simple.Log;
import com.novoda.support.Optional;
import com.novoda.tpbot.bot.device.DeviceConnection;

public class UsbDeviceConnection extends DeviceConnection implements UsbChangesListener {

    private final UsbChangesRegister usbChangesRegister;
    private final SupportedDeviceRetriever supportedDeviceRetriever;
    private final SerialPortMonitor serialPortMonitor;
    private final UsbPermissionRequester usbPermissionRequester;

    private Optional<UsbDevice> supportedUsbDevice = Optional.absent();

    public static UsbDeviceConnection newInstance(Context context, DeviceConnectionListener deviceConnectionListener) {
        UsbChangesRegister usbChangesRegister = new UsbChangesRegister(context);
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        SupportedDeviceRetriever supportedDeviceRetriever = new SupportedDeviceRetriever(usbManager);
        SerialPortCreator serialPortCreator = new SerialPortCreator();
        SerialPortMonitor serialPortMonitor = new SerialPortMonitor(usbManager, serialPortCreator);
        UsbPermissionRequester usbPermissionRequester = new UsbPermissionRequester(context, usbManager);

        return new UsbDeviceConnection(
                deviceConnectionListener,
                usbChangesRegister,
                supportedDeviceRetriever,
                serialPortMonitor,
                usbPermissionRequester
        );
    }

    private UsbDeviceConnection(DeviceConnectionListener deviceConnectionListener,
                                UsbChangesRegister usbChangesRegister,
                                SupportedDeviceRetriever supportedDeviceRetriever,
                                SerialPortMonitor serialPortMonitor,
                                UsbPermissionRequester usbPermissionRequester) {
        super(deviceConnectionListener);
        this.usbChangesRegister = usbChangesRegister;
        this.supportedDeviceRetriever = supportedDeviceRetriever;
        this.serialPortMonitor = serialPortMonitor;
        this.usbPermissionRequester = usbPermissionRequester;
    }

    @Override
    public void connect() {
        usbChangesRegister.register(this);
    }

    @Override
    public void onDeviceAttached() {
        openConnection();
    }

    private void openConnection() {
        if (supportedUsbDevice.isPresent()) {
            return;
        }

        supportedUsbDevice = supportedDeviceRetriever.retrieveFirstSupportedUsbDevice();

        if (supportedUsbDevice.isPresent()) {
            usbPermissionRequester.requestPermission(supportedUsbDevice.get());
        } else {
            Log.d(getClass().getSimpleName(), "startConnection: Could not find a supported USB device.");
        }
    }

    @Override
    public void onPermissionGranted() {
        boolean canMonitor = serialPortMonitor.tryToMonitorSerialPortFor(supportedUsbDevice.get(), dataReceiver);

        if (canMonitor) {
            deviceConnectionListener().onDeviceConnected();
        } else {
            closeConnection();
        }
    }

    private final SerialPortMonitor.DataReceiver dataReceiver = new SerialPortMonitor.DataReceiver() {
        @Override
        public void onReceive(String data) {
            deviceConnectionListener().onDataReceived(data);
        }
    };

    @Override
    public void send(String data) {
        // TODO: send data to serial port.
    }

    @Override
    public void disconnect() {
        usbChangesRegister.unregister();
        closeConnection();
    }

    private void closeConnection() {
        serialPortMonitor.stopMonitoring();
        supportedUsbDevice = Optional.absent();
        deviceConnectionListener().onDeviceDisconnected();
    }

    @Override
    public void onPermissionDenied() {
        closeConnection();
    }

    @Override
    public void onDeviceDetached() {
        closeConnection();
    }

}