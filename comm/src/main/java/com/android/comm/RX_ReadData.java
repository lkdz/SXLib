package com.android.comm;

import java.io.Serializable;

public class RX_ReadData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String meterNo; // 表号    *必填*
    public double surplusAmount; // 剩余量
    public double sumUseGas; // 累计用气量
    public int rfgCount; // 无线干扰次数
    public int magCount; // 磁干扰次数
    public int valveState; // 阀门状态 0;开阀;1;关阀;2;异常
    public int meterState; // 表状态
    public int powerState; // 电池电压 0-正常，1-低电
    public int signalStrength; // 信号强度
    public int dataTag; // 返回数据有效性 0- 无效, 1有效

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public double getSurplusAmount() {
        return surplusAmount;
    }

    public void setSurplusAmount(double surplusAmount) {
        this.surplusAmount = surplusAmount;
    }

    public double getSumUseGas() {
        return sumUseGas;
    }

    public void setSumUseGas(double sumUseGas) {
        this.sumUseGas = sumUseGas;
    }

    public int getRfgCount() {
        return rfgCount;
    }

    public void setRfgCount(int rfgCount) {
        this.rfgCount = rfgCount;
    }

    public int getMagCount() {
        return magCount;
    }

    public void setMagCount(int magCount) {
        this.magCount = magCount;
    }

    public int getValveState() {
        return valveState;
    }

    public void setValveState(int valveState) {
        this.valveState = valveState;
    }

    public int getMeterState() {
        return meterState;
    }

    public void setMeterState(int meterState) {
        this.meterState = meterState;
    }

    public int getPowerState() {
        return powerState;
    }

    public void setPowerState(int powerState) {
        this.powerState = powerState;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public int getDataTag() {
        return dataTag;
    }

    public void setDataTag(int dataTag) {
        this.dataTag = dataTag;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }
}
