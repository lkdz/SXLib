package com.android.comm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.android.bluetooth.BluetoothService;

public class RX_MT_Comm {
    private static final int RECEIVE_TIMEOUT = 8000;    // 无线模块接收超时时间
    private ArrayList<Integer> recvBytes;               // 缓存蓝牙接收的无线模块数据
    private String recvMeterNo;                         // 点抄的表号
    private List<RX_ReadData> readDatas;                // 批抄结果
    private BluetoothService bts;                       // 蓝牙服务

    /**
     * xx_MT_Comm 单例模式(xx为表厂名称缩写：荣鑫-RX,威星-WX,蓝宝石-LBS)
     */
    private static RX_MT_Comm rx = new RX_MT_Comm();

    private RX_MT_Comm() {
        recvBytes = new ArrayList<Integer>();
        readDatas = new ArrayList<RX_ReadData>();
        recvMeterNo = null;
        bts = null;
    }

    public static RX_MT_Comm getInstance() {
        return rx;
    }

    public void setBTService(BluetoothService mService) {
        bts = mService;
    }

    /**
     * 传递蓝牙数据
     *
     * @param bytes
     *            通过蓝牙读到的字节数据
     */
    public void setReadData(int bytes) {
//        String read = Integer.toHexString(bytes & 0xFF);
//        if (read.length() == 1) {
//            read = '0' + read;
//        }

        // 按字节缓存命令包
        recvBytes.add(bytes);

    }

    /**
     * 发送
     *
     * @param cmd
     * @return
     */
    private boolean send(byte[] cmd) {
        // 表厂代码（0-先锋,1-荣鑫,2-威星,3-蓝宝石,4-新天）
        boolean flag = bts.send(cmd, 1);
        return flag;
    }

