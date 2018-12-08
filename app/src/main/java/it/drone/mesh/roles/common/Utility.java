package it.drone.mesh.roles.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.drone.mesh.models.Device;
import it.drone.mesh.models.User;
import it.drone.mesh.tasks.ConnectBLETask;

/**
 * Tutte i metodi e le variabile condivise da server e client (quelle per la scansione per esempio) vengono messi qua.
 * Per il momento sia server che client devono avere accesso alla possiblità di eseguire scansioni, in futuro potrebbe cambiare
 */

public class Utility {
    // Stops scanning after 5 seconds.
    public static final long SCAN_PERIOD = 5000;

    public static String TAG = Utility.class.getSimpleName();
    public static int PACK_LEN = 18;
    public static int SINGLE_PACK_MESSAGE_LEN = 17;
    public static int DEST_PACK_MESSAGE_LEN = 16;

    public static int getBit(byte val, int offset) {
        return (val >> offset) & 1;
    }

    public static byte setBit(byte val, int offset) {
        val |= 1 << offset;
        return val;
    }

    public static byte clearBit(byte val, int offset) {
        val = (byte) (val & ~(1 << offset));
        return val;
    }

    public static void printByte(byte b) {
        String s = "";
        for (int i = 7; i > -1; i--) {
            s += getBit(b, i);
        }
        Log.d(TAG, "OUD: " + s);
    }

    public static byte byteMessageBuilder(int serverId, int clientId) {
        byte b = 0b00000000;

        Integer server = serverId;
        Integer client = clientId;

        byte serv = server.byteValue();
        byte clie = client.byteValue();

        b |= serv;
        b = (byte) (b << 4);
        clie = (byte) (clie << 1);
        b |= clie;
        b = setBit(b, 0);
        return b;
    }

    public static byte byteNearServerBuilder(int server1Id, int server2Id) {
        byte b = 0b00000000;

        Integer server = server1Id;
        Integer client = server2Id;

        byte server1 = server.byteValue();
        byte server2 = client.byteValue();

        b |= server1;
        b = (byte) (b << 4);

        b |= server2;

        return b;
    }

    public static byte[][] messageBuilder(byte firstByte, String message) {
        byte[] sInByte = message.getBytes();

        int numPacks = (int) Math.floor(sInByte.length / SINGLE_PACK_MESSAGE_LEN);
        byte[][] finalMessage = new byte[numPacks + 1][PACK_LEN];
        int lastLen = sInByte.length % SINGLE_PACK_MESSAGE_LEN;

        for (int i = 0; i < numPacks + 1; i++) {
            if (i == numPacks) {
                byte[] pack = new byte[lastLen + 1];
                firstByte = clearBit(firstByte, 0);
                pack[0] = firstByte;
                for (int j = 0; j < lastLen; j++) {
                    pack[j + 1] = sInByte[j + (i * SINGLE_PACK_MESSAGE_LEN)];
                }
                finalMessage[i] = pack;
                break;
            }
            byte[] pack = new byte[PACK_LEN];
            pack[0] = firstByte;
            for (int j = 0; j < SINGLE_PACK_MESSAGE_LEN; j++) {
                pack[j + 1] = sInByte[j + (i * SINGLE_PACK_MESSAGE_LEN)];
            }
            finalMessage[i] = pack;
        }
        return finalMessage;
    }

    public static byte[][] messageBuilder(byte firstByte, byte destByte, String message) {
        byte[] sInByte = message.getBytes();

        int numPacks = (int) Math.floor(sInByte.length / DEST_PACK_MESSAGE_LEN);
        byte[][] finalMessage = new byte[numPacks + 1][PACK_LEN];
        int lastLen = sInByte.length % DEST_PACK_MESSAGE_LEN;

        for (int i = 0; i < numPacks + 1; i++) {
            if (i == numPacks) {
                byte[] pack = new byte[lastLen + 1];
                firstByte = clearBit(firstByte, 0);
                pack[0] = firstByte;
                pack[1] = destByte;
                for (int j = 1; j < lastLen; j++) {
                    pack[j + 1] = sInByte[j + (i * DEST_PACK_MESSAGE_LEN)];
                }
                finalMessage[i] = pack;
                break;
            }
            byte[] pack = new byte[PACK_LEN];
            pack[0] = firstByte;
            pack[1] = destByte;
            for (int j = 0; j < DEST_PACK_MESSAGE_LEN; j++) {
                pack[j + 2] = sInByte[j + (i * DEST_PACK_MESSAGE_LEN)];
            }
            finalMessage[i] = pack;
        }
        return finalMessage;
    }

    public static int[] getByteInfo(byte firstByte) {
        int[] ret = new int[3];
        ret[2] = getBit(firstByte, 0);
        ret[1] = getBit(firstByte, 1) + getBit(firstByte, 2) * 2 + getBit(firstByte, 3) * 4;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret;
    }

    public static int[] getIdServerByteInfo(byte firstByte) {
        int[] ret = new int[2];
        ret[1] = getBit(firstByte, 0) + getBit(firstByte, 1) * 2 + getBit(firstByte, 2) * 4 + getBit(firstByte, 3) * 8;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret;
    }

    public static String getStringId(byte firstByte) {
        int[] ret = new int[2];
        ret[1] = getBit(firstByte, 1) + getBit(firstByte, 2) * 2 + getBit(firstByte, 3) * 4;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret[0] + "" + ret[1];
    }