    /**
     * 点抄
     *
     */
    public boolean readSingleMeter(String meterNo, List<String> param) {
        // 获取发送数据包
        byte[] arrayCmd = getArrayWithShell(getBytesOfReading(meterNo, param));

        // 获取命令失败返回false
        if (arrayCmd == null) return false;

        // 清空接收数据缓冲区
        recvBytes.clear();
        recvMeterNo = meterNo;

        // 发送数据包
        boolean flag = send(arrayCmd);

        // 发送失败则返回false
        if (!flag) return false;

        long sendTime = System.currentTimeMillis(); // 记录发送数据时间
        int recvCount = 0;
        // 开始等待接收数据
        try {
            while (true) {
                Thread.sleep(100);  // 100毫秒去查看一次接收缓冲区是否有数据
                if (recvBytes.size() > 0) { // 进入接收数据状态
                    while (recvBytes.size() > recvCount) {
                        recvCount = recvBytes.size();
                        // 蓝牙数据是通过平台调用setReadData逐字节发送，此时间必须大于平台调用的间隔
                        Thread.sleep(200);
                    }
                    // 判断接收完成（没有后续数据）
                    break;
                }
                // 看是否接收超时
                if ((System.currentTimeMillis() - sendTime) > RECEIVE_TIMEOUT) {
                    return false;
                }
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取点抄信息
     *
     * @return
     */
    public RX_ReadData recvSingleMeter() {
        // 解析接收的数据
        RX_ReadData readData = getReadData(getArrayExcludeShell(recvBytes));
        if (readData == null) {
            readData = new RX_ReadData();
            readData.setMeterNo(recvMeterNo);
            readData.setDataTag(0);
        }
        return readData;
    }

    /**
     * 批量抄表
     *
     * @return
     */
    public boolean readBatchMeter(String[] meterNo, String[] frequency) {
        if (meterNo == null || meterNo.length == 0) {
            return false;
        }

        this.readDatas.clear();
        for (int i = 0; i < meterNo.length; i++) {
            if (readSingleMeter(meterNo[i], null)) {
                this.readDatas.add(recvSingleMeter());
            }
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        return true;
    }

    /**
     * 获取批量抄表结果
     *
     * @return
     */
    public List<RX_ReadData> recvBatchMeter() {
        return this.readDatas;
    }

    /**
     * 开关阀
     *
     * @return
     */
    public boolean setValve(String meterNo, int type, List<String> param) {
        // 获取发送数据包
        byte[] arrayCmd;
        // 关阀
        if (type == 0) {
            arrayCmd = getArrayWithShell(getBytesOfClosingValve(meterNo, param));
        }
        else {
            arrayCmd = getArrayWithShell(getBytesOfOpeningValve(meterNo, param));
        }

        // 获取命令失败返回false
        if (arrayCmd == null) return false;

        // 清空接收数据缓冲区
        recvBytes.clear();

        // 发送数据包
        boolean b = send(arrayCmd);

        // 发送失败则返回false
        if (!b) return false;

        long sendTime = System.currentTimeMillis(); // 记录发送数据时间
        int recvCount = 0;
        // 开始等待接收数据
        try {
            while (true) {
                Thread.sleep(100);  // 100毫秒去查看一次接收缓冲区是否有数据
                if (recvBytes.size() > recvCount) {
                    while (recvBytes.size() > recvCount) {
                        recvCount = recvBytes.size();
                        // 蓝牙数据是通过平台调用setReadData逐字节发送，此时间必须大于平台调用的间隔
                        Thread.sleep(200);
                    }
                    break;
                }
                // 看是否接收超时
                if ((System.currentTimeMillis() - sendTime) > RECEIVE_TIMEOUT)
                    return false;
            }
        }
        catch (Exception e) {
            return false;
        }

        // 解析接收的数据
        if (getReadData(getArrayExcludeShell(recvBytes)) == null) {
            return false;
        }

        return true;
    }



    // 获取开阀命令
    protected byte[] getBytesOfOpeningValve(String meterId, List<String> params) {
        String p1, p2;
        p1 = p2 = null;
        if (params != null && params.size() >= 1) p1 = params.get(0);
        if (params != null && params.size() >= 2) p2 = params.get(1);

        if (getArrayModuleId(meterId) == null ||
                getArrayDateTime(p1) == null ||
                getArrayWakeUpSetting(p2) == null) {
            return null;
        }

        byte[] arrayCmd = new byte[20];

        arrayCmd[0] = (byte) 0x18;
        arrayCmd[1] = (byte) 0x12;
        arrayCmd[2] = (byte) 0x07;
        arrayCmd[3] = (byte) 0x00;
        arrayCmd[4] = (byte) 0x05;
        System.arraycopy(getArrayModuleId(meterId), 0, arrayCmd, 5, 5);
        System.arraycopy(getArrayDateTime(p1), 0, arrayCmd, 10, 5);
        System.arraycopy(getArrayWakeUpSetting(p2), 0, arrayCmd, 15, 4);
        arrayCmd[19] = getSum(arrayCmd, 1, 18);

        return arrayCmd;
    }

    // 获取关阀命令
    protected byte[] getBytesOfClosingValve(String meterId, List<String> params) {
        String p1, p2;
        p1 = p2 = null;
        if (params != null && params.size() >= 1) p1 = params.get(0);
        if (params != null && params.size() >= 2) p2 = params.get(1);

        if (getArrayModuleId(meterId) == null ||
                getArrayDateTime(p1) == null ||
                getArrayWakeUpSetting(p2) == null) {
            return null;
        }

        byte[] arrayCmd = new byte[20];

        arrayCmd[0] = (byte) 0x18;
        arrayCmd[1] = (byte) 0x12;
        arrayCmd[2] = (byte) 0x08;
        arrayCmd[3] = (byte) 0x00;
        arrayCmd[4] = (byte) 0x05;
        System.arraycopy(getArrayModuleId(meterId), 0, arrayCmd, 5, 5);
        System.arraycopy(getArrayDateTime(p1), 0, arrayCmd, 10, 5);
        System.arraycopy(getArrayWakeUpSetting(p2), 0, arrayCmd, 15, 4);
        arrayCmd[19] = getSum(arrayCmd, 1, 18);

        return arrayCmd;
    }

    // 获取燃气表累积流量的命令
    protected byte[] getBytesOfReading(String meterId, List<String> params) {
        // 表号长度必须是8、9、10位的数字
        // params（可以为Null，此时取系统时间和默认值）: p1-时间 p2-开关日开关时
        // 时间长度必须是10位的YYMMddHHmm格式（如果有）
        // 开关日长度必须是8位的开日关日开时关时的格式，比如01310023（如果有）
        String p1, p2;
        p1 = p2 = null;
        if (params != null && params.size() >= 1) p1 = params.get(0);
        if (params != null && params.size() >= 2) p2 = params.get(1);

        if (getArrayModuleId(meterId) == null ||
                getArrayDateTime(p1) == null ||
                getArrayWakeUpSetting(p2) == null) {
            return null;
        }

        byte[] arrayCmd = new byte[20];

        arrayCmd[0] = (byte) 0x18;
        arrayCmd[1] = (byte) 0x12;
        arrayCmd[2] = (byte) 0x02;
        arrayCmd[3] = (byte) 0x00;
        arrayCmd[4] = (byte) 0x05;
        System.arraycopy(getArrayModuleId(meterId), 0, arrayCmd, 5, 5);
        System.arraycopy(getArrayDateTime(p1), 0, arrayCmd, 10, 5);
        System.arraycopy(getArrayWakeUpSetting(p2), 0, arrayCmd, 15, 4);
        arrayCmd[19] = getSum(arrayCmd, 1, 18);

        return arrayCmd;
    }


    protected byte[] getArrayWithShell(byte[] array) {
        if (array == null) {
            return null;
        }
        int len = array.length;

        byte[] arrayCmd = new byte[len + 4];

        arrayCmd[0] = (byte) 0x68;          // 帧起始符
        arrayCmd[1] = (byte) 0x01;          // 厂商代号
        arrayCmd[2] = (byte) len;           // 数据长度
        System.arraycopy(array, 0, arrayCmd, 3, len);
        arrayCmd[len + 3] = (byte) 0x16;    // 结束符

        return arrayCmd;
    }

    protected byte[] getArrayModuleId(String moduleId) {
        // 燃气表号规则（上海大众：8位 金山、崇明：9位 绍兴：10位, 8位和9位取最后8位前面加24，10位转换成16进制后面加00）
        //检查moduleId
        if (moduleId == null || moduleId.trim().length() == 0) {
            return null;
        }

        String id = moduleId.trim();
        byte[] arrayModuleId = new byte[5];
        boolean isMatch;

        if (id.matches("^\\d{8}$")) {
            arrayModuleId[0] = (byte) 0x24;
            arrayModuleId[1] = (byte) (Integer.parseInt(id.substring(0, 2), 16) & 0xFF);
            arrayModuleId[2] = (byte) (Integer.parseInt(id.substring(2, 4), 16) & 0xFF);
            arrayModuleId[3] = (byte) (Integer.parseInt(id.substring(4, 6), 16) & 0xFF);
            arrayModuleId[4] = (byte) (Integer.parseInt(id.substring(6, 8), 16) & 0xFF);
        }
        else if (id.matches("^\\d{9}$")) {
            //第一个数字去掉，只保留后面8位
            arrayModuleId[0] = (byte) 0x24;
            arrayModuleId[1] = (byte) (Integer.parseInt(id.substring(1, 3), 16) & 0xFF);
            arrayModuleId[2] = (byte) (Integer.parseInt(id.substring(3, 5), 16) & 0xFF);
            arrayModuleId[3] = (byte) (Integer.parseInt(id.substring(5, 7), 16) & 0xFF);
            arrayModuleId[4] = (byte) (Integer.parseInt(id.substring(7, 9), 16) & 0xFF);
        }
        else if (id.matches("^\\d{10}$")) {
            if (Long.parseLong(id) <= 0xFFFFFFFFL) {
                Long idLong = Long.parseLong(id);
                arrayModuleId[0] = (byte) ((idLong >>> 24) & 0xFF);	// 最高位,无符号右移。
                arrayModuleId[1] = (byte) ((idLong >> 16) & 0xFF);	// 次高位
                arrayModuleId[2] = (byte) ((idLong >> 8) & 0xFF);	// 次低位
                arrayModuleId[3] = (byte) (idLong & 0xFF);			// 最低位
                arrayModuleId[4] = (byte) 0x00;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }

        return arrayModuleId;
    }

    protected byte[] getArrayDateTime(String datetime) {
        // 只能处理时间为Null（取系统时间）或者有时间且时间为10个长度的数字
        if (datetime != null && datetime.length() != 10) {
            return null;
        }

        byte[] arrayDateTime = new byte[5];

        if (datetime == null) {
            Calendar c = Calendar.getInstance();    // 可以对每个时间域单独修改

            arrayDateTime[0] = (byte) (c.get(Calendar.YEAR) - 2000);
            arrayDateTime[1] = (byte) (c.get(Calendar.MONTH) + 1);
            arrayDateTime[2] = (byte) c.get(Calendar.DATE);
            arrayDateTime[3] = (byte) c.get(Calendar.HOUR_OF_DAY);
            arrayDateTime[4] = (byte) c.get(Calendar.MINUTE);
        }
        else {
            arrayDateTime[0] = (byte) (Integer.parseInt(datetime.substring(0, 2), 10) & 0xFF);
            arrayDateTime[1] = (byte) (Integer.parseInt(datetime.substring(2, 4), 10) & 0xFF);
            arrayDateTime[2] = (byte) (Integer.parseInt(datetime.substring(4, 6), 10) & 0xFF);
            arrayDateTime[3] = (byte) (Integer.parseInt(datetime.substring(6, 8), 10) & 0xFF);
            arrayDateTime[4] = (byte) (Integer.parseInt(datetime.substring(8, 10), 10) & 0xFF);
        }

        return arrayDateTime;
    }

    protected byte[] getArrayWakeUpSetting(String setting) {
        // 只能处理开关日开关时为Null（取默认值1~31 06~19）或者为8位长度
        if (setting != null && setting.length() != 8) {
            return null;
        }

        byte[] arrayWakeUpSetting = new byte[4];

        if (setting == null) {
            arrayWakeUpSetting[0] = (byte) 1;
            arrayWakeUpSetting[1] = (byte) 31;
            arrayWakeUpSetting[2] = (byte) 6;
            arrayWakeUpSetting[3] = (byte) 19;
        }
        else {
            arrayWakeUpSetting[0] = (byte) (Integer.parseInt(setting.substring(0, 2), 10) & 0xFF);
            arrayWakeUpSetting[1] = (byte) (Integer.parseInt(setting.substring(2, 4), 10) & 0xFF);
            arrayWakeUpSetting[2] = (byte) (Integer.parseInt(setting.substring(4, 6), 10) & 0xFF);
            arrayWakeUpSetting[3] = (byte) (Integer.parseInt(setting.substring(6, 8), 10) & 0xFF);
        }

        return arrayWakeUpSetting;
    }

    protected byte getSum(byte[] array, int offset, int length) {
        int sum = 0x00;

        for	(int i = offset; i < offset + length; i++) {
            sum = (sum + (array[i] & 0xFF)) & 0xFF;
        }

        return (byte) sum;
    }


    // 处理接收的数据
    protected RX_ReadData getReadData(int[] receive) {
        if (receive == null || receive.length == 0) {
            return null;
        }

        int[] fullData = null;
        // 提取完整协议的数据包
        for (int i = 0; i < receive.length; i++) {
            if (receive.length - i < 5) {
                return null;
            }
            // 帧头
            if (receive[i] == 0x18) {
                int frameLen = receive[i + 1] + 2; // 帧长度
                int dataLen = receive.length - i;  // 包长度
                if (frameLen > dataLen) {
                    continue;
                }
                // 命令字
                if (receive[i + 2] != 0x80 && receive[i + 2] != 0x81 && receive[i + 2] != 0x01) {
                    continue;
                }
                // 校验码
                if (getSum(receive, i + 1, frameLen - 2) != receive[frameLen - 1 + i]) {
                    continue;
                }
                fullData = new int[frameLen];
                System.arraycopy(receive, i, fullData, 0, frameLen);
                break;
            }
        }

        if (fullData[2] == 0x01) {
            return null;
        }
        else if (fullData[2] == 0x81) {
            return null;
        }
        else if (fullData[2] == 0x80) {
            RX_ReadData readData = new RX_ReadData();
            readData.setMeterNo(recvMeterNo);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%02x", fullData[5]));
            sb.append(String.format("%02x", fullData[6]));
            sb.append(String.format("%02x", fullData[7]));
            String sumUseGas = sb.toString();
            if (sumUseGas.matches("^\\d{6}$")) {
                readData.setSumUseGas(Integer.parseInt(sumUseGas));
                readData.setDataTag(1);
            }
            else {
                readData.setSumUseGas(0);
                readData.setDataTag(0);
            }
            if (fullData[1] == 0x0C) {
                readData.setValveState(fullData[10] & 0x01);
                String voltage = String.format("%02x", fullData[11]);
                // 3.2V（包括3.2）以下为低电压
                if (voltage.matches("^\\d{2}$") && Integer.parseInt(voltage) > 32) {
                    readData.setPowerState(0);
                }
                else {
                    readData.setPowerState(1);
                }
            }
            return readData;
        }
        else {
            return null;
        }

    }

    protected int[] getArrayExcludeShell(ArrayList<Integer> receive) {
        if (receive == null || receive.size() == 0) {
            return null;
        }

        int[] arrayRecv = new int[receive.size()];
        for (int i = 0; i < receive.size(); i++) {
            arrayRecv[i] = receive.get(i);
        }

        if (arrayRecv.length < 4) {
            return null;
        }
        if (arrayRecv[0] != 0x68
                || arrayRecv[1] != 0x01
                || arrayRecv[arrayRecv.length - 1] != 0x16
                || arrayRecv[2] != arrayRecv.length - 4) {
            return null;
        }

        int[] ret = new int[arrayRecv[2]];
        System.arraycopy(arrayRecv, 3, ret, 0, arrayRecv[2]);

        return ret;
    }

    protected byte getSum(int[] array, int offset, int length) {
        int sum = 0x00;

        for	(int i = offset; i < offset + length; i++) {
            sum = ((sum + array[i])) & 0xFF;
        }

        return (byte) sum;
    }




}