    public static boolean sendMessage(String message, BluetoothGatt gatt, int[] info) {
        byte[][] finalMessage = messageBuilder(byteMessageBuilder(info[0], info[1]), message);
        boolean result = true;
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().equals(Constants.ServiceUUID)) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().equals(Constants.CharacteristicUUID)) {
                            for (int i = 0; i < finalMessage.length; i++) {
                                chars.setValue(finalMessage[i]);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                result = res && result;
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + new String(finalMessage[i]));
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                                if (i == finalMessage.length - 1) return result;
                            }
                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
        return false;
    }

    public static boolean sendMessage(String message, BluetoothGatt gatt, int[] infoSorg, int[] infoDest, OnMessageSentListener listener) {
        byte[][] finalMessage = messageBuilder(byteMessageBuilder(infoSorg[0], infoSorg[1]), byteMessageBuilder(infoDest[0], infoDest[1]), message);
        boolean result = true;
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().equals(Constants.ServiceUUID)) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().equals(Constants.CharacteristicUUID)) {
                            for (int i = 0; i < finalMessage.length; i++) {
                                chars.setValue(finalMessage[i]);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                result = res && result;
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + new String(finalMessage[i]));
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                                if (i == finalMessage.length - 1) {
                                    if (listener != null) {
                                        if (result)
                                            listener.OnMessageSent(Arrays.deepToString(finalMessage));
                                        else
                                            listener.OnCommunicationError("Error on communication");
                                    }
                                    return result;
                                }
                            }
                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
        return false;
    }

    public static boolean sendRoutingTable(String message, BluetoothGatt gatt, int[] infoSorg, int[] infoDest) {
        byte[][] finalMessage = messageBuilder(byteMessageBuilder(infoSorg[0], infoSorg[1]), byteMessageBuilder(infoDest[0], infoDest[1]), message);
        boolean result = true;
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().equals(Constants.RoutingTableServiceUUID)) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().equals(Constants.RoutingTableCharacteristicUUID)) {
                            for (int i = 0; i < finalMessage.length; i++) {
                                chars.setValue(finalMessage[i]);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                result = res && result;
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + new String(finalMessage[i]));
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                                if (i == finalMessage.length - 1) return result;
                            }
                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
        return false;
    }


    /**
     * Use this check to determine whether BLE is supported on the device. Then
     * you can selectively disable BLE-related features.
     *
     * @param context
     * @return
     */
    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    public static List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());
        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    public static ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        //builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        return builder.build();
    }

    public static ConnectBLETask createBroadcastRoutingTableClient(BluetoothDevice device, final String routingId, Context context, final byte[] value, final String id) {
        User u = new User(device, device.getName());
        return new ConnectBLETask(u, context, new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.RoutingTableServiceUUID);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                if (characteristic == null) return;
                BluetoothGattDescriptor desc = characteristic.getDescriptor(Constants.RoutingTableDescriptorUUID);
                boolean res = gatt.readDescriptor(desc);
                Log.d(TAG, "OUD: " + "Read desc routing: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "OUD: " + "i wrote a characteristic !");
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (Integer.parseInt(routingId) > Integer.parseInt(new String(descriptor.getValue()))) {
                        descriptor.setValue(routingId.getBytes());
                        gatt.writeDescriptor(descriptor);
                    } else return;
                }
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    String temp = new String(value);
                    BluetoothGattService service = gatt.getService(Constants.RoutingTableServiceUUID);
                    if (service == null) return;
                    BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                    if (characteristic1 == null) return;
                    int[] infoSorg = new int[2];
                    int[] infoDest = new int[2];
                    infoSorg[0] = Integer.parseInt(id);
                    infoSorg[1] = 0;
                    infoDest[0] = 0;
                    infoDest[1] = 0;
                    Utility.sendRoutingTable(temp, gatt, infoSorg, infoDest);
                }
                super.onDescriptorWrite(gatt, descriptor, status);
            }
        });
    }

    public static byte[][] buildMapFromString(String mapString) {
        byte[][] res = new byte[16][10];
        byte[] mapByte = mapString.getBytes();
        int j = 0;
        int counter = 0;
        for (int i = 0; i < mapByte.length; i++) {
            if (i % 10 == 0) {
                j++;
                counter = 0;
            }
            res[j][counter++] = mapByte[i];
        }
        return res;
    }

    public static ConnectBLETask createBroadCastNextServerIdClient(BluetoothDevice device, final String nextId, Context context, final byte[] value) {
        User u = new User(device, device.getName());
        return new ConnectBLETask(u, context, new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicNextServerIdUUID);
                if (characteristic == null) return;
                boolean res = gatt.readCharacteristic(characteristic);
                Log.d(TAG, "OUD: " + "Read Characteristic nextServerID: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    String temp = new String(characteristic.getValue());
                    if (Integer.parseInt(nextId) > Integer.parseInt(temp)) {
                        characteristic.setValue(nextId);
                        gatt.beginReliableWrite();
                        boolean res = gatt.writeCharacteristic(characteristic);
                        Log.d(TAG, "OUD: " + "Write Characteristic :--> " + res);
                        gatt.executeReliableWrite();
                    }

                }
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: " + "i wrote a characteristic !");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    String temp = new String(value);
                    BluetoothGattService service = gatt.getService(Constants.RoutingTableServiceUUID);
                    if (service == null) return;
                    BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                    if (characteristic1 == null) return;
                    characteristic1.setValue(temp);
                    gatt.beginReliableWrite();
                    boolean res = gatt.writeCharacteristic(characteristic1);
                    Log.d(TAG, "OUD: " + "write charac? " + res);
                    gatt.executeReliableWrite();
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
    }

    public interface OnRoutingTableUpdateListener {
        public void OnRoutingTableUpdate();
    }

    public interface OnMessageReceivedListener {
        public void OnMessageReceived(String message);
    }

    public interface OnMessageSentListener {
        public void OnMessageSent(String message);

        public void OnCommunicationError(String error);
    }

    public interface OnScanCompletedListener {
        public void OnScanCompleted(ArrayList<Device> devicesFound);
    }


}